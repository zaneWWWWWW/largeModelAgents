#!/bin/bash

set -e

echo "========================================="
echo "LoRA 合并 + GGUF 转换 + 量化流程"
echo "========================================="

WORK_DIR="/home/zanewang/projects/fine-tuning"
MERGED_MODEL_DIR="${WORK_DIR}/output/qwen2.5-0.5b-merged"
GGUF_OUTPUT_DIR="${WORK_DIR}/output/gguf"
LLAMA_CPP_DIR="${WORK_DIR}/llama.cpp"

cd $WORK_DIR

# 步骤 1: 合并 LoRA 权重
echo ""
echo "步骤 1/4: 合并 LoRA 权重到基座模型..."
python3 merge_lora.py

# 步骤 2: 克隆或更新 llama.cpp (如果还没有)
if [ ! -d "$LLAMA_CPP_DIR" ]; then
    echo ""
    echo "步骤 2/4: 克隆 llama.cpp 仓库..."
    git clone https://github.com/ggerganov/llama.cpp.git
    cd llama.cpp
    echo "编译 llama.cpp..."
    make
    cd ..
else
    echo ""
    echo "步骤 2/4: llama.cpp 已存在，跳过克隆"
fi

# 步骤 3: 转换为 GGUF 格式
echo ""
echo "步骤 3/4: 转换为 GGUF 格式..."
mkdir -p $GGUF_OUTPUT_DIR
python3 ${LLAMA_CPP_DIR}/convert_hf_to_gguf.py \
    $MERGED_MODEL_DIR \
    --outfile ${GGUF_OUTPUT_DIR}/model-f16.gguf \
    --outtype f16

# 步骤 4: 量化为 Q4_K_M
echo ""
echo "步骤 4/4: 量化为 Q4_K_M 格式..."
${LLAMA_CPP_DIR}/llama-quantize \
    ${GGUF_OUTPUT_DIR}/model-f16.gguf \
    ${GGUF_OUTPUT_DIR}/model-q4_k_m.gguf \
    Q4_K_M

echo ""
echo "========================================="
echo "✅ 全部完成！"
echo "========================================="
echo ""
echo "📍 合并后的模型: $MERGED_MODEL_DIR"
echo "📍 FP16 GGUF 模型: ${GGUF_OUTPUT_DIR}/model-f16.gguf"
echo "📍 Q4_K_M 量化模型: ${GGUF_OUTPUT_DIR}/model-q4_k_m.gguf"
echo ""
echo "💡 Q4_K_M 量化模型可以在手机上高效运行！"
echo ""



