import subprocess
import json

def completions(prompt, model="llama3.1:8b"):
    cmd = ["ollama", "run", model]
    p = subprocess.run(cmd, input=prompt.encode("utf-8"), capture_output=True)
    out = p.stdout.decode("utf-8").strip()
    try:
        json.loads(out)
        return out
    except Exception:
        return "{}"