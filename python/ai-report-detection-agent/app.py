import json
import math
import os
import re
import urllib.request
import zipfile
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timezone
from html import unescape
from html.parser import HTMLParser
from io import BytesIO
from typing import Any

from fastapi import FastAPI, HTTPException
from openai import OpenAI
from pydantic import BaseModel, Field


DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
DEFAULT_MODEL = "qwen3-max"
TEXT_SUFFIXES = {
    ".txt",
    ".md",
    ".java",
    ".py",
    ".js",
    ".ts",
    ".vue",
    ".html",
    ".css",
    ".json",
    ".xml",
    ".yml",
    ".yaml",
    ".sql",
    ".c",
    ".cpp",
    ".h",
    ".hpp",
    ".go",
    ".rs",
    ".kt",
    ".sh",
}
RISK_LEVELS = {"LOW", "MEDIUM", "HIGH"}

app = FastAPI(title="AI Report Detection Agent", version="1.0.0")


class AttachmentItem(BaseModel):
    fileId: int | None = None
    name: str = ""
    mimeType: str | None = None
    fileSize: int | None = None
    storagePath: str | None = None


class PreviousVersionItem(BaseModel):
    reportVersionId: int | None = None
    versionNo: int | None = None
    submittedAt: str | None = None
    contentText: str | None = None


class PeerReportItem(BaseModel):
    reportId: int | None = None
    reportVersionId: int | None = None
    studentName: str | None = None
    versionNo: int | None = None
    wordCount: int | None = None
    contentText: str | None = None


class ReportDetectionRequest(BaseModel):
    reportId: int
    reportVersionId: int
    experimentId: int | None = None
    experimentTitle: str | None = None
    experimentObjective: str | None = None
    experimentContentText: str | None = None
    studentId: int | None = None
    studentName: str | None = None
    versionNo: int | None = None
    wordCount: int | None = None
    draftSaveCount: int | None = None
    reportContentHtml: str | None = None
    reportContentText: str | None = None
    draftContentText: str | None = None
    previousVersions: list[PreviousVersionItem] = Field(default_factory=list)
    peerReports: list[PeerReportItem] = Field(default_factory=list)
    attachments: list[AttachmentItem] = Field(default_factory=list)


class EvidenceReference(BaseModel):
    sourceType: str
    title: str
    similarity: float = 0.0
    note: str = ""
    excerpt: str = ""


class SuspiciousChunkItem(BaseModel):
    chunkIndex: int
    chunkText: str
    aiScore: int
    riskLevel: str
    confidence: int
    reasons: list[str] = Field(default_factory=list)
    matchedRefs: list[EvidenceReference] = Field(default_factory=list)


class ReportDetectionResponse(BaseModel):
    success: bool
    model: str
    overallAiRatio: int
    riskLevel: str
    confidence: int
    needsHumanReview: bool
    summary: str
    explanations: list[str] = Field(default_factory=list)
    featureSummary: dict[str, Any] = Field(default_factory=dict)
    suspiciousChunks: list[SuspiciousChunkItem] = Field(default_factory=list)
    generatedAt: str = ""
    rawText: str | None = None


@dataclass
class ChunkEvidence:
    chunk_index: int
    chunk_text: str
    heuristic_score: int
    template_similarity: float
    peer_similarity: float
    history_similarity: float
    attachment_similarity: float
    specificity_score: int
    reasons: list[str]
    matched_refs: list[dict[str, Any]]


class BlockTextExtractor(HTMLParser):
    BLOCK_TAGS = {
        "p",
        "div",
        "section",
        "article",
        "header",
        "footer",
        "blockquote",
        "pre",
        "ul",
        "ol",
        "li",
        "table",
        "tr",
        "td",
        "th",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
    }

    def __init__(self) -> None:
        super().__init__()
        self.parts: list[str] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        if tag == "br":
            self.parts.append("\n")
        elif tag in self.BLOCK_TAGS:
            self.parts.append("\n\n")

    def handle_endtag(self, tag: str) -> None:
        if tag in self.BLOCK_TAGS:
            self.parts.append("\n\n")

    def handle_data(self, data: str) -> None:
        if data:
            self.parts.append(unescape(data))

    def get_text(self) -> str:
        text = "".join(self.parts)
        text = re.sub(r"\r\n?", "\n", text)
        text = re.sub(r"[ \t\f\v]+", " ", text)
        text = re.sub(r"\n{3,}", "\n\n", text)
        return text.strip()


@app.get("/health")
def health() -> dict[str, Any]:
    key = os.getenv("OPENAI_API_KEY", "").strip()
    return {
        "success": bool(key),
        "configured": bool(key),
        "model": os.getenv("REPORT_AI_MODEL", DEFAULT_MODEL),
    }


