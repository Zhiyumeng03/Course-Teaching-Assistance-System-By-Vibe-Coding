import json
import os
import re
from typing import Any

from fastapi import FastAPI, HTTPException
from openai import OpenAI
from pydantic import BaseModel, Field


DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
DEFAULT_MODEL = "qwen3-max"
MASTERY_LEVELS = {"POOR", "FAIR", "GOOD", "EXCELLENT"}

app = FastAPI(title="AI Practice Diagnosis Agent", version="1.0.0")


class PracticeAnswerItem(BaseModel):
    answerId: int
    paperQuestionId: int | None = None
    questionId: int | None = None
    sortNo: int | None = None
    questionType: str | None = None
    stem: str
    content: dict[str, Any] = Field(default_factory=dict)
    referenceAnswer: dict[str, Any] = Field(default_factory=dict)
    analysisText: str | None = None
    knowledgePointNames: list[str] = Field(default_factory=list)
    studentAnswer: dict[str, Any] = Field(default_factory=dict)
    score: int | None = None
    fullScore: int | None = None
    isCorrect: int | None = None


class PracticeDiagnosisRequest(BaseModel):
    recordId: int
    courseId: int | None = None
    courseName: str | None = None
    paperId: int | None = None
    paperTitle: str | None = None
    studentId: int | None = None
    studentName: str | None = None
    objectiveScore: int | None = None
    totalScore: int | None = None
    answers: list[PracticeAnswerItem] = Field(default_factory=list)


class PracticeAnswerFeedbackItem(BaseModel):
    answerId: int
    paperQuestionId: int | None = None
    questionId: int | None = None
    masteryLevel: str
    knowledgePoints: list[dict[str, Any]] = Field(default_factory=list)
    summary: str
    improvement: str
    extra: dict[str, Any] = Field(default_factory=dict)


class PracticeDiagnosisResponse(BaseModel):
    success: bool
    model: str
    answerFeedbacks: list[PracticeAnswerFeedbackItem] = Field(default_factory=list)
    diagnosisJson: dict[str, Any] = Field(default_factory=dict)
    diagnosisText: str = ""


@app.get("/health")
def health() -> dict[str, Any]:
    # 健康检查只确认模型调用所需配置是否已准备好，不触发真实推理。
    key = os.getenv("OPENAI_API_KEY", "").strip()
    return {
        "success": bool(key),
        "configured": bool(key),
        "model": os.getenv("PRACTICE_DIAGNOSIS_MODEL", DEFAULT_MODEL),
    }


@app.post("/diagnose-practice", response_model=PracticeDiagnosisResponse)
def diagnose_practice(request: PracticeDiagnosisRequest) -> PracticeDiagnosisResponse:
    # 入口流程：校验请求 -> 初始化模型客户端 -> 逐题诊断 -> 汇总整体诊断。
    if not request.answers:
        raise HTTPException(status_code=400, detail="answers is required")

    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY is not configured")

    model = os.getenv("PRACTICE_DIAGNOSIS_MODEL", DEFAULT_MODEL).strip() or DEFAULT_MODEL
    client = OpenAI(
        api_key=api_key,
        base_url=os.getenv("OPENAI_BASE_URL", DEFAULT_BASE_URL).strip() or DEFAULT_BASE_URL,
    )

    answer_feedbacks = generate_answer_feedbacks(client, model, request)
    diagnosis_json, diagnosis_text = generate_summary_diagnosis(client, model, request, answer_feedbacks)
    return PracticeDiagnosisResponse(
        success=True,
        model=model,
        answerFeedbacks=answer_feedbacks,
        diagnosisJson=diagnosis_json,
        diagnosisText=diagnosis_text,
    )


def generate_answer_feedbacks(
    client: OpenAI,
    model: str,
    request: PracticeDiagnosisRequest,
) -> list[PracticeAnswerFeedbackItem]:
    # 第一步：让大模型逐题输出诊断结果，再把返回内容清洗成稳定结构。
    prompt = build_answer_feedback_prompt(request)
    raw_text = chat_json(client, model, prompt)
    parsed = parse_model_json(raw_text)
    feedback_candidates = parsed.get("answerFeedbacks") if isinstance(parsed, dict) else parsed
    if not isinstance(feedback_candidates, list):
        raise HTTPException(status_code=502, detail="answerFeedbacks format invalid")

    request_answer_map = {item.answerId: item for item in request.answers}
    result: list[PracticeAnswerFeedbackItem] = []
    for item in feedback_candidates:
        feedback = sanitize_feedback_item(item, request_answer_map)
        if feedback is not None:
            result.append(feedback)

    if not result:
        raise HTTPException(status_code=502, detail="model returned no valid answer feedbacks")
    return result


def generate_summary_diagnosis(
    client: OpenAI,
    model: str,
    request: PracticeDiagnosisRequest,
    answer_feedbacks: list[PracticeAnswerFeedbackItem],
) -> tuple[dict[str, Any], str]:
    # 第二步：基于逐题反馈做整体学习诊断，产出结构化摘要和面向学生的建议文案。
    prompt = build_summary_prompt(request, answer_feedbacks)
    raw_text = chat_json(client, model, prompt)
    parsed = parse_model_json(raw_text)
    if not isinstance(parsed, dict):
        raise HTTPException(status_code=502, detail="diagnosis output format invalid")

    diagnosis_json = parsed.get("diagnosisJson")
    diagnosis_text = stringify(parsed.get("diagnosisText"))
    if not isinstance(diagnosis_json, dict):
        diagnosis_json = {}
    if not diagnosis_text:
        diagnosis_text = "建议继续结合薄弱知识点进行针对性训练。"
    return diagnosis_json, diagnosis_text


