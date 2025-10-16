# Contributing

To ensure effective and high-quality collaboration, please follow these guidelines.

## Branching Strategy
- `main`: Only merge reviewed PRs; do not push directly.
- Feature branch naming:
  - Model: `model/<counseling|assessment>-<feature>`
  - App: `app/mobile-<feature>`
  - Service: `service/api-<feature>`
  - Docs: `docs/<topic>`

## Commit Messages (Conventional Commits recommended)
- Format: `<type>(scope): <subject>`
- Common types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
- Example: `feat(api): add assessment endpoint with scoring`

## PR Process
- Describe the background and motivation of changes; link related issues if any.
- Checklist: code runs, basic checks pass, no sensitive information.
- At least one peer review; team lead merges if needed.

## Code & Dependencies
- Python code follows PEP8; optionally use `ruff/black` (formatting workflow may be added later).
- Do not commit large datasets or model weights; use external storage or Git LFS.

## Security & Privacy
- Do not commit real user data to the repository or remote.
- Do not commit local `.env` files (already in `.gitignore`).

---

# 贡献指南（中文）

为保证多人协作的效率与质量，请遵循以下约定。

## 分支策略
- `main`：仅合并通过评审的 PR，不直接推送。
- 功能分支命名：
  - 模型：`model/<counseling|assessment>-<feature>`
  - App：`app/mobile-<feature>`
  - 服务：`service/api-<feature>`
  - 文档：`docs/<topic>`

## 提交信息（Conventional Commits 推荐）
- 格式：`<type>(scope): <subject>`
- 常见 type：`feat`、`fix`、`docs`、`refactor`、`test`、`chore`
- 示例：`feat(api): add assessment endpoint with scoring`

## PR 流程
- 说明变更背景与动机，关联 Issue（如有）。
- 勾选自检清单：代码可运行、基本校验通过、无敏感信息。
- 至少 1 位同学评审，必要时由组长合并。

## 代码与依赖
- Python 代码遵循 PEP8，必要时使用 `ruff/black`（后续可添加格式化工作流）。
- 大体量数据与模型权重不入库，使用外部存储或 Git LFS。

## 安全与隐私
- 不将真实用户数据提交到仓库与远程。
- 本地 `.env` 文件勿提交（已在 `.gitignore`）。