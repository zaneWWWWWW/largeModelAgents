# System Architecture (Initial)

## Overview
- Mobile App: provides dialogues, assessment, result display, and safety guidance.
- Backend API: unified ingress layer handling session routing, assessment endpoints, and risk control policies.
- Model layer:
  - Counseling model (dialogue): generates safe, empathetic, and helpful responses.
  - Assessment model (scoring/classification): computes instrument scores and categories.
- Deployment: a combination of on-device, edge, or cloud; choose based on resources and compliance.

## Data Flow
1. The App sends user inputs and questionnaire answers to the backend via API.
2. The backend routes to the appropriate model inference or assessment logic.
3. Returns dialogue replies or instrument scores/categories, and records necessary anonymous audit info.

## Privacy & Risk Control
- Data minimization: collect/store only when necessary.
- Sensitive data de-identification and encryption; configure emergency help and risk warning mechanisms.
- Rate limiting and input validation on both client and backend.

## Deployment Options
- On-device inference: low latency and privacy-friendly, limited by device compute and package size.
- Edge/cloud: easier updates and centralized management; high availability but strict data and compliance control required.
- Mix by module: on-device assessment and cloud-based counseling (or vice versa) depending on resources and UX.

---

# 系统架构（中文）

## 总览
- 移动端 App：提供对话、量表测评、结果展示与安全指引。
- 后端 API：统一接入层，负责会话路由、评测接口与风控策略。
- 模型层：
  - 心理咨询模型（对话）：负责安全、同理心与建议提示的生成。
  - 心理评测模型（打分/分类）：负责量表分数计算与分级判定。
- 部署：端上、边缘或云端的组合，因资源与合规做差异化选择。

## 数据流
1. App 将用户输入与问卷答案通过 API 发送至后端。
2. 后端根据路由调用对应模型推理或评测逻辑。
3. 返回对话回复或量表分数与分级，并记录必要的匿名审计信息。

## 隐私与风控
- 最小化收集原则：仅在必要时采集与存储。
- 敏感信息脱敏与加密；配置紧急求助与风险提示机制。
- 端上与后端都应设置速率限制与输入安全校验。

## 部署选型
- 端上推理：低延迟、隐私友好，但受限于算力与包体。
- 边缘/云端：易更新与统一管理，高可用但需严控数据与合规。
- 可按模块分治：评测端上、咨询云端，或反之，视资源与体验定夺。