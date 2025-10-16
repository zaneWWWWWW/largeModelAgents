# App Development Guide

This guide consolidates App-side requirements and integration agreements, covering Client (user-facing) and Admin modules, on-device models, data privacy, and server-side deployment.

## Goals & Scope
- Tech stack: determined by the dev team (Flutter, React Native, native Android, etc.).
- Platform compatibility: minimum Android (Android-only is acceptable).
- Client modules: User (AI counseling, questionnaires, profile) and Admin (questionnaire publishing & processing).
- Server deployment: App backend needs to be deployed on a cloud server (e.g., Aliyun ECS).

## Client Features (User)
- AI counseling
  - Use on-device model `basemodel/chat.gguf` for dialogue generation.
  - During counseling, use on-device `basemodel/judgement.gguf` to assess psychological state in real time (local-only).
  - Store session data locally (privacy-preserving); support user clearing and export if needed.
- Psychological questionnaires
  - Render standard instruments (e.g., PHQ-9, GAD-7), compute scores, and display results.
  - Upload questionnaire results to the server for backend statistics and admin viewing.
- Profile center
  - Preferences, privacy notice, emergency help entry.
  - Local data management: view and clear chat logs and model judgement results.

## Admin Features
- Questionnaire publishing
  - Create, update, and retire questionnaires (questions, options, scoring rules, visibility scope).
- Questionnaire processing
  - View submitted results and statistics reports.
  - Export data (CSV/JSON) and provide basic filtering/aggregation.

## On-device Models & Directory
- Model files are placed under `develop/baseapp/basemodel/`
  - Dialogue model: `chat.gguf`
  - Psychological state judgement model: `judgement.gguf`
- Both models run on-device; do not upload raw dialogues or judgement results without user authorization.
- Recommended integration in the App project:
  - Copy/reference `basemodel` into app resources or readable path (e.g., `android/app/src/main/assets/` or app private directory).
  - Use the corresponding inference library (ggml/gguf wrappers for Java/Kotlin/Flutter/RN) to load `chat.gguf` and `judgement.gguf`.

## BaseApp Overview (develop/baseapp/project)
- Tech stack: Native Android (Kotlin/Java), Gradle; optional backend (Spring Boot) for questionnaire management and data storage.
- Modules: AI chat, questionnaires, profile center, basic privacy settings and data cleaning.
- Project structure: mobile app as the main project; backend enabled as needed and exposed via HTTPS.

## BaseModel Usage in BaseApp (develop/baseapp/basemodel)
- `chat.gguf`: offline dialogue generation for the counseling module. Load via inference library and produce replies with conversation context.
- `judgement.gguf`: local-only psychological state tagging during conversations. Results are not uploaded.
- Deployment: place models in app private directory or `assets`; warm up on first launch to avoid first-turn delay.
- Performance & resources: watch model size and memory usage; optionally provide "light/standard" profiles and let users choose in settings.

## Data & Privacy
- Local storage (privacy-preserving)
  - Chat data: local-only, support one-click clearing; not uploaded by default.
  - Psychological state assessment (model judgement): local-only, not uploaded by default.
- Server-side storage
  - Questionnaire results: must be uploaded to the server for statistics and admin viewing.
- Recommendations: local encryption (SQLite/file), explicit privacy notice, data minimization, and offline mode that does not hinder basic use.

## Server & Deployment
- Deployment: cloud server (e.g., Aliyun ECS) with HTTPS and basic security groups.
- Reference backend: `develop/services/api` (FastAPI skeleton, extendable for questionnaire management and result storage).
- Basics:
  - App uses `BASE_URL` pointing to the cloud address (e.g., `https://your-domain.example/`).
  - Questionnaire endpoints:
    - `GET /questionnaires`: fetch published questionnaires with content.
    - `POST /assessment/submit`: submit questionnaire results; server persists.
  - Chat and psychological state judgement are performed on-device; if cloud fallback is needed, evaluate separately with a feature toggle.

## API Agreements (initial version aligned with this repo)
- Determined by the dev team.

## Compatibility
- Minimum platform: Android.

## Acceptance Criteria
- User side:
  - Counseling: dialogue generation and local psychological judgement work properly.
  - Questionnaires: correct rendering and scoring; results uploaded successfully and visible on admin side.
  - Profile center: privacy notice and data clearing entry available.
- Admin side:
  - Publishing workflow is smooth; APIs and frontend integrate properly.
  - Results page can view and export statistics.
- Security & privacy:
  - Local data is not uploaded by default; HTTPS and auth strategies work (if needed).
  - Clear privacy policy and user consent flow.

---

# 开发组 App 需求与实现指南（中文）

本指南汇总 App 侧的明确需求与对接约定，覆盖用户端（Client）与管理端（Admin），以及端侧模型、数据隐私与服务端部署要求。