@app.post("/detect-report-ai", response_model=ReportDetectionResponse)
def detect_report_ai(request: ReportDetectionRequest) -> ReportDetectionResponse:
    report_text = build_report_text(request.reportContentHtml, request.reportContentText)
    if len(report_text) < 40:
        raise HTTPException(status_code=400, detail="report text is too short")

    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY is not configured")

    model = os.getenv("REPORT_AI_MODEL", DEFAULT_MODEL).strip() or DEFAULT_MODEL
    client = OpenAI(
        api_key=api_key,
        base_url=os.getenv("OPENAI_BASE_URL", DEFAULT_BASE_URL).strip() or DEFAULT_BASE_URL,
    )

    request = enrich_detection_request(request)
    attachment_summaries = extract_attachment_summaries(request.attachments)
    chunk_evidences, feature_summary = build_chunk_evidences(request, report_text, attachment_summaries)
    prompt = build_prompt(request, report_text, attachment_summaries, chunk_evidences, feature_summary)
    raw_text = chat_json(client, model, prompt)
    parsed = parse_model_json(raw_text)
    response = sanitize_response(parsed, model, raw_text, feature_summary, chunk_evidences)
    return response


def build_report_text(content_html: str | None, content_text: str | None) -> str:
    if content_html and content_html.strip():
        parser = BlockTextExtractor()
        parser.feed(content_html)
        text = parser.get_text()
    else:
        text = normalize_plain_text(content_text or "")
    if not text and content_text:
        text = normalize_plain_text(content_text)
    return text


def normalize_plain_text(text: str) -> str:
    normalized = re.sub(r"\r\n?", "\n", text or "")
    normalized = re.sub(r"[ \t\f\v]+", " ", normalized)
    normalized = re.sub(r"\n{3,}", "\n\n", normalized)
    return normalized.strip()


def enrich_detection_request(request: ReportDetectionRequest) -> ReportDetectionRequest:
    normalized_peers: list[PeerReportItem] = []
    seen_version_ids: set[int] = set()
    for item in request.peerReports:
        normalized_text = normalize_plain_text(item.contentText or "")
        if not normalized_text:
            continue
        clone = item.model_copy(deep=True)
        clone.contentText = normalized_text
        normalized_peers.append(clone)
        if clone.reportVersionId is not None:
            seen_version_ids.add(clone.reportVersionId)

    db_peers = load_peer_reports_from_db(request, seen_version_ids)
    request.peerReports = normalized_peers + db_peers
    return request


def split_report_chunks(report_text: str) -> list[str]:
    paragraphs = [item.strip() for item in re.split(r"\n{2,}", report_text) if item.strip()]
    if len(paragraphs) >= 2:
        return paragraphs[:18]

    dense = re.sub(r"\s+", " ", report_text).strip()
    if not dense:
        return []

    sentences = re.split(r"(?<=[。！？!?\.;；])", dense)
    chunks: list[str] = []
    current = ""
    for sentence in sentences:
        if not sentence.strip():
            continue
        if len(current) + len(sentence) <= 220:
            current += sentence
        else:
            if current.strip():
                chunks.append(current.strip())
            current = sentence
        if len(chunks) >= 18:
            break
    if current.strip() and len(chunks) < 18:
        chunks.append(current.strip())
    return chunks


