# 开发组说明（develop）

本目录由开发小组维护，包含移动端 App、后端服务、部署与辅助脚本。技术栈由开发组自行确定（Flutter 或 React Native 等）。

## 需求概述（由组长提出）
- 心理咨询对话：与后端 `/chat` 接口交互，提供安全提示与风险识别。
- 心理评测：支持 PHQ-9、GAD-7 渲染与评分展示，与后端 `/assessment/submit` 交互。
- 结果页：展示分数、分级与解释，并给出下一步建议（提示资源与求助入口）。
- 隐私与合规：数据最小化存储、本地缓存可清理、显式的隐私说明与紧急求助入口。
- 异常处理：网络错误、接口失败的统一提示与重试机制。

## 目录说明
- `app/`：移动端应用与资源（问卷 JSON 已提供）。
- `services/api/`：后端 API 服务骨架（FastAPI）。
- `deployment/`：一键启动与容器化示例（docker-compose）。
- `scripts/`：Windows 便捷脚本（虚拟环境与依赖）。
- `baseapp/`：已导入旧版 AI Agent App 与本地模型；详见 `baseapp/README.md`。

### BaseApp 快速指引
- 项目：`develop/baseapp/project`（Android Gradle 工程，详见其 `README.md`）
- 本地模型：`develop/baseapp/basemodel`（`chat.gguf`、`judgement.gguf`）
- 构建示例：`cd develop/baseapp/project && .\gradlew.bat assembleDebug`
- 与本仓库的后端对接：将旧 App 中的 `BASE_URL` 指向 `http://localhost:8000/` 或你的部署地址；接口定义见下文。

## 接口约定（初版）
- `POST /chat`：`{ user_id, message }` → `{ reply }`
- `POST /assessment/submit`：`{ user_id, instrument, answers:int[] }` → `{ score, category }`
- `GET /health`：健康检查

## 验收标准
- 基本流程可用：对话、评测、结果展示完整闭环。
- 可恢复的错误处理：离线/接口异常有回退与重试。
- 隐私保护：本地数据清理、日志不含敏感内容。
- 文档完善：`README`、运行指南、打包说明、接口对接说明。

## 开发建议
- 统一接口层：封装 HTTP 客户端，集中处理错误、重试、超时与鉴权（如有）。
- 独立问卷渲染组件：消费 `app/assets/questionnaires/` 的 JSON，评分逻辑统一。
- UI 文案与提示统一管理：便于迭代与本地化（如需）。
- 模块化状态管理：区分对话状态、测评状态与用户偏好设置。