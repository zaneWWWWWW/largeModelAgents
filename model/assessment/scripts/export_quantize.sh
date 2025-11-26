#!/bin/bash
#SBATCH --job-name=export_quantize
#SBATCH --partition=A40
#SBATCH --gres=gpu:1
#SBATCH --cpus-per-task=8
#SBATCH --mem=32G
#SBATCH --time=02:00:00
#SBATCH --output=llf_export_quantize_%j.out
#SBATCH --error=llf_export_quantize_%j.err

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

echo "=========================================="
echo "开始导出和量化最优LoRA模型"
echo "=========================================="
echo "时间: $(date)"
echo "节点: $(hostname)"
echo ""

# 步骤1: 合并最优LoRA模型到基础模型
echo "=========================================="
echo "步骤1: 合并MindChat-Qwen2-0.5B LoRA模型"
echo "=========================================="

$PYTHON -m llamafactory.cli export llamafactory/configs/merge_mindchat_qwen2_05b.yaml || echo "导出命令返回非零，但继续尝试修复并继续"

if [ ! -d "models/mindchat-qwen2-05b-merged" ]; then
    echo "错误: 合并失败，目录不存在"
    exit 1
fi

# 如果tokenizer文件为空或缺失，则从基础模型复制
BASE_MODEL_PATH=$(grep -E '^model_name_or_path:' llamafactory/configs/merge_mindchat_qwen2_05b.yaml | awk '{print $2}')
MERGED_DIR="models/mindchat-qwen2-05b-merged"
for f in tokenizer.json tokenizer_config.json merges.txt vocab.json chat_template.jinja added_tokens.json special_tokens_map.json; do
    if [ ! -s "$MERGED_DIR/$f" ]; then
        echo "修复: 复制缺失的$f"
        cp -f "$BASE_MODEL_PATH/$f" "$MERGED_DIR/" 2>/dev/null || true
    fi
done

# 列出合并目录
echo "✓ 模型合并完成(含tokenizer修复)"
ls -lh models/mindchat-qwen2-05b-merged/

# 步骤2: 转换为GGUF格式
echo ""
echo "=========================================="
echo "步骤2: 转换为GGUF格式"
echo "=========================================="

MERGED_MODEL="models/mindchat-qwen2-05b-merged"
GGUF_DIR="models/mindchat-qwen2-05b-merged-gguf"

mkdir -p "$GGUF_DIR"

# 检查llama.cpp是否存在
if [ ! -d "llama.cpp" ]; then
    echo "克隆llama.cpp..."
    git clone https://github.com/ggerganov/llama.cpp.git
    cd llama.cpp
    make -j$(nproc)
    cd ..
else
    cd llama.cpp
    if [ ! -f "quantize" ]; then
        echo "编译llama.cpp..."
        make -j$(nproc)
    fi
    cd ..
fi

# 转换为GGUF
echo "转换模型为GGUF格式..."
$PYTHON llama.cpp/convert.py "$MERGED_MODEL" \
    --outfile "$GGUF_DIR/model.gguf" \
    --outtype f16

if [ ! -f "$GGUF_DIR/model.gguf" ]; then
    echo "错误: GGUF转换失败"
    exit 1
fi

echo "✓ GGUF转换完成"
ls -lh "$GGUF_DIR/model.gguf"

# 步骤3: 量化为Q4_KM
echo ""
echo "=========================================="
echo "步骤3: 量化为Q4_KM格式"
echo "=========================================="

QUANTIZED_MODEL="$GGUF_DIR/model-q4_km.gguf"

echo "量化模型为Q4_KM..."
./llama.cpp/quantize "$GGUF_DIR/model.gguf" "$QUANTIZED_MODEL" Q4_K_M

if [ ! -f "$QUANTIZED_MODEL" ]; then
    echo "错误: 量化失败"
    exit 1
fi

echo "✓ 量化完成"
ls -lh "$QUANTIZED_MODEL"

# 步骤4: 生成文件大小对比
echo ""
echo "=========================================="
echo "文件大小对比"
echo "=========================================="

echo "原始合并模型:"
du -sh "$MERGED_MODEL"

echo "GGUF F16模型:"
du -sh "$GGUF_DIR/model.gguf"

echo "量化Q4_KM模型:"
du -sh "$QUANTIZED_MODEL"

# 计算压缩率
ORIGINAL_SIZE=$(du -sb "$MERGED_MODEL" | cut -f1)
QUANTIZED_SIZE=$(du -sb "$QUANTIZED_MODEL" | cut -f1)
COMPRESSION_RATIO=$(echo "scale=2; $ORIGINAL_SIZE / $QUANTIZED_SIZE" | bc)

echo ""
echo "压缩率: ${COMPRESSION_RATIO}x"

echo ""
echo "=========================================="
echo "导出和量化完成！"
echo "=========================================="
echo "最优模型: MindChat-Qwen2-0.5B LoRA"
echo "评估损失: 0.0733"
echo "量化模型: $QUANTIZED_MODEL"
echo "时间: $(date)"

