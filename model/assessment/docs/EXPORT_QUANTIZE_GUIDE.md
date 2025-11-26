# LoRA模型导出与量化指南

## 概述

本文档记录了三个LoRA微调模型的性能分析、最优模型选择、导出和量化的完整流程。

## 1. 模型性能分析

### 三个微调模型对比

| 模型 | 基础模型 | 评估损失 | 评估样本/秒 | 训练损失 | 状态 |
|------|---------|---------|-----------|---------|------|
| MindChat-Qwen2-0.5B | MindChat-Qwen2-0.5B | **0.073324** | 26.88 | 0.084587 | ✅ 最优 |
| Qwen1.5-0.5B | Qwen1.5-0.5B | 0.073341 | 27.14 | 0.083435 | 次优 |
| Qwen2.5-0.5B | Qwen2.5-0.5B | 0.075774 | 25.05 | 0.077423 | 第三 |

### 分析结论

**最优模型: MindChat-Qwen2-0.5B LoRA**

- 评估损失最低: 0.073324
- 相比Qwen1.5-0.5B: 低0.000017 (0.02%)
- 相比Qwen2.5-0.5B: 低0.002450 (3.3%)
- 推理速度: 26.88 样本/秒

### 选择理由

1. **最低的评估损失**: 在三个模型中表现最好
2. **稳定的推理性能**: 评估速度稳定
3. **学生心理健康场景适配**: MindChat模型专门针对心理咨询场景优化
4. **平衡的参数规模**: 0.5B参数适合边缘部署

## 2. 导出流程

### 步骤1: 合并LoRA权重

```bash
python -m llamafactory.cli export llamafactory/configs/merge_mindchat_qwen2_05b.yaml
```

**输出**: `models/mindchat-qwen2-05b-merged/`

包含:
- `model.safetensors` - 合并后的模型权重
- `config.json` - 模型配置
- `tokenizer.json` - 分词器
- 其他配置文件

### 步骤2: 转换为GGUF格式

```bash
python llama.cpp/convert.py models/mindchat-qwen2-05b-merged \
    --outfile models/mindchat-qwen2-05b-merged-gguf/model.gguf \
    --outtype f16
```

**输出**: `models/mindchat-qwen2-05b-merged-gguf/model.gguf`

- 格式: GGUF F16 (16位浮点)
- 大小: 约1GB (原始模型大小)

### 步骤3: 量化为Q4_KM

```bash
./llama.cpp/quantize models/mindchat-qwen2-05b-merged-gguf/model.gguf \
    models/mindchat-qwen2-05b-merged-gguf/model-q4_km.gguf \
    Q4_K_M
```

**输出**: `models/mindchat-qwen2-05b-merged-gguf/model-q4_km.gguf`

- 格式: GGUF Q4_KM (4位量化)
- 大小: 约250-300MB (压缩率 3-4x)
- 精度损失: 最小 (Q4_KM是高质量量化方案)

## 3. 量化方案说明

### Q4_KM (推荐)

- **精度**: 4位量化
- **质量**: 高质量 (K-means聚类)
- **速度**: 快速推理
- **大小**: 最小 (~250MB)
- **适用场景**: 移动端、边缘设备、实时推理

### 其他量化方案对比

| 方案 | 大小 | 速度 | 精度 | 适用场景 |
|------|------|------|------|---------|
| F32 | ~2GB | 慢 | 最高 | 服务器 |
| F16 | ~1GB | 中 | 高 | 服务器/GPU |
| Q8_0 | ~500MB | 快 | 很高 | 通用 |
| Q5_K_M | ~350MB | 快 | 高 | 通用 |
| **Q4_K_M** | **~250MB** | **很快** | **高** | **移动端** |
| Q4_0 | ~250MB | 很快 | 中 | 轻量级 |

## 4. 导出和量化作业

### 提交作业

```bash
sbatch scripts/export_quantize.sh
```

**作业ID**: 50966

### 监控进度