def build_chunk_evidences(
    request: ReportDetectionRequest,
    report_text: str,
    attachment_summaries: list[dict[str, Any]],
) -> tuple[list[ChunkEvidence], dict[str, Any]]:
    chunks = split_report_chunks(report_text)
    if not chunks:
        chunks = [shorten(report_text, 320)]

    template_text = normalize_plain_text(
        "\n".join(
            [
                request.experimentTitle or "",
                request.experimentObjective or "",
                request.experimentContentText or "",
            ]
        )
    )
    previous_texts = [
        {
            "title": f"历史版本 V{item.versionNo or '-'}",
            "text": normalize_plain_text(item.contentText or ""),
        }
        for item in request.previousVersions
        if normalize_plain_text(item.contentText or "")
    ]
    if request.draftContentText and normalize_plain_text(request.draftContentText):
        previous_texts.append({"title": "当前草稿", "text": normalize_plain_text(request.draftContentText)})

    peer_texts = [
        {
            "title": f"同题同学 {index + 1}",
            "text": normalize_plain_text(item.contentText or ""),
        }
        for index, item in enumerate(request.peerReports[:10])
        if normalize_plain_text(item.contentText or "")
    ]
    attachment_texts = [
        {
            "title": item.get("title", ""),
            "text": normalize_plain_text(item.get("textPreview", "")),
        }
        for item in attachment_summaries
        if normalize_plain_text(item.get("textPreview", ""))
    ]

    process_risk = estimate_process_risk(request, len(report_text))
    experiment_keywords = extract_keywords(template_text)
    chunk_evidences: list[ChunkEvidence] = []

    template_scores: list[float] = []
    peer_scores: list[float] = []
    history_scores: list[float] = []
    attachment_scores: list[float] = []

    for index, chunk_text in enumerate(chunks):
        template_similarity = compare_texts(chunk_text, template_text) if template_text else 0.0
        peer_similarity, peer_ref = best_reference(chunk_text, peer_texts, "PEER")
        history_similarity, history_ref = best_reference(chunk_text, previous_texts, "SELF_HISTORY")
        attachment_similarity, attachment_ref = best_reference(chunk_text, attachment_texts, "ATTACHMENT")
        specificity_score = estimate_specificity(chunk_text, experiment_keywords)

        template_scores.append(template_similarity)
        peer_scores.append(peer_similarity)
        history_scores.append(history_similarity)
        attachment_scores.append(attachment_similarity)

        rewrite_gap = 1.0 - history_similarity if previous_texts else 0.35
        heuristic = (
            template_similarity * 0.30
            + peer_similarity * 0.32
            + rewrite_gap * 0.12
            + (1.0 - attachment_similarity) * 0.10
            + (1.0 - specificity_score / 100.0) * 0.06
            + process_risk * 0.10
        )
        heuristic_score = clamp_int(round(heuristic * 100), 0, 100)

        reasons: list[str] = []
        refs: list[dict[str, Any]] = []
        if template_similarity >= 0.55:
            reasons.append("与实验题面或模板文字较接近")
        if peer_similarity >= 0.58:
            reasons.append("与同题其他提交存在较高相似表达")
        if previous_texts and history_similarity <= 0.18:
            reasons.append("与本人历史草稿/版本差异较大")
        if attachment_texts and attachment_similarity <= 0.12:
            reasons.append("正文与附件可提取内容关联较弱")
        if specificity_score <= 35:
            reasons.append("表述较泛化，缺少实验细节")
        if process_risk >= 0.65:
            reasons.append("草稿过程信号偏弱，需要人工复核")

        if template_similarity > 0 and template_text:
            refs.append(
                {
                    "sourceType": "TEMPLATE",
                    "title": "实验题面/教师模板",
                    "similarity": round(template_similarity, 4),
                    "note": "模板重合度",
                    "excerpt": shorten(template_text, 120),
                }
            )
        if peer_ref:
            refs.append(peer_ref)
        if history_ref:
            refs.append(history_ref)
        if attachment_ref:
            refs.append(attachment_ref)

        chunk_evidences.append(
            ChunkEvidence(
                chunk_index=index,
                chunk_text=chunk_text,
                heuristic_score=heuristic_score,
                template_similarity=round(template_similarity, 4),
                peer_similarity=round(peer_similarity, 4),
                history_similarity=round(history_similarity, 4),
                attachment_similarity=round(attachment_similarity, 4),
                specificity_score=specificity_score,
                reasons=reasons[:4],
                matched_refs=refs[:4],
            )
        )

    chunk_evidences.sort(key=lambda item: item.heuristic_score, reverse=True)
    feature_summary = {
        "chunkCount": len(chunks),
        "maxHeuristicScore": max((item.heuristic_score for item in chunk_evidences), default=0),
        "maxTemplateSimilarity": round(max_or_zero(template_scores), 4),
        "maxPeerSimilarity": round(max_or_zero(peer_scores), 4),
        "maxHistorySimilarity": round(max_or_zero(history_scores), 4),
        "maxAttachmentSimilarity": round(max_or_zero(attachment_scores), 4),
        "processRisk": round(process_risk, 4),
        "draftSaveCount": request.draftSaveCount or 0,
        "peerSourceCount": len(peer_texts),
        "attachmentExtractedCount": len(attachment_texts),
    }
    return chunk_evidences[:12], feature_summary


