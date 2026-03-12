import json
import os
import re
from typing import Any

from fastapi import FastAPI, HTTPException, Request
from openai import OpenAI
from pydantic import BaseModel, Field, ValidationError


TYPE_SET = {"SINGLE", "MULTI", "JUDGE", "BLANK", "SHORT", "MIXED"}
DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
DEFAULT_MODEL = "qwen3-max"
MAX_COUNT = 10

app = FastAPI(title="AI Question Agent", version="1.0.0")


class GenerateQuestionsRequest(BaseModel):
    courseId: int
    courseName: str | None = None
    knowledgePoints: list[str] = Field(default_factory=list)
    questionType: str
    difficulty: int = 3
    count: int = 1
    language: str = "zh-CN"
    extraRequirements: str | None = None


class QuestionItem(BaseModel):
    type: str
    stem: str
    content: dict[str, Any]
    answer: dict[str, Any]
    analysisText: str | None = None
    difficulty: int
    knowledgePointNames: list[str] = Field(default_factory=list)


class GenerateQuestionsResponse(BaseModel):
    success: bool
    model: str
    questions: list[QuestionItem] = Field(default_factory=list)
    rawText: str | None = None


@app.post("/health")
def health() -> dict[str, Any]:
    key = os.getenv("OPENAI_API_KEY", "").strip()
    return {
        "success": bool(key),
        "configured": bool(key),
        "model": os.getenv("QWEN_MODEL", DEFAULT_MODEL),
    }


@app.post("/generate-questions", response_model=GenerateQuestionsResponse)
async def generate_questions(http_request: Request) -> GenerateQuestionsResponse:
    raw_body = await http_request.body()
    print(f"[ai-question-agent] raw body bytes={raw_body!r}")
    if not raw_body:
        raise HTTPException(status_code=422, detail="request body is empty")

    try:
        request = GenerateQuestionsRequest.model_validate_json(raw_body)
    except ValidationError as exc:
        raise HTTPException(status_code=422, detail=json.loads(exc.json())) from exc

    api_key = os.getenv("OPENAI_API_KEY", "").strip()
    if not api_key:
        raise HTTPException(status_code=500, detail="OPENAI_API_KEY is not configured")

    normalized_type = normalize_type(request.questionType)
    if normalized_type not in TYPE_SET:
        raise HTTPException(status_code=400, detail="unsupported questionType")

    difficulty = clamp_difficulty(request.difficulty)
    count = min(max(request.count, 1), MAX_COUNT)
    model = os.getenv("QWEN_MODEL", DEFAULT_MODEL).strip() or DEFAULT_MODEL

    client = OpenAI(
        api_key=api_key,
        base_url=os.getenv("OPENAI_BASE_URL", DEFAULT_BASE_URL).strip() or DEFAULT_BASE_URL,
    )

    prompt = build_prompt(request, normalized_type, difficulty, count)
    try:
        completion = client.chat.completions.create(
            model=model,
            temperature=0.7,
            messages=[
                {
                    "role": "system",
                    "content": (
                        "You are a question-bank generator. Output valid JSON only. "
                        "Do not use markdown, code fences, explanations, or extra text. "
                        "Return a JSON object with shape {\"questions\": [...]}"
                    ),
                },
                {"role": "user", "content": prompt},
            ],
        )
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"model call failed: {exc}") from exc

    raw_text = extract_message_text(completion)
    print(f"[ai-question-agent] model raw text={raw_text}")
    parsed = parse_model_json(raw_text)
    question_candidates = parsed.get("questions") if isinstance(parsed, dict) else parsed
    if not isinstance(question_candidates, list):
        raise HTTPException(status_code=502, detail="model output format invalid")

    cleaned_questions: list[QuestionItem] = []
    discard_reasons: list[str] = []
    for index, item in enumerate(question_candidates):
        cleaned, reason = sanitize_question(item, difficulty, request.knowledgePoints, normalized_type)
        if cleaned is not None:
            cleaned_questions.append(cleaned)
        else:
            discard_reasons.append(f"item[{index}] {reason}")
        if len(cleaned_questions) >= count:
            break

    if discard_reasons:
        print("[ai-question-agent] discarded items:", discard_reasons)

    if not cleaned_questions:
        raise HTTPException(status_code=502, detail="model returned no valid questions")

    return GenerateQuestionsResponse(
        success=True,
        model=model,
        questions=cleaned_questions,
        rawText=raw_text,
    )


