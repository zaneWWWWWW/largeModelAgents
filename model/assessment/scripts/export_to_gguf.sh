#!/bin/bash
set -euo pipefail
MERGED_DIR=${1:-/share/home/gpu093197/zanewang/project-llm/models/r1-qwen1p5b-merged}
OUT_DIR=${2:-/share/home/gpu093197/zanewang/project-llm/models/gguf}
QUANT=${3:-Q4_K_M}
LLAMACPP_ROOT=${LLAMACPP_ROOT:-$HOME/llama.cpp}
LLAMACPP_BIN=${LLAMACPP_BIN:-$HOME/llama.cpp/build/bin}
mkdir -p "$OUT_DIR"
python "$LLAMACPP_ROOT/convert-hf-to-gguf.py" --model "$MERGED_DIR" --outfile "$OUT_DIR/model-f16.gguf"
"$LLAMACPP_BIN/quantize" "$OUT_DIR/model-f16.gguf" "$OUT_DIR/model-$QUANT.gguf" "$QUANT"