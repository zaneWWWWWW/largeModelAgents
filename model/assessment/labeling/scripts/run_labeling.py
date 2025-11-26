import os
import argparse
import json
from tqdm import tqdm
from .common import jsonl_read, jsonl_write, clip_conversation, build_prompt, validate_labels

def load_instruction(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def get_provider():
    prov = os.environ.get('PROVIDER', 'ollama').lower()
    if prov == 'openai':
        from .provider_openai import completions
        model = os.environ.get('MODEL', 'gpt-4o-mini')
        return lambda prompt: completions(prompt, model)
    if prov == 'gemini':
        from .provider_gemini import completions
        model = os.environ.get('MODEL', 'gemini-2.5-flash')
        return lambda prompt: completions(prompt, model)
    else:
        from .provider_ollama import completions
        model = os.environ.get('MODEL', 'llama3.1:8b')
        return lambda prompt: completions(prompt, model)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--input', required=True)
    ap.add_argument('--output', required=True)
    ap.add_argument('--instruction', default=os.path.join(os.path.dirname(__file__), '..', 'prompts', 'min_instruction.txt'))
    ap.add_argument('--max_chars', type=int, default=2000)
    ap.add_argument('--max_turns', type=int, default=12)
    args = ap.parse_args()

    instruction = load_instruction(args.instruction)
    provider = get_provider()
    results = []

    for item in tqdm(jsonl_read(args.input)):
        convo = item.get('conversation', [])
        convo = clip_conversation(convo, args.max_chars, args.max_turns)
        prompt = build_prompt(instruction, convo)
        raw = provider(prompt)
        try:
            obj = json.loads(raw)
            validate_labels(obj)
        except Exception:
            continue
        results.append({'id': item.get('id'), 'labels': obj})

    os.makedirs(os.path.dirname(args.output), exist_ok=True)
    jsonl_write(args.output, results)

if __name__ == '__main__':
    main()