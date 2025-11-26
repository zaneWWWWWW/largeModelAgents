import os
import argparse

def split_jsonl(src_path, out_dir, chunk_size=300, prefix="input_part_"):
    os.makedirs(out_dir, exist_ok=True)
    idx = 0
    part = 1
    writer = None
    with open(src_path, 'r', encoding='utf-8') as f:
        for line in f:
            if not line.strip():
                continue
            if idx % chunk_size == 0:
                if writer:
                    writer.close()
                out_path = os.path.join(out_dir, f"{prefix}{part:04d}.jsonl")
                writer = open(out_path, 'w', encoding='utf-8')
                part += 1
            writer.write(line)
            idx += 1
    if writer:
        writer.close()

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--src')
    ap.add_argument('--out')
    ap.add_argument('--chunk', type=int, default=300)
    ap.add_argument('--input')
    ap.add_argument('--outdir')
    args = ap.parse_args()
    src = args.src or args.input
    out = args.out or args.outdir
    if not src or not out:
        raise SystemExit("must provide --src/--out or --input/--outdir")
    split_jsonl(src, out, args.chunk)

if __name__ == '__main__':
    main()