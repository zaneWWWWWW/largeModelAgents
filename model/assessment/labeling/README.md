目的
- 本目录用于在本地环境执行四标签标注：`depression_level`、`anxiety_level`、`risk_flag`、`student_distress_score`
- 任务背景：面向学生群体的心理评估，基于多轮对话输出简洁、可解释的状态标签与一个综合自评分，用于后续微调与产品呈现。

目录结构
- `data/sample.jsonl` 示例数据（请替换为你的全量 `unified_min.jsonl`）
- `prompts/min_instruction.txt` 最小指令与取值规范
- `scripts/run_labeling.py` 运行入口
- `scripts/provider_ollama.py` 本地 Ollama 提供者
- `scripts/provider_openai.py` OpenAI 提供者
- `scripts/common.py` 裁剪、格式化、校验与解析工具
- `requirements.txt` 依赖
- `results/` 输出目录

环境准备
- Python ≥ 3.9
- 依赖安装：`pip install -r requirements.txt`
- 选择提供者：
  - Ollama 本地模型：安装并启动 Ollama；`export PROVIDER=ollama`；`export MODEL=llama3.1:8b`
  - OpenAI 远程模型：`export PROVIDER=openai`；`export MODEL=gpt-4o-mini`；设置 `export OPENAI_API_KEY=...`
  - Gemini 远程模型：`export PROVIDER=gemini`；`export MODEL=gemini-2.5-flash`；设置 `export GEMINI_API_KEY=...`

运行示例
- 替换数据：将 `unified_min.jsonl` 复制到 `data/` 并重命名为 `input.jsonl`
- 执行：
  - `python scripts/run_labeling.py --input data/input.jsonl --output results/labels.jsonl`
- 输出：每行 `{"id": ..., "labels": {depression_level, anxiety_level, risk_flag, student_distress_score}}`

注意
- 只输出 JSON，不含解释文本；键与取值严格遵循指令文件。
- 如使用 OpenAI，注意成本与速率限制；建议先小批量验证再全量运行。
 - 对话将被自动裁剪（默认近 2000 字或 ≤12 轮）以降低 tokens 消耗；如需保留更多上下文可在命令行参数中调整。