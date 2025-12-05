# Brain LLM V2 训练指南

> **新架构**: local_chat 始终调用 + 多工具并行决策

## 🎯 架构变化

### V1 (旧架构)
- 单工具路由：每次只选择一个工具
- 问题：直接调用 `psychological_assessment` 造成对话割裂感

### V2 (新架构) ✅
- `local_chat` **必须调用**：每次都生成对话响应
- 多工具并行：根据需要同时调用其他工具
- 优势：保证对话连贯性，同时进行专业评估

## 📊 训练数据

### 数据分布
- **总计**: 250 条样本
- **仅 local_chat**: 201 条 (80.4%)
- **local_chat + assessment**: 39 条 (15.6%)
- **local_chat + memory_query**: 10 条 (4.0%)

### 数据来源
1. **核心样本** (50条): 手工设计的高质量样本，覆盖所有场景
2. **心理咨询数据** (80条): 从真实心理咨询对话中提取
3. **Alpaca中文数据** (120条): 通用对话场景

### 输出格式

```json
{
  "tools": [
    {"name": "local_chat", "parameters": {"user_input": "用户输入"}},
    {"name": "psychological_assessment", "parameters": {"trigger_reason": "触发原因"}}
  ]
}
```

## 🚀 训练流程

### 1. 开始训练

```bash
cd /home/zanewang/projects/fine-tuning
./train_brain.sh
```

**预计时间**: 2-3小时（取决于GPU）
**显存需求**: 约 6-8GB

### 2. 训练配置

- **基座模型**: Qwen2.5-0.5B-Instruct
- **微调方法**: LoRA (rank=16, alpha=32)
- **学习率**: 3e-4
- **训练轮数**: 5 epochs
- **批次大小**: 4 (梯度累积 x4)
- **验证集**: 10%

### 3. 测试模型

```bash
python test_brain_model.py
```

**测试内容**:
- 14个测试用例
- 覆盖三种场景（纯聊天、评估、历史查询）
- 输出准确率统计

### 4. 合并 LoRA 权重

```bash
python merge_brain_lora.py
```

**输出**: `output/qwen2.5-0.5b-brain-merged/`

### 5. 转换为 GGUF

```bash
./convert_brain_to_gguf.sh
```

**输出文件**:
- `brain-fp16.gguf` (约 1GB)
- `brain-q4_k_m.gguf` (约 350MB, **推荐用于 Android**)
- `brain-q8_0.gguf` (约 530MB, 高质量)

## 📱 Android 集成

### 核心逻辑

```java
// 1. Brain LLM 决策
String brainOutput = brainModel.generate(userInput);
JSONObject decision = parseDecision(brainOutput);
JSONArray tools = decision.getJSONArray("tools");

// 2. 执行所有工具
String chatResponse = null;
AssessmentResult assessmentResult = null;
List<Memory> memories = null;

for (int i = 0; i < tools.length(); i++) {
    JSONObject tool = tools.getJSONObject(i);
    String toolName = tool.getString("name");
    JSONObject params = tool.getJSONObject("parameters");
    
    switch (toolName) {
        case "local_chat":
            // 调用大模型生成对话
            chatResponse = chatModel.generate(params.getString("user_input"));
            break;
        case "psychological_assessment":
            // 调用评估工具
            assessmentResult = assessmentTool.assess(params.getString("trigger_reason"));
            break;
        case "memory_query":
            // 查询历史记录
            memories = memoryDB.query(params.getString("query"));
            break;
    }
}

// 3. 整合结果
String finalResponse = integrateResults(chatResponse, assessmentResult, memories);
return finalResponse;
```

### 结果整合策略

1. **仅 local_chat**: 直接返回对话响应
2. **local_chat + assessment**: 
   - 先展示聊天内容（表示关心）
   - 然后展示评估结果和建议
3. **local_chat + memory**: 
   - 将历史记录注入到聊天上下文
   - 返回带有历史信息的回复

## 🔍 评估指标

### 训练指标
- **Train Loss**: 应降至 0.05-0.1
- **Eval Loss**: 应接近 train loss
- **格式正确率**: 应 >95%

### 测试指标
- **工具选择准确率**: >90%
- **JSON 解析成功率**: 100%
- **推理速度**: <100ms (Q4量化)

## 💡 调优建议

### 如果训练loss不降
- 增加 learning_rate 到 5e-4
- 增加 epoch 到 8-10
- 检查数据格式是否正确

### 如果测试准确率低
- 增加训练数据（特别是边界case）
- 调整 lora_rank 到 32
- 使用全量微调而非 LoRA

### 如果推理速度慢
- 使用 Q4_K_M 量化
- 减少 max_new_tokens 到 100
- 考虑使用 Qwen2-0.5B（更小）

## 📦 文件结构

```
fine-tuning/
├── brain_train_config.yaml           # 训练配置
├── train_brain.sh                    # 训练脚本
├── test_brain_model.py               # 测试脚本
├── merge_brain_lora.py               # 合并脚本
├── convert_brain_to_gguf.sh          # 量化脚本
├── rebuild_brain_data_v2.py          # 数据生成脚本
├── LLaMA-Factory/
│   └── data/
│       ├── brain_training_data_v2.json   # 训练数据
│       └── dataset_info.json             # 数据集注册
├── saves/
│   └── Qwen2.5-0.5B-Brain/
│       └── lora/sft/                 # LoRA 权重
└── output/
    ├── qwen2.5-0.5b-brain-merged/    # 合并后的模型
    └── brain-gguf/                   # GGUF 文件
```

## 🎓 最佳实践

1. **数据质量 > 数量**: 250条高质量数据足够
2. **格式一致性**: 确保训练和推理时 system prompt 一致
3. **温度控制**: 推理时使用低温度(0.1-0.3)保证格式稳定
4. **错误处理**: 始终验证 JSON 解析结果
5. **优雅降级**: JSON 解析失败时，默认调用 local_chat

## 📞 故障排除

### Q: 训练时显存不足
A: 减小 `per_device_train_batch_size` 到 2，增加 `gradient_accumulation_steps` 到 8

### Q: 模型总是输出格式错误
A: 增加训练轮数，或在数据中添加更多格式示例

### Q: psychological_assessment 触发过于频繁/不足
A: 调整训练数据中评估样本的比例，或修改 system prompt 中的触发条件描述

### Q: Android 上推理太慢
A: 使用 Q4_K_M 量化，或考虑使用更小的基座模型（如 Qwen2-0.25B）

---

**版本**: V2.0  
**更新日期**: 2025-12-02  
**作者**: AI Assistant

