# Mobile App (placeholder)

Recommended tech stacks:
- Flutter (recommended for consistent cross-platform UX and mature ecosystem), or
- React Native (if the team is more familiar with RN).

## Target Features
- Counseling chat: safety prompts and risk detection, emotional support and suggestions.
- Assessments: render instruments (PHQ-9, GAD-7), compute scores and categories.
- Results display: scores, category, interpretation, and next-step suggestions.
- Privacy & compliance: local caching, data minimization, emergency help entry.

## Development Notes
- Consume questionnaire JSON and scoring rules from `assets/questionnaires/`.
- Integrate with `services/api` endpoints; handle network exceptions and retries.
- Unify error copy and risk warning copy.

---

# 移动端应用（中文占位）

建议技术栈：
- Flutter（推荐，跨平台一致性好，生态成熟），或
- React Native（如团队前端更熟悉 RN）。

## 目标功能
- 心理咨询对话：安全提示与风险识别、情绪支持与建议。
- 心理评测：渲染量表题目（PHQ-9、GAD-7），计算分数与分级。
- 结果展示：分数、分级、解释与下一步建议。
- 隐私与合规：本地缓存、数据最小化、紧急求助入口。

## 开发建议
- 在 `assets/questionnaires/` 直接消费 JSON 问卷定义与评分规则。
- 与 `services/api` 约定接口，处理网络异常与重试。
- 统一错误文案与风险提示文案。