def load_peer_reports_from_db(
    request: ReportDetectionRequest,
    existing_version_ids: set[int],
) -> list[PeerReportItem]:
    if request.experimentId is None or request.reportId is None:
        return []

    db_host = os.getenv("REPORT_DB_HOST", "localhost").strip()
    db_name = os.getenv("REPORT_DB_NAME", "course_assist").strip()
    db_user = os.getenv("REPORT_DB_USER", "root").strip()
    if not (db_host and db_name and db_user):
        return []

    try:
        import pymysql
        from pymysql.cursors import DictCursor
    except Exception:
        return []

    connection = None
    try:
        connection = pymysql.connect(
            host=db_host,
            port=int(os.getenv("REPORT_DB_PORT", "3306")),
            user=db_user,
            password=os.getenv("REPORT_DB_PASSWORD", "root"),
            database=db_name,
            charset="utf8mb4",
            cursorclass=DictCursor,
            connect_timeout=3,
            read_timeout=5,
            write_timeout=5,
        )
        with connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT r.id AS report_id,
                       v.id AS report_version_id,
                       COALESCE(su.real_name, su.username) AS student_name,
                       v.version_no,
                       v.word_count,
                       v.content_text,
                       v.content_html
                FROM t_report r
                INNER JOIN t_user su ON su.id = r.student_id
                INNER JOIN t_report_version v ON v.report_id = r.id
                WHERE r.experiment_id = %s
                  AND r.id <> %s
                  AND v.id = (
                      SELECT MAX(v2.id)
                      FROM t_report_version v2
                      WHERE v2.report_id = r.id
                  )
                ORDER BY v.submitted_at DESC, v.id DESC
                LIMIT 12
                """,
                (request.experimentId, request.reportId),
            )
            rows = cursor.fetchall()
    except Exception:
        return []
    finally:
        if connection is not None:
            try:
                connection.close()
            except Exception:
                pass

    result: list[PeerReportItem] = []
    for row in rows:
        version_id = int_or_none(row.get("report_version_id"))
        if version_id is not None and version_id in existing_version_ids:
            continue
        content_text = build_report_text(row.get("content_html"), row.get("content_text"))
        if not content_text:
            continue
        result.append(
            PeerReportItem(
                reportId=int_or_none(row.get("report_id")),
                reportVersionId=version_id,
                studentName=stringify(row.get("student_name")) or None,
                versionNo=int_or_none(row.get("version_no")),
                wordCount=int_or_none(row.get("word_count")),
                contentText=shorten(content_text, 2400),
            )
        )
    return result


def extract_attachment_summaries(attachments: list[AttachmentItem]) -> list[dict[str, Any]]:
    summaries: list[dict[str, Any]] = []
    for attachment in attachments[:6]:
        suffix = infer_suffix(attachment.name, attachment.storagePath)
        text_preview = ""
        note = ""
        if suffix in TEXT_SUFFIXES or suffix == ".docx":
            try:
                text_preview = extract_attachment_text(attachment, suffix)
            except Exception as exc:
                note = f"提取失败: {exc}"
        elif suffix in {".pdf", ".doc"}:
            note = "轻量版暂不解析该格式正文，只保留元数据"
        else:
            note = "未识别为可直接提取文本的附件"
        summaries.append(
            {
                "fileId": attachment.fileId,
                "title": attachment.name or attachment.storagePath or "附件",
                "mimeType": attachment.mimeType or "",
                "fileSize": attachment.fileSize or 0,
                "suffix": suffix,
                "textPreview": shorten(normalize_plain_text(text_preview), 1200),
                "note": note,
            }
        )
    return summaries


def infer_suffix(name: str, path: str | None) -> str:
    raw = (name or path or "").strip().lower()
    if "." not in raw:
        return ""
    return raw[raw.rfind(".") :]


def extract_attachment_text(attachment: AttachmentItem, suffix: str) -> str:
    if not attachment.storagePath:
        return ""
    raw_bytes = load_attachment_bytes(attachment.storagePath)
    if not raw_bytes:
        return ""
    if suffix == ".docx":
        return extract_docx_text(raw_bytes)
    return decode_text_bytes(raw_bytes)


def load_attachment_bytes(storage_path: str) -> bytes:
    if storage_path.startswith("http://") or storage_path.startswith("https://"):
        request = urllib.request.Request(storage_path, headers={"User-Agent": "CodexReportDetector/1.0"})
        with urllib.request.urlopen(request, timeout=8) as response:
            return response.read(180_000)

    local_path = storage_path
    if os.path.exists(local_path):
        with open(local_path, "rb") as file:
            return file.read(180_000)
    return b""


def extract_docx_text(raw_bytes: bytes) -> str:
    with zipfile.ZipFile(BytesIO(raw_bytes)) as archive:
        xml_text = archive.read("word/document.xml").decode("utf-8", errors="ignore")
    xml_text = re.sub(r"</w:p>", "\n\n", xml_text)
    xml_text = re.sub(r"<[^>]+>", "", xml_text)
    return unescape(xml_text)


def decode_text_bytes(raw_bytes: bytes) -> str:
    for encoding in ["utf-8", "gbk", "gb2312", "latin-1"]:
        try:
            return raw_bytes.decode(encoding)
        except UnicodeDecodeError:
            continue
    return raw_bytes.decode("utf-8", errors="ignore")


def estimate_process_risk(request: ReportDetectionRequest, report_length: int) -> float:
    draft_count = max(request.draftSaveCount or 0, 0)
    previous_count = len(request.previousVersions)
    first_submit_risk = 0.25 if (request.versionNo or 1) <= 1 else 0.05
    weak_draft_risk = 0.40 if draft_count <= 1 else 0.24 if draft_count <= 3 else 0.10
    large_report_risk = 0.22 if report_length >= 1500 and draft_count <= 1 else 0.10 if report_length >= 800 else 0.04
    history_risk = 0.16 if previous_count == 0 else 0.08
    return min(first_submit_risk + weak_draft_risk + large_report_risk + history_risk, 1.0)


def extract_keywords(text: str) -> list[str]:
    candidates = re.findall(r"[A-Za-z_]{3,}|[\u4e00-\u9fff]{2,}", text or "")
    filtered = [token.lower() for token in candidates if len(token.strip()) >= 2]
    counter = Counter(filtered)
    return [item for item, _ in counter.most_common(16)]


def estimate_specificity(chunk_text: str, keywords: list[str]) -> int:
    if not chunk_text:
        return 0
    if not keywords:
        return 55
    lowered = chunk_text.lower()
    hit_count = sum(1 for keyword in keywords if keyword in lowered)
    digit_bonus = 1 if re.search(r"\d", chunk_text) else 0
    code_bonus = 1 if re.search(r"[{}();=_<>]", chunk_text) else 0
    raw = min(hit_count + digit_bonus + code_bonus, 6)
    return clamp_int(round(raw / 6 * 100), 0, 100)


def best_reference(chunk_text: str, references: list[dict[str, str]], source_type: str) -> tuple[float, dict[str, Any] | None]:
    best_score = 0.0
    best_item: dict[str, str] | None = None
    for item in references:
        score = compare_texts(chunk_text, item.get("text", ""))
        if score > best_score:
            best_score = score
            best_item = item
    if not best_item or best_score <= 0:
        return 0.0, None
    return best_score, {
        "sourceType": source_type,
        "title": best_item.get("title", source_type),
        "similarity": round(best_score, 4),
        "note": "最高相似参考",
        "excerpt": shorten(best_item.get("text", ""), 120),
    }


def compare_texts(left: str, right: str) -> float:
    left_norm = compact_for_similarity(left)
    right_norm = compact_for_similarity(right)
    if not left_norm or not right_norm:
        return 0.0
    cosine = cosine_similarity(char_ngrams(left_norm), char_ngrams(right_norm))
    overlap = jaccard_similarity(set(char_ngrams(left_norm)), set(char_ngrams(right_norm)))
    return min(1.0, cosine * 0.75 + overlap * 0.25)


def compact_for_similarity(text: str) -> str:
    compact = re.sub(r"\s+", "", text or "")
    return compact[:2200]


def char_ngrams(text: str, n: int = 2) -> Counter[str]:
    if not text:
        return Counter()
    if len(text) <= n:
        return Counter([text])
    return Counter(text[index : index + n] for index in range(len(text) - n + 1))


def cosine_similarity(left: Counter[str], right: Counter[str]) -> float:
    if not left or not right:
        return 0.0
    common_keys = set(left) & set(right)
    numerator = sum(left[key] * right[key] for key in common_keys)
    left_norm = math.sqrt(sum(value * value for value in left.values()))
    right_norm = math.sqrt(sum(value * value for value in right.values()))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return numerator / (left_norm * right_norm)


def jaccard_similarity(left: set[str], right: set[str]) -> float:
    if not left or not right:
        return 0.0
    union = left | right
    if not union:
        return 0.0
    return len(left & right) / len(union)


def build_prompt(
    request: ReportDetectionRequest,
    report_text: str,
    attachment_summaries: list[dict[str, Any]],
    chunk_evidences: list[ChunkEvidence],
    feature_summary: dict[str, Any],
) -> str:
    prompt_payload = {
        "experimentTitle": request.experimentTitle or "",
        "experimentObjective": shorten(request.experimentObjective or "", 240),
        "experimentContentText": shorten(request.experimentContentText or "", 320),
        "studentName": request.studentName or "",
        "reportWordCount": request.wordCount or len(report_text),
        "draftSaveCount": request.draftSaveCount or 0,
        "versionNo": request.versionNo or 1,
        "featureSummary": feature_summary,
        "attachmentSummaries": attachment_summaries,
        "chunkEvidences": [
            {
                "chunkIndex": item.chunk_index,
                "chunkText": shorten(item.chunk_text, 220),
                "heuristicScore": item.heuristic_score,
                "templateSimilarity": item.template_similarity,
                "peerSimilarity": item.peer_similarity,
                "historySimilarity": item.history_similarity,
                "attachmentSimilarity": item.attachment_similarity,
                "specificityScore": item.specificity_score,
                "reasons": item.reasons,
                "matchedRefs": item.matched_refs,
            }
            for item in chunk_evidences
        ],
    }
    return f"""
