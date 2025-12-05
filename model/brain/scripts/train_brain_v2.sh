#!/bin/bash
# Brain LLM 训练脚本 V2 - 带保存验证
set -e

cd /home/zanewang/projects/fine-tuning/LLaMA-Factory

OUTPUT_DIR="../saves/Qwen2.5-0.5B-Brain/lora/sft"

echo "=========================================="
echo "🧠 Brain LLM 训练 V2 (带保存验证)"
echo "=========================================="
echo "输出目录: $OUTPUT_DIR"
echo ""

# 1. 清理旧的输出（如果存在）
if [ -d "$OUTPUT_DIR" ]; then
    echo "⚠️  发现旧的训练结果，创建备份..."
    BACKUP_DIR="../saves/Qwen2.5-0.5B-Brain_backup_$(date +%Y%m%d_%H%M%S)"
    mv "../saves/Qwen2.5-0.5B-Brain" "$BACKUP_DIR"
    echo "✅ 备份至: $BACKUP_DIR"
fi

# 2. 创建输出目录
echo "📁 创建输出目录..."
mkdir -p "$OUTPUT_DIR"
echo "✅ 目录创建成功"

# 3. 开始训练
echo ""
echo "🚀 开始训练..."
echo ""

llamafactory-cli train ../brain_train_config.yaml

# 4. 验证保存结果
echo ""
echo "=========================================="
echo "🔍 验证保存结果..."
echo "=========================================="

if [ ! -d "$OUTPUT_DIR" ]; then
    echo "❌ 错误: 输出目录不存在!"
    exit 1
fi

# 检查关键文件
REQUIRED_FILES=(
    "adapter_model.safetensors"
    "adapter_config.json"
    "all_results.json"
)

MISSING_FILES=()
for file in "${REQUIRED_FILES[@]}"; do
    if [ ! -f "$OUTPUT_DIR/$file" ]; then
        MISSING_FILES+=("$file")
    fi
done

if [ ${#MISSING_FILES[@]} -gt 0 ]; then
    echo "❌ 错误: 以下文件缺失:"
    for file in "${MISSING_FILES[@]}"; do
        echo "  - $file"
    done
    exit 1
fi

# 5. 显示结果
echo "✅ 所有必需文件已保存"
echo ""
echo "📊 训练结果:"
cat "$OUTPUT_DIR/all_results.json" | python3 -m json.tool
echo ""
echo "📁 模型文件大小:"
ls -lh "$OUTPUT_DIR"/*.safetensors

echo ""
echo "=========================================="
echo "✅ Brain LLM 训练完成并验证成功！"
echo "=========================================="
echo "模型位置: $OUTPUT_DIR"



