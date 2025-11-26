import os
import json

INSTRUCTION = (
    "请根据对话内容评估四项标签，并只输出JSON："
    "depression_level(0-3), anxiety_level(0-3), risk_flag(none|suicidal|self_harm|violence), student_distress_score(0-9)。"
)

def format_conversation(convo):
    parts = []
    for turn in convo:
        role = turn.get("role", "user")
        text = turn.get("text", "")
        parts.append(f"{role}: {text}")
    return "\n".join(parts)

def to_output_json(item):
    obj = {
        "depression_level": item.get("depression_level"),
        "anxiety_level": item.get("anxiety_level"),
        "risk_flag": item.get("risk_flag"),
        "student_distress_score": item.get("student_distress_score"),
    }
    return json.dumps(obj, ensure_ascii=False)

def main():
    root = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    src = os.path.join(root, "labeling", "repro_package", "results", "dataset.jsonl")
    out_dir = os.path.join(root, "data", "datasets")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "sft_student_mental.jsonl")

    total = 0
    kept = 0
    with open(src, "r", encoding="utf-8") as r, open(out_path, "w", encoding="utf-8") as w:
        for line in r:
            if not line.strip():
                continue
            total += 1
            try:
                item = json.loads(line)
            except Exception:
                continue
            if not all(k in item for k in ["depression_level","anxiety_level","risk_flag","student_distress_score","conversation"]):
                continue
            record = {
                "id": item.get("id"),
                "instruction": INSTRUCTION,
                "input": format_conversation(item.get("conversation", [])),
                "output": to_output_json(item)
            }
            w.write(json.dumps(record, ensure_ascii=False) + "\n")
            kept += 1
    print(json.dumps({"total": total, "kept": kept, "out": out_path}, ensure_ascii=False))

if __name__ == "__main__":
    main()