```bash
# 查看作业状态
squeue -j 50966

# 实时监控
bash scripts/monitor_export.sh 50966

# 查看日志
tail -f llf_export_quantize_50966.out
```

### 预期时间

- 合并LoRA: ~2-3分钟
- 转换GGUF: ~5-10分钟
- 量化Q4_KM: ~10-15分钟
- **总计**: ~20-30分钟

## 5. 推理测试

### 测试合并模型

```bash
python scripts/test_merged_model.py \
    --model models/mindchat-qwen2-05b-merged \
    --output results/merged_model_test.json
```

**测试内容**:
- 5个学生心理健康相关的测试用例
- 推理时间测试
- 生成速度测试 (tokens/s)
- 内存占用监控

### 预期性能

- **推理时间**: 2-5秒/样本 (GPU)
- **生成速度**: 50-100 tokens/秒
- **内存占用**: 1-2GB (GPU)

## 6. 文件清单

### 生成的文件

```
models/
├── mindchat-qwen2-05b-merged/          # 合并后的PyTorch模型
│   ├── model.safetensors
│   ├── config.json
│   ├── tokenizer.json
│   └── ...
└── mindchat-qwen2-05b-merged-gguf/     # GGUF量化模型
    ├── model.gguf                       # F16格式 (~1GB)
    └── model-q4_km.gguf                # Q4_KM量化 (~250MB) ✅

results/
├── model_analysis.json                  # 模型性能分析报告
├── merged_model_test.json               # 推理测试报告
└── EXPORT_QUANTIZE_GUIDE.md            # 本文档
```

## 7. 部署建议

### 服务器部署

```bash
# 使用PyTorch模型
python inference_server.py \
    --model models/mindchat-qwen2-05b-merged \
    --device cuda
```

### 移动端/边缘设备部署

```bash
# 使用量化GGUF模型
./llama-cpp-cli \
    -m models/mindchat-qwen2-05b-merged-gguf/model-q4_km.gguf \
    -n 256 \
    -t 4
```

### 性能优化

1. **批处理**: 使用batch推理提升吞吐量
2. **缓存**: 启用KV缓存加速生成
3. **量化**: 使用Q4_KM量化降低内存占用
4. **并发**: 使用多个工作进程处理并发请求

## 8. 质量保证

### 量化后的精度验证

```bash
# 对比原始模型和量化模型的输出
python scripts/compare_models.py \
    --pytorch models/mindchat-qwen2-05b-merged \
    --gguf models/mindchat-qwen2-05b-merged-gguf/model-q4_km.gguf
```

### 预期结果

- 输出相似度: >95%
- 性能差异: <5%
- 精度损失: 可接受

## 9. 故障排查

### 问题1: GGUF转换失败

**症状**: `convert.py` 找不到或转换错误

**解决**:
```bash
# 确保llama.cpp已克隆
git clone https://github.com/ggerganov/llama.cpp.git

# 检查convert.py
ls llama.cpp/convert.py
```

### 问题2: 量化失败

**症状**: `quantize` 工具不存在

**解决**:
```bash
cd llama.cpp
make quantize
cd ..
```

### 问题3: 内存不足

**症状**: OOM错误

**解决**:
- 增加SLURM内存: `--mem=64G`
- 使用更小的batch size
- 分阶段处理

## 10. 下一步

1. ✅ 分析三个LoRA模型性能
2. ✅ 确定最优模型: MindChat-Qwen2-0.5B
3. [object Object](进行中, Job 50966)
4. ⏳ 推理测试和性能验证
5. ⏳ 集成到应用系统
6. ⏳ 部署到生产环境

## 参考资源

- [LLaMA-Factory文档](https://github.com/hiyouga/LLaMA-Factory)
- [llama.cpp项目](https://github.com/ggerganov/llama.cpp)
- [GGUF格式说明](https://github.com/ggerganov/ggml/blob/master/docs/gguf.md)
- [量化方案对比](https://github.com/ggerganov/llama.cpp/discussions/3684)

---

**最后更新**: 2025-11-25
**作者**: AI Assistant
**状态**: 进行中 (等待Job 50966完成)

