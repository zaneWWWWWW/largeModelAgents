#!/bin/bash
# Brain LLM 训练脚本
# 用于训练 Agent 框架的决策大脑

set -e

# cd /home/zanewang/projects/fine-tuning/LLaMA-Factory

echo "=========================================="
echo "🧠 开始训练 Brain LLM (工具路由决策模型)"
echo "=========================================="
echo "基座模型: Qwen2.5-0.5B-Instruct"
echo "训练数据: brain_training_data (50 samples)"
echo "训练目标: 学习三个工具的调用决策"
echo "  - local_chat: 日常对话"
echo "  - psychological_assessment: 心理评估"
echo "  - memory_query: 历史查询"
echo "=========================================="

# 使用 llamafactory-cli 进行训练
cd /home/zanewang/projects/fine-tuning/LLaMA-Factory
llamafactory-cli train ../scripts/brain_v2/brain_train_config.yaml

echo "=========================================="
echo "✅ Brain LLM 训练完成！"
echo "模型保存在: saves/Qwen2.5-0.5B-Brain/lora/sft"
echo "=========================================="

