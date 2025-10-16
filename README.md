# largeModelAgents

面向学生用户的移动端心理咨询与心理评测 AI Agent 项目。本仓库采用单仓库（monorepo）结构，支持模型训练、后端服务、移动端应用与报告编写的协同开发。

## 项目目标
- 提供隐私友好、可解释的心理咨询对话服务。
- 集成标准化心理测评量表（如 PHQ-9、GAD-7），输出分数与分级。
- 支持手机端部署（可选：端上推理/边缘/云端），兼顾体验与合规。

## 仓库结构（调整后）
```
.gitignore                # 忽略数据、模型权重、构建产物等
CONTRIBUTING.md           # 协作规范与提交流程
CODE_OF_CONDUCT.md        # 行为准则与沟通约定
 
model/
  counseling/             # 心理咨询对话模型
    configs/ training/ evaluation/ inference/ requirements.txt
  assessment/             # 心理评测/分级模型
    configs/ training/ evaluation/ inference/ requirements.txt

develop/
  app/                    # 移动端应用目录与资产（技术栈由开发组决定）
    README.md
    assets/questionnaires/ (PHQ-9 / GAD-7 JSON)
  services/
    api/                  # 后端 API 服务（FastAPI 骨架）
      main.py
      requirements.txt
      Dockerfile
      README.md
      .env.example
  deployment/             # 一键启动与部署脚本
    docker-compose.yml
  scripts/
    setup_venv.ps1        # Windows 虚拟环境与依赖安装脚本
  baseapp/                # 既有 AI Agent App（技术参考与迁移起点）
    README.md

docs/
  architecture.md         # 系统架构与数据流
  report/outline.md       # 报告提纲（撰写同学维护）
```

## 团队分工映射
- 你（组长）+ 同伴（模型训练）：维护 `model/counseling` 与 `model/assessment` 的数据、配置、训练与评估代码。
- 两位同学（App 开发与部署）：维护 `develop/app`，并与 `develop/services/api` 约定接口；参与 `develop/deployment`。
- 一位同学（Report）：维护 `docs/report/outline.md` 与最终报告文档，协同 `docs/architecture.md`。

## 快速开始
### 1) 后端 API 本地运行
1. 创建虚拟环境并安装依赖（Windows）：
   - `powershell -ExecutionPolicy Bypass -File .\develop\scripts\setup_venv.ps1`
   - 激活虚拟环境：`./.venv/Scripts/Activate.ps1`
2. 进入 API 目录并运行：
   - `cd develop/services/api`
   - `uvicorn main:app --reload --port 8000`
3. 健康检查：访问 `http://localhost:8000/health`

### 2) 模型训练占位（示例）
- 心理咨询模型：`python model/counseling/training/train.py`
- 评测模型：`python model/assessment/training/train.py`
> 训练脚本会读取 `configs/default.yaml`，当前为骨架，后续按数据与方案扩展。

### 3) 移动端问卷资产
- 问卷 JSON 位于 `develop/app/assets/questionnaires/`（已含 PHQ-9 与 GAD-7）。
- App 端可直接消费该 JSON 渲染问题与评分规则。

## 接口约定（初版占位）
- `POST /chat`：入参 `user_id`、`message`；回参 `reply`。
- `POST /assessment/submit`：入参 `user_id`、`instrument`（量表名，如 `PHQ-9`）、`answers`（整型分数组）；回参 `score`、`category`（分级）。

## 合规与隐私
- 不收集可识别信息（如学号/身份证）除非必要并获明确同意。
- 会话与测评数据最小化保存，支持匿名化与脱敏。
- 提供风险提醒与紧急求助指引（在 App 端 UI 与后端策略中体现）。

## 协作规范
- 提交规范与分支策略见 `CONTRIBUTING.md`。
- 大体量数据/模型文件请使用外部存储或 LFS，勿直接入库。

## 下一步
- 决定移动端技术栈（Flutter / React Native），由 App 组在 `develop/app` 继续脚手架。
- 模型组补充数据准备、训练细节与评测指标，完善 `model/*` 目录。
- 部署组完善 `develop/deployment`（云端/边缘/端上路径），对接 CI/CD。
This repository, called largeModelAgents, is a team project for the combinatorics course.
