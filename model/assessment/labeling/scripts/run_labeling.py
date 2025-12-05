import os
import re
import time
import argparse
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
import orjson
from tqdm import tqdm

def read_instruction(p):
    return Path(p).read_text(encoding="utf-8")

def format_conversation(item, max_rounds, max_chars):
    conv = item.get("conversation") or item.get("messages") or []
    pairs = []
    for m in conv[-max_rounds:]:
        r = m.get("role") or "user"
        t = m.get("text") or m.get("content") or ""
        if r == "user":
            pairs.append("用户: " + t)
        elif r == "assistant":
            pairs.append("助理: " + t)
        else:
            pairs.append(r + ": " + t)
    s = "\n".join(pairs)
    if len(s) <= max_chars:
        return s
    return s[-max_chars:]

def build_messages(instruction, convo_text):
    return [{"role": "system", "content": instruction}, {"role": "user", "content": convo_text}]

def post_chat(base_url, api_key, model, messages, timeout_s=60, max_retries=3):
    url = base_url.rstrip("/")
    if not url.endswith("chat/completions"):
        if url.endswith("/v1"):
            url = url + "/chat/completions"
        elif url.endswith("/v1/"):
            url = url + "chat/completions"
        else:
            if url.endswith("/"):
                url = url + "v1/chat/completions"
            else:
                url = url + "/v1/chat/completions"
    payload = {"model": model, "messages": messages}
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {api_key}"}
    for i in range(max_retries):
        try:
            r = requests.post(url, headers=headers, data=orjson.dumps(payload), timeout=timeout_s)
            if r.status_code == 200:
                return r.json()
            if r.status_code in (429, 500, 502, 503, 504):
                time.sleep(min(2 ** i, 8))
                continue
            return {"error": {"status": r.status_code, "text": r.text}}
        except Exception as e:
            time.sleep(min(2 ** i, 8))
    return {"error": {"status": "timeout", "text": "retry_exhausted"}}

def extract_json(text):
    if isinstance(text, dict):
        return text
    if not isinstance(text, str):
        return None
    m = re.search(r"\{[\s\S]*\}", text)
    if not m:
        return None
    try:
        return orjson.loads(m.group(0))
    except Exception:
        return None

ALLOWED_FLAGS = {"none", "suicidal", "self_harm", "violence"}

def validate_result(obj):
    if not isinstance(obj, dict):
        return {"depression_level": 0, "anxiety_level": 0, "risk_flag": "none", "student_distress_score": 0}
    dl = obj.get("depression_level", 0)
    al = obj.get("anxiety_level", 0)
    rf = obj.get("risk_flag", "none")
    sd = obj.get("student_distress_score", 0)
    try:
        dl = int(dl)
    except Exception:
        dl = 0
    try:
        al = int(al)
    except Exception:
        al = 0
    try:
        sd = int(sd)
    except Exception:
        sd = 0
    if dl < 0 or dl > 3:
        dl = 0
    if al < 0 or al > 3:
        al = 0
    if sd < 0 or sd > 9:
        sd = max(0, min(sd, 9))
    if not isinstance(rf, str):
        rf = "none"
    rf = rf.strip().lower()
    if rf not in ALLOWED_FLAGS:
        rf = "none"
    return {"depression_level": dl, "anxiety_level": al, "risk_flag": rf, "student_distress_score": sd}

def mock_label(convo_text):
    t = convo_text.lower()
    dl = 0
    al = 0
    sd = 0
    rf = "none"
    if any(x in t for x in ["自杀", "轻生", "结束生命"]):
        rf = "suicidal"
    if any(x in t for x in ["自残", "割", "伤害自己"]):
        rf = "self_harm"
    if any(x in t for x in ["打人", "报复", "暴力"]):
        rf = "violence"
    if any(x in t for x in ["低落", "没兴趣", "不想动", "沮丧"]):
        dl = max(dl, 1)
    if any(x in t for x in ["持续低落", "每天都", "两个星期", "一个月"]):
        dl = max(dl, 2)
    if any(x in t for x in ["完全不想", "无法学习", "起不来", "崩溃"]):
        dl = max(dl, 3)
    if any(x in t for x in ["焦虑", "担心", "紧张", "压力大", "害怕"]):
        al = max(al, 1)
    if any(x in t for x in ["总是", "经常", "频繁", "睡不着", "失眠"]):
        al = max(al, 2)
    if any(x in t for x in ["惊恐", "强烈", "无法控制", "影响生活"]):
        al = max(al, 3)
    sd = min(9, dl * 2 + al * 2)
    return {"depression_level": dl, "anxiety_level": al, "risk_flag": rf, "student_distress_score": sd}

def process_item(item, instruction, base_url, api_key, model, max_rounds, max_chars, mock=False):
    convo = format_conversation(item, max_rounds, max_chars)
    if mock:
        lab = mock_label(convo)
        return lab, True
    messages = build_messages(instruction, convo)
    resp = post_chat(base_url, api_key, model, messages)
    if "error" in resp:
        lab = {"depression_level": 0, "anxiety_level": 0, "risk_flag": "none", "student_distress_score": 0}
        return lab, False
    choices = resp.get("choices") or []
    if not choices:
        lab = {"depression_level": 0, "anxiety_level": 0, "risk_flag": "none", "student_distress_score": 0}
        return lab, False
    content = choices[0].get("message", {}).get("content")
    obj = extract_json(content)
    ok = obj is not None
    lab = validate_result(obj)
    return lab, ok

