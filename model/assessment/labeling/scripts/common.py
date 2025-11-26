import orjson

def jsonl_read(path):
    with open(path, 'rb') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            yield orjson.loads(line)

def jsonl_write(path, items):
    with open(path, 'wb') as w:
        for it in items:
            w.write(orjson.dumps(it))
            w.write(b"\n")

def clip_conversation(convo, max_chars=2000, max_turns=12):
    total = 0
    trimmed = []
    for t in reversed(convo):
        total += len(t.get('text', ''))
        trimmed.append(t)
        if total >= max_chars:
            break
    trimmed = list(reversed(trimmed))
    if len(trimmed) > max_turns:
        trimmed = trimmed[-max_turns:]
    return trimmed

def build_prompt(instruction, convo):
    parts = [instruction.strip(), "\n\n对话：\n"]
    for turn in convo:
        role = turn.get('role', 'user')
        text = turn.get('text', '')
        parts.append(f"{role}: {text}\n")
    parts.append("\n只输出 JSON。")
    return "".join(parts)

def validate_labels(obj):
    assert set(obj.keys()) == {"depression_level","anxiety_level","risk_flag","student_distress_score"}
    assert obj["depression_level"] in [0,1,2,3]
    assert obj["anxiety_level"] in [0,1,2,3]
    assert obj["risk_flag"] in ["none","suicidal","self_harm","violence"]
    assert isinstance(obj["student_distress_score"], int) and 0 <= obj["student_distress_score"] <= 9