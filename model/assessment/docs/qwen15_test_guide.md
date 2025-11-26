# Qwen1.5-0.5B LoRA模型测试指南

## 当前状态

### 模型信息
- **模型**: Qwen1.5-0.5B-Chat LoRA
- **评估损失**: 0.073341（非常接近最优的0.073324）
- **LoRA权重**: ✅ 已存在 (`saves/qwen15-05b/lora/sft/`)
- **合并模型**: ❌ 需要合并
- **量化模型**: ❌ 需要量化

## 遇到的问题

### torchvision兼容性问题
当前环境存在torchvision兼容性问题，导致无法直接使用transformers和peft库合并模型。

错误信息：
```
RuntimeError: operator torchvision::nms does not exist
ModuleNotFoundError: Could not import module 'PreTrainedModel'
```

## 解决方案

### 方案1: 修复torchvision环境（推荐）

```bash
# 激活环境
mamba activate project-llm

# 重新安装torchvision（与PyTorch版本匹配）
mamba install torchvision -c pytorch -y

# 或者使用pip重新安装
pip uninstall torchvision -y
pip install torchvision
```

### 方案2: 使用conda环境重新安装

```bash
# 创建新环境
mamba create -n project-llm-fixed python=3.10 -y
mamba activate project-llm-fixed

# 安装依赖
mamba install pytorch torchvision -c pytorch -y
pip install transformers peft accelerate
```

### 方案3: 在Linux服务器上合并（如果可用）

如果Mac环境问题难以解决，可以在Linux服务器上运行合并：

```bash
# 在Linux服务器上
cd /path/to/project-llm
python scripts/merge_lora_simple.py
```

然后将合并后的模型复制到Mac：
```bash
# 复制合并模型
scp -r server:/path/to/project-llm/models/qwen15-05b-merged ./models/
```

## 合并模型步骤

一旦环境问题解决，运行：

```bash
cd /Users/zanewang/course-combinatorics/project/project-llm
mamba activate project-llm
python scripts/merge_lora_simple.py
```

这将：
1. 自动下载Qwen1.5-0.5B-Chat模型（如果未下载）
2. 合并LoRA权重
3. 保存到 `models/qwen15-05b-merged/`

## 量化模型步骤

合并完成后，进行量化：

```bash
# 1. 转换为GGUF格式
python llama.cpp/convert.py models/qwen15-05b-merged \
    --outfile models/qwen15-05b-merged-gguf/model.gguf \
    --outtype f16

# 2. 量化为Q4_K_M
./llama.cpp/build/bin/llama-quantize \
    models/qwen15-05b-merged-gguf/model.gguf \
    models/qwen15-05b-merged-gguf/model-q4_km.gguf \
    Q4_K_M
```

## 测试模型

量化完成后，使用测试脚本：

```bash
python scripts/test_mental_assessment.py \
    --gguf-model models/qwen15-05b-merged-gguf/model-q4_km.gguf \
    --gguf-only \
    --output results/qwen15_mental_assessment_test.json
```

## 预期结果

基于训练结果，Qwen1.5-0.5B LoRA模型应该：
- **评估损失**: 0.073341（略高于MindChat的0.073324）
- **JSON输出**: 应该能正常输出，但可能需要后处理
- **性能**: 与MindChat模型相近

## 对比测试

完成测试后，可以对比两个模型：

| 模型 | 评估损失 | JSON输出率 | 平均推理时间 |
|------|---------|-----------|------------|
| MindChat-Qwen2-0.5B | 0.073324 | 100% (使用后处理) | ~0.13s |
| Qwen1.5-0.5B | 0.073341 | 待测试 | 待测试 |

---
**状态**: 等待环境问题解决
**最后更新**: 2025-11-26

