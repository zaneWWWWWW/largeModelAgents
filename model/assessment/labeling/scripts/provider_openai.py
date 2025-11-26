import os
from openai import OpenAI

def get_client():
    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY not set")
    return OpenAI(api_key=api_key)

def completions(prompt, model="gpt-4o-mini"):
    client = get_client()
    resp = client.chat.completions.create(
        model=model,
        messages=[{"role":"user","content":prompt}],
        temperature=0,
        response_format={"type":"json_object"}
    )
    return resp.choices[0].message.content