import os
import json
import requests

API_URL = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"

def completions(prompt, model="gemini-2.5-flash"):
    api_key = os.environ.get("GEMINI_API_KEY")
    if not api_key:
        raise RuntimeError("GEMINI_API_KEY not set")
    url = API_URL.format(model=model, api_key=api_key)
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {"temperature": 0}
    }
    r = requests.post(url, json=payload, timeout=60)
    r.raise_for_status()
    data = r.json()
    try:
        txt = data["candidates"][0]["content"]["parts"][0]["text"]
    except Exception:
        txt = "{}"
    return txt