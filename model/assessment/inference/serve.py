from typing import Dict, List


def score_phq9(answers: List[int]) -> Dict:
    total = sum(answers)
    if total <= 4:
        cat = "无症状"
    elif total <= 9:
        cat = "轻度"
    elif total <= 14:
        cat = "中度"
    elif total <= 19:
        cat = "中重度"
    else:
        cat = "重度"
    return {"score": total, "category": cat}


def score_gad7(answers: List[int]) -> Dict:
    total = sum(answers)
    if total <= 4:
        cat = "最小"
    elif total <= 9:
        cat = "轻度"
    elif total <= 14:
        cat = "中度"
    else:
        cat = "重度"
    return {"score": total, "category": cat}