def build_prompt(
    request: GenerateQuestionsRequest,
    normalized_type: str,
    difficulty: int,
    count: int,
) -> str:
    knowledge_text = ", ".join([item for item in request.knowledgePoints if item.strip()]) or "none"
    extra = (request.extraRequirements or "").strip() or "none"
    example_knowledge = request.knowledgePoints[0] if request.knowledgePoints else "general"
    example_block = build_example_block(normalized_type, difficulty, example_knowledge)
    type_rule = """
Rules:
- Allowed question item type values: SINGLE, MULTI, JUDGE, BLANK, SHORT.
- SINGLE/MULTI/JUDGE: content must be an object like {"A":"...","B":"..."}. answer must be an object where each key exists in content and each answer value exactly matches content[key].
- For JUDGE specifically, prefer content {"T":"正确","F":"错误"} and answer {"T":"正确"} or {"F":"错误"}.
- BLANK: content must be an object like {"1":"blank 1","2":"blank 2"}. answer must be an object with the same keys.
- SHORT: content may be {} or {"guide":"..."}. answer must be {"text":"..."}.
- Every item must include: type, stem, content, answer, analysisText, difficulty, knowledgePointNames.
- difficulty must be an integer from 1 to 5.
- knowledgePointNames must be an array of strings and should prefer the provided knowledge points.
- The output language for stem, options, answers, and analysisText must be Simplified Chinese.
- If questionType is not MIXED, every question must use exactly that type.
"""
    return f"""
Generate course questions and return JSON only.

courseId: {request.courseId}
courseName: {request.courseName or "unknown"}
knowledgePoints: {knowledge_text}
questionType: {normalized_type}
difficulty: {difficulty}
count: {count}
language: {request.language}
extraRequirements: {extra}

{type_rule}

Output example:
{example_block}
""".strip()


def build_example_block(normalized_type: str, difficulty: int, example_knowledge: str) -> str:
    if normalized_type == "JUDGE":
        return f"""{{
  "questions": [
    {{
      "type": "JUDGE",
      "stem": "Spring Boot supports dependency injection.",
      "content": {{"T": "正确", "F": "错误"}},
      "answer": {{"T": "正确"}},
      "analysisText": "Dependency injection is a core Spring capability.",
      "difficulty": {difficulty},
      "knowledgePointNames": ["{example_knowledge}"]
    }}
  ]
}}"""
    return f"""{{
  "questions": [
    {{
      "type": "SINGLE",
      "stem": "Sample stem in Chinese",
      "content": {{"A": "Option A", "B": "Option B", "C": "Option C", "D": "Option D"}},
      "answer": {{"A": "Option A"}},
      "analysisText": "Analysis in Chinese",
      "difficulty": {difficulty},
      "knowledgePointNames": ["{example_knowledge}"]
    }}
  ]
}}"""


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
        pass
    raise HTTPException(status_code=502, detail="model output is empty")


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


def sanitize_question(
    item: Any,
    fallback_difficulty: int,
    fallback_knowledge_points: list[str],
    requested_type: str,
) -> tuple[QuestionItem | None, str]:
    if not isinstance(item, dict):
        return None, "item is not object"

    q_type = normalize_type(item.get("type"))
    stem = stringify(item.get("stem"))
    content = item.get("content")
    answer = item.get("answer")
    analysis_text = stringify(item.get("analysisText")) or None
    difficulty = clamp_difficulty(int_or_default(item.get("difficulty"), fallback_difficulty))
    knowledge_point_names = normalize_knowledge_names(item.get("knowledgePointNames"), fallback_knowledge_points)

    if q_type not in TYPE_SET or q_type == "MIXED":
        return None, f"invalid type={q_type!r}"
    if requested_type != "MIXED" and q_type != requested_type:
        return None, f"type mismatch requested={requested_type} actual={q_type}"
    if not stem:
        return None, "stem is empty"
    if not isinstance(content, dict) or not isinstance(answer, dict):
        return None, "content or answer is not object"

    cleaned_content = clean_string_map(content)
    cleaned_answer = clean_string_map(answer)
    if q_type == "JUDGE":
        cleaned_content, cleaned_answer = normalize_judge_payload(cleaned_content, cleaned_answer)

    if not validate_type_payload(q_type, cleaned_content, cleaned_answer):
        return None, f"payload validation failed content={cleaned_content} answer={cleaned_answer}"

    return QuestionItem(
        type=q_type,
        stem=stem,
        content=cleaned_content,
        answer=cleaned_answer,
        analysisText=analysis_text,
        difficulty=difficulty,
        knowledgePointNames=knowledge_point_names,
    ), "ok"