## 目标与范围
- 技术选型：开发组自行决定（Flutter、React Native、原生 Android 等）。
- 平台兼容：最低要求为 Android（安卓端独占即可）。
- 客户端模块：用户端（AI 咨询、心理问卷、个人中心）与管理端（问卷发布与处理）。
- 服务端部署：App 服务器端需要部署在云服务器（如：阿里云 ECS）。

## 客户端功能（用户端）
- AI 心理咨询
  - 使用端侧模型 `basemodel/chat.gguf` 进行对话生成。
  - 咨询过程中，使用端侧模型 `basemodel/judgement.gguf` 对用户心理状态进行实时判断与标注（仅本地使用）。
  - 会话数据存储在本地（隐私保护），支持用户清理与导出（如需）。
- 心理问卷调查
  - 渲染标准量表（如 PHQ-9、GAD-7），完成评分与结果展示。
  - 问卷结果需要上传服务器端存储（用于后端统计与管理端查看）。
- 个人中心
  - 资料与偏好设置、隐私说明、紧急求助入口。
  - 本地数据管理：聊天记录与模型判断结果的查看与清理。

## 管理端功能（Admin）
- 问卷发布
  - 支持创建、更新与下线问卷（题目、选项、评分规则、可见范围）。
- 问卷处理
  - 查看问卷提交结果与统计报表。
  - 导出数据（CSV/JSON）与基础筛选、聚合分析。

## 端侧模型与目录
- 模型文件统一放置于仓库路径：`develop/baseapp/basemodel/`
  - 对话模型：`chat.gguf`
  - 心理状态判断模型：`judgement.gguf`
- 两个模型均在端侧部署与推理，未经用户授权不上传原始对话与判断结果。
- 在 App 工程中引入模型的推荐方式：
  - 将 `basemodel` 目录复制/引用到 App 的资源或可读路径（例如 `android/app/src/main/assets/` 或应用私有目录）。
  - 使用对应的推理库（如 ggml/gguf 的 Java/Kotlin/Flutter/RN 封装）加载 `chat.gguf` 与 `judgement.gguf`。

## BaseApp 项目简介（develop/baseapp/project）
- 技术栈：Android 原生（Kotlin/Java），Gradle 构建；可选集成后端服务（Spring Boot），用于问卷管理与数据存储。
- 功能模块：AI 咨询聊天、心理问卷、个人中心、基础隐私设置与数据清理。
- 工程结构：移动端 App 工程为主体；后端工程按需启用并通过 HTTPS 暴露接口。

## BaseModel 在 BaseApp 中的用途（develop/baseapp/basemodel）
- `chat.gguf`：提供离线对话生成能力，用于 AI 咨询模块。通过相应推理库加载，结合对话上下文产生回复。
- `judgement.gguf`：在聊天过程中进行用户心理状态的本地判断与标签，仅本地使用，不上传原始结果。
- 部署建议：将模型放置在应用私有目录或 `assets`，首次启动可进行模型加载预热，避免首轮对话延迟。
- 性能与资源：关注模型体积与内存占用，必要时提供“轻量/标准”两档配置，允许用户在设置中选择。

## 数据与隐私
- 本地存储（隐私保护）
  - 聊天数据：仅本地保存，支持用户一键清理；默认不上传。
  - 心理状态评估结果（模型判断）：仅本地保存，默认不上传。
- 服务器端存储
  - 问卷结果：需要上传至服务器端，用于统计与管理端查看。
- 建议：本地数据加密（SQLite/文件加密）、显式隐私说明、最小化收集原则、离线模式下不影响基本使用。

## 服务端与部署
- 部署位置：云服务器（如阿里云 ECS），开启 HTTPS 与基础安全组配置。
- 参考后端：`develop/services/api`（FastAPI 骨架，可扩展问卷管理与结果存储接口）。
- 基本约定：
  - App 端通过 `BASE_URL` 指向云端地址（例如 `https://your-domain.example/`）。
  - 问卷相关接口需提供：
    - `GET /questionnaires`：拉取已发布问卷列表与内容。
    - `POST /assessment/submit`：提交问卷结果，服务端持久化。
  - Chat 与心理状态判断在端侧完成；如需云端兜底，请单独评估与开关控制。

## 接口约定（与本仓库保持一致的初版）
- 开发组自行决定

## 兼容性
- Android 为最低兼容平台。

## 验收标准
- 用户端：
  - AI 咨询可用：对话生成与心理状态本地判断正常工作。
  - 问卷：渲染与评分准确；结果成功上传至服务器端并可在管理端查看。
  - 个人中心：隐私说明与数据清理入口可用。
- 管理端：
  - 问卷发布流程顺畅，接口与前端联动正常。
  - 结果处理页面可查看与导出统计数据。
- 安全与隐私：
  - 本地数据默认不上传；HTTPS 与鉴权策略生效（如需）。
  - 明确的隐私政策与用户同意流程。
