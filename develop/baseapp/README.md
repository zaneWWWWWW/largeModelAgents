# BaseApp (Technical Reference)

This directory hosts your previously developed AI Agent App as a technical reference and migration starting point.

## Import Suggestions
- If you already have a repository: copy code into this directory or add it as a submodule.
- Keep the following key modules for reference:
  - Routing and navigation structure (pages and flow)
  - State management (sessions, questionnaires, settings)
  - Network layer (API calls, error handling, retry strategy)
  - Local storage (caching and privacy clearing)

## Integration Notes
- Adjust API base URL and protocols to integrate with `develop/services/api`.
- Use unified questionnaire JSON from `develop/app/assets/questionnaires/`.
- Follow privacy & security guidance in `docs/architecture.md` and the top-level README.

## Next Steps
- After the dev team chooses the tech stack, scaffold the actual project under `app/`.
- Migrate reusable modules from BaseApp to the new project (components, services, utilities, etc.).

---

## Paths
- Project path: `develop/baseapp/project`
- Local models: `develop/baseapp/basemodel` (contains `chat.gguf` and `judgement.gguf`)

## Run Guide (Brief)
### Android App
1. Open `develop/baseapp/project` (Android Studio or CLI).
2. Build via CLI:
   - Windows: `cd develop/baseapp/project && .\gradlew.bat assembleDebug`
   - Output: `app\build\outputs\apk\debug\app-debug.apk`
3. Adjust backend address: set `BASE_URL` to `http://localhost:8000/` (or your server address) in `ApiClient` or related constants.
4. If using on-device inference: place `chat.gguf` and `judgement.gguf` in an app-readable location (e.g., `app/src/main/assets`) and configure load paths in code.

### Backend (optional in legacy project)
- Path: `develop/baseapp/project/server`
- Start: `mvn package` or `mvn spring-boot:run` (port usually `8080`)
- Recommended unified backend in this repo: `develop/services/api` (FastAPI). If keeping the legacy backend, use it for comparison and reference.

## Integration with This Repo
- Unify chat and assessment endpoints to `develop/services/api`:
  - `POST /chat`: `{ user_id, message }` → `{ reply }`
  - `POST /assessment/submit`: `{ user_id, instrument, answers:int[] }` → `{ score, category }`
- Questionnaire source: `develop/app/assets/questionnaires/` (PHQ-9, GAD-7 provided).
- Model paths: for on-device inference, encapsulate model loading service to centralize gguf file paths and versions.

## Data & Privacy
- Do not commit model files under `basemodel` to remote (top-level `.gitignore` already ignores `*.gguf` and that directory).
- Do not commit real user data or private configs; build artifacts are ignored.

---

# BaseApp（技术参考，中文）

此目录用于放置你之前开发的 AI Agent App 作为技术参考与迁移起点。

## 导入建议
- 若已有仓库：可直接复制代码至本目录或作为子模块引入。
- 保留以下关键模块以便参考：
  - 路由与导航结构（页面层级与跳转流）
  - 状态管理（会话、问卷、设置）
  - 网络层封装（接口调用、错误处理、重试策略）
  - 本地存储（缓存与隐私清理）

## 对接本项目的注意点
- 调整接口地址与协议：对接 `develop/services/api` 暴露的接口。
- 使用统一问卷 JSON：`develop/app/assets/questionnaires/`。
- 隐私与安全提示：参考 `docs/architecture.md` 与顶层 README 的合规要求。

## 后续步骤
- 开发组确定技术栈后，在 `app/` 目录脚手架实际工程。
- 将 BaseApp 的可复用模块迁移至新工程（组件、服务、工具函数等）。

## 路径定位
- 项目路径：`develop/baseapp/project`
- 本地模型：`develop/baseapp/basemodel`（包含 `chat.gguf` 与 `judgement.gguf`）

## 运行指南（简版）
### Android App
1. 打开 `develop/baseapp/project`（Android Studio 或命令行）。
2. 命令行构建：
   - Windows：`cd develop/baseapp/project && .\gradlew.bat assembleDebug`
   - 输出位置：`app\build\outputs\apk\debug\app-debug.apk`
3. 调整后端地址：根据旧项目说明，在 `ApiClient` 或相关常量处将 `BASE_URL` 指向 `http://localhost:8000/`（或你的服务器地址）。
4. 若使用端侧推理：将 `chat.gguf` 与 `judgement.gguf` 放置到应用可读位置（示例：`app/src/main/assets`），并在代码中配置读取路径。

### 后端（旧项目内可选）
- 路径：`develop/baseapp/project/server`
- 启动方式：`mvn package` 或 `mvn spring-boot:run`（端口通常为 `8080`）
- 本仓库推荐统一后端为 `develop/services/api`（FastAPI）；如需保留旧后端，可作为对照与参考。

## 与本仓库的集成建议
- 对话与评测接口统一对接 `develop/services/api`：
  - `POST /chat`：`{ user_id, message }` → `{ reply }`
  - `POST /assessment/submit`：`{ user_id, instrument, answers:int[] }` → `{ score, category }`
- 问卷统一来源：`develop/app/assets/questionnaires/`（PHQ-9、GAD-7 已提供）。
- 模型路径：如需端侧推理，建议封装模型加载服务，集中管理 gguf 文件路径与版本。

## 数据与隐私
- 请勿将 `basemodel` 下的模型文件提交到远端（顶层 `.gitignore` 已忽略 `*.gguf` 与该目录）。
- 不提交真实用户数据与私密配置；构建产物已加入忽略清单。