def build_answer_feedback_prompt(request: PracticeDiagnosisRequest) -> str:
    # 直接把练习记录序列化给模型，并在提示词里约束输出字段和枚举值。
    payload = request.model_dump()
    return f"""
You are a learning diagnosis assistant.
Analyze each practice answer and return JSON only.

Rules:
- Output language must be Simplified Chinese.
- Return exactly one feedback item for each answerId in the request.
- Output shape:
  {{
    "answerFeedbacks": [
      {{
        "answerId": 1,
        "paperQuestionId": 1,
        "questionId": 1,
        "masteryLevel": "GOOD",
        "knowledgePoints": [
          {{"name": "知识点A", "mastery": "较好", "evidence": "学生表现...", "advice": "继续..."}}
        ],
        "summary": "对本题掌握情况的简短评价",
        "improvement": "后续改进建议",
        "extra": {{"scoreRate": 0.8}}
      }}
    ]
  }}
- masteryLevel must be one of: POOR, FAIR, GOOD, EXCELLENT.
- knowledgePoints can be empty, but if the input provides knowledgePointNames, you should prioritize them.
- Consider referenceAnswer, analysisText, score/fullScore, and studentAnswer together.

Request JSON:
{json.dumps(payload, ensure_ascii=False)}
""".strip()


def build_summary_prompt(
    request: PracticeDiagnosisRequest,
    answer_feedbacks: list[PracticeAnswerFeedbackItem],
) -> str:
    # 汇总阶段只传递必要上下文，避免模型重复读取完整题目原文。
    context = {
        "recordId": request.recordId,
        "courseName": request.courseName,
        "paperTitle": request.paperTitle,
        "studentName": request.studentName,
        "objectiveScore": request.objectiveScore,
        "totalScore": request.totalScore,
        "answerFeedbacks": [item.model_dump() for item in answer_feedbacks],
    }
    return f"""
You are a learning diagnosis assistant.
Summarize the answer feedbacks and return JSON only.

Rules:
- Output language must be Simplified Chinese.
- Output shape:
  {{
    "diagnosisJson": {{
      "overallLevel": "GOOD",
      "strengths": ["..."],
      "weakKnowledgePoints": ["..."],
      "nextActions": ["..."],
      "scoreSummary": {{"objectiveScore": 10, "totalScore": 16}}
    }},
    "diagnosisText": "给学生的个性化学习建议，2-4 句。"
  }}
- overallLevel must be one of: POOR, FAIR, GOOD, EXCELLENT.
- diagnosisText should be direct, practical, and encouraging.

Context JSON:
{json.dumps(context, ensure_ascii=False)}
""".strip()


def sanitize_feedback_item(
    item: Any,
    request_answer_map: dict[int, PracticeAnswerItem],
) -> PracticeAnswerFeedbackItem | None:
    # 对模型输出做兜底，避免 answerId 缺失、掌握等级非法等情况影响接口稳定性。
    if not isinstance(item, dict):
        return None

    answer_id = int_or_none(item.get("answerId"))
    if answer_id is None or answer_id not in request_answer_map:
        return None

    request_answer = request_answer_map[answer_id]
    mastery_level = stringify(item.get("masteryLevel")).upper()
    if mastery_level not in MASTERY_LEVELS:
        mastery_level = infer_mastery_level(request_answer)

    knowledge_points = item.get("knowledgePoints")
    if not isinstance(knowledge_points, list):
        knowledge_points = []

    summary = stringify(item.get("summary")) or "已完成本题诊断。"
    improvement = stringify(item.get("improvement")) or "建议继续围绕本题涉及知识点进行复盘。"
    extra = item.get("extra") if isinstance(item.get("extra"), dict) else {}

    return PracticeAnswerFeedbackItem(
        answerId=answer_id,
        paperQuestionId=int_or_none(item.get("paperQuestionId")) or request_answer.paperQuestionId,
        questionId=int_or_none(item.get("questionId")) or request_answer.questionId,
        masteryLevel=mastery_level,
        knowledgePoints=knowledge_points,
        summary=summary,
        improvement=improvement,
        extra=extra,
    )


def infer_mastery_level(answer: PracticeAnswerItem) -> str:
    # 当模型没有给出合法掌握等级时，按得分率/对错情况推断一个默认等级。
    if answer.fullScore and answer.score is not None:
        rate = answer.score / max(answer.fullScore, 1)
        if rate >= 0.9:
            return "EXCELLENT"
        if rate >= 0.7:
            return "GOOD"
        if rate >= 0.4:
            return "FAIR"
        return "POOR"
    if answer.isCorrect == 1:
        return "GOOD"
    if answer.isCorrect == 0:
        return "POOR"
    return "FAIR"


def chat_json(client: OpenAI, model: str, prompt: str) -> str:
    # 所有模型调用统一走这里，便于复用异常处理和 JSON 输出约束。
    try:
        completion = client.chat.completions.create(
            model=model,
            temperature=0.4,
            messages=[
                {
                    "role": "system",
                    "content": "Return valid JSON only. Do not use markdown or extra explanations.",
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
    # 兼容不同 SDK 返回的 content 形态：纯字符串或分段列表。
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
    # 模型偶尔会包裹 ```json 代码块或附带多余说明，这里尽量从中提取 JSON。
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
    # 从混杂文本里截取最外层对象/数组，作为 JSON 解析的兜底尝试。
    start = text.find(left)
    end = text.rfind(right)
    if start == -1 or end == -1 or end <= start:
        return ""
    return text[start : end + 1]


def int_or_none(value: Any) -> int | None:
    try:
        return int(value)
    except Exception:
        return None


def stringify(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("AI_PRACTICE_DIAGNOSIS_PORT", "8002")))
