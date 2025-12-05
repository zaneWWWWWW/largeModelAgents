#!/bin/bash
# Brain LLM 量化脚本
# 将合并后的模型转换为 GGUF 格式

set -e

INPUT_DIR="output/qwen2.5-0.5b-brain-merged"
OUTPUT_DIR="output/brain-gguf"
LLAMA_CPP_DIR="llama.cpp"

echo "=========================================="
echo "🔧 将 Brain LLM 转换为 GGUF 格式"
echo "=========================================="

# 创建输出目录
mkdir -p $OUTPUT_DIR

# 检查 llama.cpp 目录
if [ ! -d "$LLAMA_CPP_DIR" ]; then
    echo "正在解压 llama.cpp..."
    if [ -f "../psychological-assessment/llama.cpp.zip" ]; then
        unzip -q ../psychological-assessment/llama.cpp.zip -d .
    elif [ -f "llama.cpp.zip" ]; then
        unzip -q llama.cpp.zip -d .
    else
        echo "错误: 找不到 llama.cpp.zip 文件"
        exit 1
    fi
fi

cd $LLAMA_CPP_DIR

# 转换为 GGUF 格式 (FP16)
echo ""
echo "1. 转换为 GGUF (FP16)..."
python convert_hf_to_gguf.py ../$INPUT_DIR --outfile ../$OUTPUT_DIR/brain-fp16.gguf --outtype f16

# 量化为 Q4_K_M (推荐的平衡量化)
echo ""
echo "2. 量化为 Q4_K_M..."
./build/bin/llama-quantize ../$OUTPUT_DIR/brain-fp16.gguf ../$OUTPUT_DIR/brain-q4_k_m.gguf Q4_K_M

# 量化为 Q8_0 (更高质量)
echo ""
echo "3. 量化为 Q8_0..."
./build/bin/llama-quantize ../$OUTPUT_DIR/brain-fp16.gguf ../$OUTPUT_DIR/brain-q8_0.gguf Q8_0

cd ..

echo ""
echo "=========================================="
echo "✅ GGUF 转换完成!"
echo "=========================================="
echo "输出文件:"
echo "  - $OUTPUT_DIR/brain-fp16.gguf    (FP16, 约 1GB)"
echo "  - $OUTPUT_DIR/brain-q4_k_m.gguf  (Q4_K_M, 约 350MB, 推荐)"
echo "  - $OUTPUT_DIR/brain-q8_0.gguf    (Q8_0, 约 530MB)"
echo ""
echo "🤖 Android 部署推荐使用 brain-q4_k_m.gguf"
echo "=========================================="

