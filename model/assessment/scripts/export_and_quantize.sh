#!/bin/bash
# 导出最优LoRA模型并量化为GGUF Q4_KM格式

set -e

PROJECT_DIR="/share/home/gpu093197/zanewang/project-llm"
cd "$PROJECT_DIR"

# 激活环境
source /share/home/gpu093197/anaconda3/etc/profile.d/conda.sh
conda activate llama-zanewang

export PYTHON=/share/home/gpu093197/anaconda3/envs/llama-zanewang/bin/python
export LLF_PY=$PROJECT_DIR/LLaMA-Factory-main
export PYTHONPATH="$LLF_PY/src:${PYTHONPATH:-}"
export HF_HOME=$PROJECT_DIR/.cache/huggingface

# 步骤1: 合并最优LoRA模型到基础模型
echo "=========================================="
echo "步骤1: 合并MindChat-Qwen2-0.5B LoRA模型"
echo "=========================================="

$PYTHON -m llamafactory.cli export llamafactory/configs/merge_mindchat_qwen2_05b.yaml

if [ ! -d "models/mindchat-qwen2-05b-merged" ]; then
    echo "错误: 合并失败，目录不存在"
    exit 1
fi

echo "✓ 模型合并完成: models/mindchat-qwen2-05b-merged"

# 步骤2: 转换为GGUF格式
echo ""
echo "=========================================="
echo "步骤2: 转换为GGUF格式"
echo "=========================================="

MERGED_MODEL="models/mindchat-qwen2-05b-merged"
GGUF_MODEL="models/mindchat-qwen2-05b-merged-gguf"

# 使用llama.cpp的转换脚本
if [ ! -d "$GGUF_MODEL" ]; then
    mkdir -p "$GGUF_MODEL"
fi

# 检查llama.cpp是否存在，如果不存在则克隆
if [ ! -d "llama.cpp" ]; then
    echo "克隆llama.cpp..."
    git clone https://github.com/ggerganov/llama.cpp.git
fi

cd llama.cpp

# 构建llama.cpp（如果需要）
if [ ! -f "convert.py" ]; then
    echo "错误: llama.cpp convert.py不存在"
    exit 1
fi

# 转换为GGUF
echo "转换模型为GGUF格式..."
$PYTHON convert.py "$PROJECT_DIR/$MERGED_MODEL" --outfile "$PROJECT_DIR/$GGUF_MODEL/model.gguf" --outtype f16

if [ ! -f "$PROJECT_DIR/$GGUF_MODEL/model.gguf" ]; then
    echo "错误: GGUF转换失败"
    exit 1
fi

echo "✓ GGUF转换完成: $GGUF_MODEL/model.gguf"

# 步骤3: 量化为Q4_KM
echo ""
echo "=========================================="
echo "步骤3: 量化为Q4_KM格式"
echo "=========================================="

QUANTIZED_MODEL="$PROJECT_DIR/$GGUF_MODEL/model-q4_km.gguf"

# 使用quantize工具
if [ ! -f "quantize" ]; then
    echo "编译quantize工具..."
    make quantize
fi

echo "量化模型为Q4_KM..."
./quantize "$PROJECT_DIR/$GGUF_MODEL/model.gguf" "$QUANTIZED_MODEL" Q4_K_M

if [ ! -f "$QUANTIZED_MODEL" ]; then
    echo "错误: 量化失败"
    exit 1
fi

echo "✓ 量化完成: $QUANTIZED_MODEL"

# 显示文件大小
echo ""
echo "=========================================="
echo "文件大小对比"
echo "=========================================="
ls -lh "$PROJECT_DIR/$MERGED_MODEL/model.safetensors" 2>/dev/null || echo "合并模型: 未找到"
ls -lh "$PROJECT_DIR/$GGUF_MODEL/model.gguf"
ls -lh "$QUANTIZED_MODEL"

cd "$PROJECT_DIR"

echo ""
echo "=========================================="
echo "导出和量化完成！"
echo "=========================================="
echo "最优模型: MindChat-Qwen2-0.5B LoRA"
echo "评估损失: 0.0733"
echo "量化模型: $QUANTIZED_MODEL"