def normalize_judge_payload(content: dict[str, str], answer: dict[str, str]) -> tuple[dict[str, str], dict[str, str]]:
    normalized_content: dict[str, str] = {"T": "正确", "F": "错误"}

    has_true = False
    has_false = False
    for key, value in content.items():
        normalized = normalize_judge_token(key) or normalize_judge_token(value)
        if normalized == "T":
            has_true = True
        elif normalized == "F":
            has_false = True

    if not (has_true and has_false):
        if len(content) == 2:
            has_true = True
            has_false = True
        else:
            return content, answer

    normalized_answer: dict[str, str] = {}
    for key, value in answer.items():
        normalized = normalize_judge_token(key) or normalize_judge_token(value)
        if normalized == "T":
            normalized_answer = {"T": "正确"}
            break
        if normalized == "F":
            normalized_answer = {"F": "错误"}
            break

    if not normalized_answer and len(answer) == 1:
        only_value = next(iter(answer.values()))
        normalized = normalize_judge_token(only_value)
        if normalized == "T":
            normalized_answer = {"T": "正确"}
        elif normalized == "F":
            normalized_answer = {"F": "错误"}

    return normalized_content, normalized_answer or answer


def normalize_judge_token(value: Any) -> str:
    text = stringify(value).lower()
    if text in {"t", "true", "yes", "1", "正确", "对", "是"}:
        return "T"
    if text in {"f", "false", "no", "0", "错误", "错", "否"}:
        return "F"
    return ""


def validate_type_payload(q_type: str, content: dict[str, Any], answer: dict[str, Any]) -> bool:
    if q_type in {"SINGLE", "MULTI"}:
        if len(content) < 2 or not answer:
            return False
        if q_type == "SINGLE" and len(answer) != 1:
            return False
        for key, value in answer.items():
            if key not in content or content[key] != value:
                return False
        return True
    if q_type == "JUDGE":
        if set(content.keys()) != {"T", "F"} or len(answer) != 1:
            return False
        for key, value in answer.items():
            if key not in content or content[key] != value:
                return False
        return True
    if q_type == "BLANK":
        if not content or not answer:
            return False
        return set(content.keys()) == set(answer.keys()) and all(stringify(v) for v in content.values()) and all(
            stringify(v) for v in answer.values()
        )
    if q_type == "SHORT":
        answer_text = stringify(answer.get("text"))
        if not answer_text:
            return False
        if content and "guide" in content and not stringify(content.get("guide")):
            return False
        return True
    return False


def clean_string_map(data: dict[str, Any]) -> dict[str, str]:
    cleaned: dict[str, str] = {}
    for key, value in data.items():
        k = stringify(key)
        v = stringify(value)
        if k and v:
            cleaned[k] = v
    return cleaned


def normalize_knowledge_names(value: Any, fallback: list[str]) -> list[str]:
    source = value if isinstance(value, list) else fallback
    result: list[str] = []
    for item in source:
        text = stringify(item)
        if text and text not in result:
            result.append(text)
    return result


def normalize_type(value: Any) -> str:
    return stringify(value).upper()


def clamp_difficulty(value: int) -> int:
    if value < 1:
        return 1
    if value > 5:
        return 5
    return value


def int_or_default(value: Any, default: int) -> int:
    try:
        return int(value)
    except Exception:
        return default


def stringify(value: Any) -> str:
    if value is None:
        return ""
    return str(value).strip()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=int(os.getenv("AI_AGENT_PORT", "8001")))