You are an academic integrity assistant for undergraduate laboratory reports.
Estimate the AI-generated ratio of the report by fusing multiple pieces of evidence.
Do not use a single style detector as the only basis.

Decision rules:
- High risk requires at least two independent evidence types to be strong at the same time, such as template similarity, peer similarity, weak draft/process signal, or mismatch with attachments.
- Generic polished writing alone is not enough to call a report high risk.
- If evidence is mixed or weak, reduce both risk level and confidence.
- Output language must be Simplified Chinese.
- Return JSON only. Do not output markdown.

Return exactly this shape:
{{
  "overallAiRatio": 0,
  "riskLevel": "LOW|MEDIUM|HIGH",
  "confidence": 0,
  "needsHumanReview": true,
  "summary": "一段简短结论",
  "explanations": ["证据1", "证据2"],
  "suspiciousChunks": [
    {{
      "chunkIndex": 0,
      "chunkText": "可疑片段原文摘录",
      "aiScore": 0,
      "riskLevel": "LOW|MEDIUM|HIGH",
      "confidence": 0,
      "reasons": ["原因1", "原因2"],
      "matchedRefs": [
        {{
          "sourceType": "TEMPLATE|PEER|SELF_HISTORY|ATTACHMENT",
          "title": "证据标题",
          "similarity": 0.0,
          "note": "说明",
          "excerpt": "参考摘录"
        }}
      ]
    }}
  ]
}}