def read_jsonl(path):
    res = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                res.append(orjson.loads(line))
            except Exception:
                pass
    return res

def write_jsonl(path, items):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        for obj in items:
            f.write(orjson.dumps(obj).decode("utf-8"))
            f.write("\n")

def append_log(log_path, entry):
    Path(log_path).parent.mkdir(parents=True, exist_ok=True)
    with open(log_path, "a", encoding="utf-8") as f:
        f.write(orjson.dumps(entry).decode("utf-8"))
        f.write("\n")

def process_file(input_path, output_path, instruction, base_url, api_key, model, max_rounds, max_chars, workers, mock=False, log_path=None):
    items = read_jsonl(input_path)
    results = [None] * len(items)
    success = 0
    start_t = time.time()
    with ThreadPoolExecutor(max_workers=workers) as ex:
        futures = {}
        for idx, it in enumerate(items):
            futures[ex.submit(process_item, it, instruction, base_url, api_key, model, max_rounds, max_chars, mock)] = idx
        bar = tqdm(total=len(items), desc=Path(input_path).name)
        for fut in as_completed(futures):
            idx = futures[fut]
            try:
                lab, ok = fut.result()
                if ok:
                    success += 1
                src = items[idx]
                merged = dict(src)
                merged.update(lab)
                results[idx] = merged
            except Exception:
                lab = {"depression_level": 0, "anxiety_level": 0, "risk_flag": "none", "student_distress_score": 0}
                src = items[idx]
                merged = dict(src)
                merged.update(lab)
                results[idx] = merged
            bar.update(1)
        bar.close()
    write_jsonl(output_path, results)
    total = len(items) if len(items) > 0 else 1
    rate = success / total
    print(f"{Path(input_path).name} 命中率: {rate:.2%}")
    end_t = time.time()
    if log_path:
        entry = {
            "file": Path(input_path).name,
            "output": str(output_path),
            "total": total,
            "success": success,
            "hit_rate": rate,
            "duration_sec": end_t - start_t,
            "workers": workers,
            "base_url": base_url,
            "model": model,
            "start_ts": start_t,
            "end_ts": end_t,
        }
        append_log(log_path, entry)

def part_index_from_name(name):
    m = re.search(r"input_part_(\d+)\.jsonl", name)
    if not m:
        return None
    try:
        return int(m.group(1))
    except Exception:
        return None

def collect_parts(data_dir, sample, part, start=None, end=None):
    p = Path(data_dir)
    if sample:
        sp = p / "sample.jsonl"
        return [sp] if sp.exists() else []
    if part:
        target = p / part
        return [target] if target.exists() else []
    parts = sorted([x for x in p.glob("input_part_*.jsonl")])
    if start is not None or end is not None:
        filtered = []
        for x in parts:
            idx = part_index_from_name(x.name)
            if idx is None:
                continue
            if start is not None and idx < start:
                continue
            if end is not None and idx > end:
                continue
            filtered.append(x)
        parts = filtered
    return parts

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data-dir", default=str(Path(__file__).parent / "data"))
    parser.add_argument("--results-dir", default=str(Path(__file__).parent / "results"))
    parser.add_argument("--log-file", default=str(Path(__file__).parent / "results" / "labeling.log"))
    parser.add_argument("--prompt", default=str(Path(__file__).parent / "prompts" / "min_instruction.txt"))
    parser.add_argument("--base-url", default=os.getenv("VE_BASE_URL", "https://api.vectorengine.ai/v1/chat/completions"))
    parser.add_argument("--api-key", default=os.getenv("VE_API_KEY", ""))
    parser.add_argument("--model", default=os.getenv("VE_MODEL", "gemini-2.5-flash"))
    parser.add_argument("--max-rounds", type=int, default=12)
    parser.add_argument("--max-chars", type=int, default=2000)
    parser.add_argument("--workers", type=int, default=50)
    parser.add_argument("--sample", action="store_true")
    parser.add_argument("--part", help="只处理指定分片文件名，如 input_part_0001.jsonl")
    parser.add_argument("--start", type=int)
    parser.add_argument("--end", type=int)
    parser.add_argument("--mock", action="store_true")
    args = parser.parse_args()
    if not args.api_key and not args.mock:
        raise SystemExit("缺少 API 密钥，请在环境变量 VE_API_KEY 中设置或使用 --api-key；或使用 --mock 进行离线示例")
    instruction = read_instruction(args.prompt)
    parts = collect_parts(args.data_dir, args.sample, args.part, args.start, args.end)
    for part in parts:
        out = Path(args.results_dir) / (part.stem + ".jsonl")
        process_file(str(part), str(out), instruction, args.base_url, args.api_key, args.model, args.max_rounds, args.max_chars, args.workers, args.mock, args.log_file)

if __name__ == "__main__":
    main()