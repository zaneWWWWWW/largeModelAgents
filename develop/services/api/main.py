from fastapi import FastAPI
from pydantic import BaseModel
from typing import List

app = FastAPI(title="Large Model Agents API", version="0.1.0")


class ChatRequest(BaseModel):
    user_id: str
    message: str


class ChatResponse(BaseModel):
    reply: str


class AssessmentRequest(BaseModel):
    user_id: str
    instrument: str
    answers: List[int]


class AssessmentResponse(BaseModel):
    score: int
    category: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    # TODO: 接入 counseling 模型推理
    return ChatResponse(reply="你好，我是你的心理助理。目前为占位回复。")


@app.post("/assessment/submit", response_model=AssessmentResponse)
def assess(req: AssessmentRequest):
    # 简单示例评分（实际评分由 models/assessment 提供）
    score = sum(req.answers)
    category = "无症状"
    if 5 <= score <= 9:
        category = "轻度"
    elif 10 <= score <= 14:
        category = "中度"
    elif 15 <= score <= 19:
        category = "中重度"
    elif score >= 20:
        category = "重度"
    return AssessmentResponse(score=score, category=category)