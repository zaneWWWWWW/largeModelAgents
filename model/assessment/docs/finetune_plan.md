目标
- 使用 `sft_student_mental.jsonl` 进行四标签的监督式指令微调（SFT），生成可在移动端部署的量化模型。

数据准备
- 生成训练集：`python scripts/prepare_sft_dataset.py`
- 输出：`data/datasets/sft_student_mental.jsonl`

训练方案（LoRA + 混合精度 + 断点保存）
- 模型基座：`InternLM2-7B-Chat` 或其他已部署友好模型
- LoRA 目标：`q_proj,v_proj`（或基座特定 `wqkv`）
- 重要超参：
  - `per_device_train_batch_size`: 1–4（依据显存）
  - `gradient_accumulation_steps`: 16–64（维持等效大批次）
  - `learning_rate`: 2e-5（LoRA）；`1e-6`（全参建议极小）
  - `num_train_epochs`: 3–5（先小步试跑）
  - `save_steps`: 每 N 步保存检查点
  - `fp16`/`bf16`: 混合精度开启

资源申请（SLURM 示例）
- A40 分区：`--gres=gpu:1 --cpus-per-task=8 --mem=32G --time=2-00:00:00`
- 监控：`echo $CUDA_VISIBLE_DEVICES`、`nvidia-smi`、`squeue`

日志与校验
- 训练日志：`output/<uuid>/logs.txt`
- 指标监控：loss、输出可解析率（JSON 结构）、校准分布（标签占比）
- 断点恢复：从 `output/<uuid>/checkpoint-*` 继续训练

后处理与导出
- 合并 LoRA 权重、保存最终权重：`model.save_pretrained()`
- 量化导出：根据移动端需求选择 4/8-bit 量化