Analysis context JSON:
{json.dumps(prompt_payload, ensure_ascii=False)}
""".strip()


def chat_json(client: OpenAI, model: str, prompt: str) -> str:
    try:
        completion = client.chat.completions.create(
            model=model,
            temperature=0.2,
            messages=[
                {
                    "role": "system",
                    "content": "Return valid JSON only. Do not output markdown or extra explanations.",
                },
                {"role": "user", "content": prompt},
            ],
        )
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"model call failed: {exc}") from exc

    text = extract_message_text(completion)
    if not text:
        raise HTTPException(status_code=502, detail="model output is empty")
    return text


def extract_message_text(completion: Any) -> str:
    try:
        message = completion.choices[0].message
        content = message.content
        if isinstance(content, str):
            return content.strip()
        if isinstance(content, list):
            parts: list[str] = []
            for item in content:
                text = getattr(item, "text", None)
                if text:
                    parts.append(text)
                elif isinstance(item, dict) and item.get("text"):
                    parts.append(str(item["text"]))
            return "\n".join(parts).strip()
    except Exception:
        return ""
    return ""


def parse_model_json(raw_text: str) -> Any:
    text = raw_text.strip()
    fenced_match = re.search(r"```(?:json)?\s*(.*?)```", text, re.DOTALL | re.IGNORECASE)
    if fenced_match:
        text = fenced_match.group(1).strip()

    for candidate in [text, extract_json_slice(text, "{", "}"), extract_json_slice(text, "[", "]")]:
        if not candidate:
            continue
        try:
            return json.loads(candidate)
        except json.JSONDecodeError:
            continue
    raise HTTPException(status_code=502, detail="model output is not valid JSON")


def extract_json_slice(text: str, left: str, right: str) -> str:
    start = text.find(left)
    end = text.rfind(right)
    if start == -1 or end == -1 or end <= start:
        return ""
    return text[start : end + 1]


def sanitize_response(
    parsed: Any,
    model: str,
    raw_text: str,
    feature_summary: dict[str, Any],
    chunk_evidences: list[ChunkEvidence],
) -> ReportDetectionResponse:
    if not isinstance(parsed, dict):
        raise HTTPException(status_code=502, detail="report detection response format invalid")

    heuristic_ratio = derive_heuristic_ratio(feature_summary, chunk_evidences)
    heuristic_explanations = build_heuristic_explanations(feature_summary, chunk_evidences)
    fallback_chunks = build_fallback_suspicious_chunks(chunk_evidences)

    overall_ai_ratio = clamp_int(int_or_default(parsed.get("overallAiRatio"), 0), 0, 100)
    risk_level = normalize_risk_level(parsed.get("riskLevel"))
    confidence = clamp_int(int_or_default(parsed.get("confidence"), 60), 0, 100)
    summary = stringify(parsed.get("summary")) or "已完成实验报告 AI 风险估计。"
    explanations = merge_strings(
        normalize_string_list(parsed.get("explanations"), limit=6),
        heuristic_explanations,
        limit=6,
    )
    needs_human_review = bool(parsed.get("needsHumanReview")) or risk_level in {"MEDIUM", "HIGH"}

    suspicious_chunks: list[SuspiciousChunkItem] = []
    items = parsed.get("suspiciousChunks")
    if isinstance(items, list):
        for item in items[:8]:
            sanitized = sanitize_chunk_item(item)
            if sanitized is not None:
                suspicious_chunks.append(sanitized)

    suspicious_chunks = merge_suspicious_chunks(suspicious_chunks, fallback_chunks)
    overall_ai_ratio = normalize_ratio_with_risk(
        overall_ai_ratio,
        risk_level,
        heuristic_ratio,
        suspicious_chunks,
    )
    risk_level = normalize_risk_with_ratio(risk_level, overall_ai_ratio, feature_summary)
    summary = enrich_summary(summary, feature_summary, explanations)
    needs_human_review = needs_human_review or feature_summary.get("maxPeerSimilarity", 0) >= 0.58

    return ReportDetectionResponse(
        success=True,
        model=model,
        overallAiRatio=overall_ai_ratio,
        riskLevel=risk_level,
        confidence=confidence,
        needsHumanReview=needs_human_review,
        summary=summary,
        explanations=explanations,
        featureSummary=feature_summary,
        suspiciousChunks=suspicious_chunks,
        generatedAt=datetime.now(timezone.utc).isoformat(),
        rawText=raw_text,
    )


def sanitize_chunk_item(item: Any) -> SuspiciousChunkItem | None:
    if not isinstance(item, dict):
        return None
    chunk_text = stringify(item.get("chunkText"))
    if not chunk_text:
        return None
    refs: list[EvidenceReference] = []
    matched_refs = item.get("matchedRefs")
    if isinstance(matched_refs, list):
        for ref in matched_refs[:4]:
            sanitized_ref = sanitize_ref_item(ref)
            if sanitized_ref is not None:
                refs.append(sanitized_ref)
    return SuspiciousChunkItem(
        chunkIndex=int_or_default(item.get("chunkIndex"), 0),
        chunkText=shorten(chunk_text, 260),
        aiScore=clamp_int(int_or_default(item.get("aiScore"), 0), 0, 100),
        riskLevel=normalize_risk_level(item.get("riskLevel")),
        confidence=clamp_int(int_or_default(item.get("confidence"), 60), 0, 100),
        reasons=normalize_string_list(item.get("reasons"), limit=4),
        matchedRefs=refs,
    )


def sanitize_ref_item(item: Any) -> EvidenceReference | None:
    if not isinstance(item, dict):
        return None
    title = stringify(item.get("title"))
    source_type = stringify(item.get("sourceType")).upper()
    if not title or not source_type:
        return None
    return EvidenceReference(
        sourceType=source_type,
        title=title,
        similarity=round(float_or_default(item.get("similarity"), 0.0), 4),
        note=stringify(item.get("note")),
        excerpt=shorten(stringify(item.get("excerpt")), 140),
    )


def normalize_risk_level(value: Any) -> str:
    text = stringify(value).upper()
    return text if text in RISK_LEVELS else "MEDIUM"


def normalize_string_list(value: Any, limit: int) -> list[str]:
    if not isinstance(value, list):
        return []
    result: list[str] = []
    for item in value:
        text = stringify(item)
        if text:
            result.append(text)
        if len(result) >= limit:
            break
    return result


def merge_strings(primary: list[str], secondary: list[str], limit: int) -> list[str]:
    result: list[str] = []
    for item in primary + secondary:
        text = stringify(item)
        if not text or text in result:
            continue
        result.append(text)
        if len(result) >= limit:
            break
    return result


def derive_heuristic_ratio(feature_summary: dict[str, Any], chunk_evidences: list[ChunkEvidence]) -> int:
    max_heuristic = int_or_default(feature_summary.get("maxHeuristicScore"), 0)
    max_peer = float_or_default(feature_summary.get("maxPeerSimilarity"), 0.0)
    max_template = float_or_default(feature_summary.get("maxTemplateSimilarity"), 0.0)
    process_risk = float_or_default(feature_summary.get("processRisk"), 0.0)

    ratio = max_heuristic
    if max_peer >= 0.88:
        ratio = max(ratio, 78)
    elif max_peer >= 0.72:
        ratio = max(ratio, 58)
    elif max_peer >= 0.58:
        ratio = max(ratio, 45)

    if max_template >= 0.7 and process_risk >= 0.6:
        ratio = max(ratio, 42)
    return clamp_int(ratio, 0, 100)


def build_heuristic_explanations(
    feature_summary: dict[str, Any],
    chunk_evidences: list[ChunkEvidence],
) -> list[str]:
    explanations: list[str] = []
    max_peer = float_or_default(feature_summary.get("maxPeerSimilarity"), 0.0)
    max_template = float_or_default(feature_summary.get("maxTemplateSimilarity"), 0.0)
    process_risk = float_or_default(feature_summary.get("processRisk"), 0.0)
    max_attachment = float_or_default(feature_summary.get("maxAttachmentSimilarity"), 0.0)
    attachment_count = int_or_default(feature_summary.get("attachmentExtractedCount"), 0)

    if max_peer >= 0.72:
        explanations.append("检测到与同题其他学生提交存在较高文本相似度，建议重点核查是否有互相参考或复制粘贴。")
    elif max_peer >= 0.58:
        explanations.append("检测到与同题其他提交存在中度相似表达，建议结合原始过程记录人工复核。")

    if max_template >= 0.7:
        explanations.append("正文中有较多与实验题面或教师模板接近的表达，可能存在模板改写痕迹。")

    if process_risk >= 0.65:
        explanations.append("草稿保存次数偏少且成文较完整，过程性证据较弱。")

    if attachment_count > 0 and max_attachment <= 0.12:
        explanations.append("正文与附件可提取内容关联较弱，建议核查报告与附件是否互相支撑。")

    if not explanations and chunk_evidences:
        top_chunk = chunk_evidences[0]
        if top_chunk.reasons:
            explanations.append(f"系统检测到重点关注片段：{top_chunk.reasons[0]}")
    return explanations[:4]


def build_fallback_suspicious_chunks(chunk_evidences: list[ChunkEvidence]) -> list[SuspiciousChunkItem]:
    fallback: list[SuspiciousChunkItem] = []
    for item in chunk_evidences[:4]:
        has_peer_ref = any(ref.get("sourceType") == "PEER" for ref in item.matched_refs)
        if item.heuristic_score < 40 and not has_peer_ref:
            continue
        risk_level = heuristic_risk_level(item.heuristic_score)
        fallback.append(
            SuspiciousChunkItem(
                chunkIndex=item.chunk_index,
                chunkText=shorten(item.chunk_text, 260),
                aiScore=item.heuristic_score,
                riskLevel=risk_level,
                confidence=75 if item.heuristic_score >= 70 else 68 if item.heuristic_score >= 50 else 60,
                reasons=item.reasons[:4],
                matchedRefs=[sanitize_ref_item(ref) for ref in item.matched_refs if sanitize_ref_item(ref) is not None],
            )
        )
    return fallback


def merge_suspicious_chunks(
    model_chunks: list[SuspiciousChunkItem],
    fallback_chunks: list[SuspiciousChunkItem],
) -> list[SuspiciousChunkItem]:
    merged = list(model_chunks)
    existing_keys = {(item.chunkIndex, item.chunkText) for item in merged}
    has_peer_reference = any(
        any(ref.sourceType == "PEER" for ref in item.matchedRefs)
        for item in merged
    )

    for item in fallback_chunks:
        key = (item.chunkIndex, item.chunkText)
        if key in existing_keys:
            continue
        if has_peer_reference and any(ref.sourceType == "PEER" for ref in item.matchedRefs):
            continue
        merged.append(item)
        existing_keys.add(key)
        if any(ref.sourceType == "PEER" for ref in item.matchedRefs):
            has_peer_reference = True
        if len(merged) >= 8:
            break
    return merged[:8]


def normalize_ratio_with_risk(
    model_ratio: int,
    risk_level: str,
    heuristic_ratio: int,
    suspicious_chunks: list[SuspiciousChunkItem],
) -> int:
    ratio = model_ratio
    max_chunk_score = max((item.aiScore for item in suspicious_chunks), default=0)
    if ratio == 0 and heuristic_ratio >= 35:
        ratio = heuristic_ratio
    if risk_level == "MEDIUM":
        ratio = max(ratio, 35 if max_chunk_score < 50 else 45)
    elif risk_level == "HIGH":
        ratio = max(ratio, 70 if max_chunk_score >= 70 else 65)
    return clamp_int(max(ratio, min(heuristic_ratio, max_chunk_score)), 0, 100)


def normalize_risk_with_ratio(risk_level: str, ratio: int, feature_summary: dict[str, Any]) -> str:
    max_peer = float_or_default(feature_summary.get("maxPeerSimilarity"), 0.0)
    if ratio >= 70 or max_peer >= 0.85:
        return "HIGH"
    if ratio >= 45 or risk_level == "MEDIUM":
        return "MEDIUM"
    return "LOW"


def enrich_summary(summary: str, feature_summary: dict[str, Any], explanations: list[str]) -> str:
    text = stringify(summary) or "已完成实验报告 AI 风险估计。"
    max_peer = float_or_default(feature_summary.get("maxPeerSimilarity"), 0.0)
    peer_note_added = False
    if max_peer >= 0.72 and "同题" not in text and "相似" not in text:
        text = f"{text} 检测到与同题其他学生提交存在较高相似表达。"
        peer_note_added = True
    elif max_peer >= 0.58 and "同题" not in text and "相似" not in text:
        text = f"{text} 同题提交间存在一定相似表达，建议人工复核。"
        peer_note_added = True
    if not explanations:
        return text
    first = explanations[0]
    if first and first not in text and len(text) < 90 and not (peer_note_added and "同题" in first):
        text = f"{text} {first}"
    return text


def heuristic_risk_level(score: int) -> str:
    if score >= 70:
        return "HIGH"
    if score >= 45:
        return "MEDIUM"
    return "LOW"


def shorten(text: str, limit: int) -> str:
    compact = normalize_plain_text(text)
    if len(compact) <= limit:
        return compact
    return compact[: max(limit - 3, 1)].rstrip() + "..."


def max_or_zero(values: list[float]) -> float:
    return max(values) if values else 0.0


def clamp_int(value: int, low: int, high: int) -> int:
    return min(max(value, low), high)


def int_or_default(value: Any, default: int) -> int:
    try:
        return int(value)
    except Exception:
        return default


def int_or_none(value: Any) -> int | None:
    try:
        return int(value)
    except Exception:
        return None


def float_or_default(value: Any, default: float) -> float:
    try:
        return float(value)
    except Exception:
        return default


def stringify(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("AI_REPORT_DETECTION_PORT", "8003")))
