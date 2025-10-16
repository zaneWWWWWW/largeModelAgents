# largeModelAgents

largeModelAgents is a team project for the combinatorics course. It delivers a student-oriented mobile AI Agent for psychological counseling and assessment. This repository uses a monorepo structure to coordinate model training, backend services, mobile app, and documentation.

## Project Goals (based on the App design)
- AI counseling: Use on-device `basemodel/chat.gguf` to generate dialogues, and `basemodel/judgement.gguf` to locally assess the user’s psychological state (privacy-preserving).
- Psychological questionnaires: Render standard instruments (e.g., PHQ-9, GAD-7), compute scores, and display results. Upload questionnaire results to the cloud for admin-side processing and statistics.
- Profile center: Privacy notice, preferences, and data cleaning entry to ensure the user controls local data (chat logs and assessment results).
- Admin: Publish and process questionnaires (view, statistics, export) with smooth data interaction with the App.
- Deployment & platform: Deploy App-side services on a cloud server (e.g., Aliyun ECS). Minimum platform: Android.
- Teaching & collaboration: Improve engineering practices, AI knowledge, documentation, collaboration, and team coordination.

## Repository Structure (top-level)
- `develop/`: Development directory including the mobile App, backend services, deployment scripts, and cloud integration notes.
- `docs/`: Documentation and reports for architecture descriptions and coursework.
- `model/`: Model code for counseling and assessment training, inference, and evaluation.

## Team Division Mapping
- Model training: Zane Wang, Classmate A — data preparation, training/evaluation, on-device/server-side inference.
- App development & deployment: Classmate B, Classmate C — user/admin app features, model integration, cloud deployment.
- Documentation: Classmate D — architecture docs and course report authoring/maintenance.

## Compliance & Privacy
- Do not collect personally identifiable information unless necessary and with explicit consent.
- Minimize storage of session and assessment data; support anonymization and de-identification.
- Provide risk reminders and emergency support guidance (in App UI and backend policy).

## Collaboration
- See `CONTRIBUTING.md` for commit standards and branching strategy.
- Store large datasets/model files externally or via LFS; do not check them into the repo.

---

# largeModelAgents（中文）
面向学生用户的移动端心理咨询与心理评测 AI Agent 项目。本仓库采用单仓库（monorepo）结构，支持模型训练、后端服务、移动端应用与报告编写的协同开发。

## 项目目标（基于 App 功能设计）
- AI 心理咨询：在咨询过程中，端侧使用 `basemodel/chat.gguf` 生成对话，并使用 `basemodel/judgement.gguf` 对用户心理状态进行本地判断（隐私保护）。
- 心理问卷调查：支持标准量表（如 PHQ-9、GAD-7）渲染与评分；结果上传云端服务器存储，供管理端处理与统计。
- 个人中心：隐私说明、偏好设置与数据清理入口，确保用户对本地数据（聊天与状态评估结果）拥有控制权。
- 管理端：问卷发布与处理（查看、统计、导出），与 App 端数据交互通畅。
- 部署与平台：App 服务器端部署在云服务器（如阿里云），最低兼容平台为 Android（安卓端独占即可）。
- 教学与协作提升：通过本项目提升学生的工程开发能力、AI 相关知识、文档撰写能力、团队协作与团队统筹能力。

## 仓库结构（顶层概览）
- `develop/`：开发组目录，包含移动端 App、后端服务与部署脚本，以及与云服务器的对接说明。
- `docs/`：文档与报告目录，用于系统架构说明与课程报告撰写。
- `model/`：模型目录，包含心理咨询与心理评测相关的训练、推理与评估代码。

## 团队分工映射
- 模型训练：Zane Wang、同学A。负责模型数据准备、训练与评估，以及端侧/服务端推理方案。
- App 开发与部署：同学B、同学C。App 负责用户端与管理端实现、端侧模型集成与云端部署。
- 文档撰写：同学D 负责架构文档与课程报告的编写与维护。

## 合规与隐私
- 不收集可识别信息（如学号/身份证）除非必要并获明确同意。
- 会话与测评数据最小化保存，支持匿名化与脱敏。
- 提供风险提醒与紧急求助指引（在 App 端 UI 与后端策略中体现）。

## 协作规范
- 提交规范与分支策略见 `CONTRIBUTING.md`。
- 大体量数据/模型文件请使用外部存储或 LFS，勿直接入库。

