# Android 前端（App）

## 概览
- AI 心理咨询移动端，含本地 LLaMA 推理与高级 Agent 集成。
- 原生部分使用 CMake/NDK 构建 `llama.cpp`，Java 层通过 `LLamaAPI` 调用。
- 本地与远端模型共享统一系统提示词，离线直聊也注入 `system` 指令。

## 核心功能
- 聊天与对话管理：普通模式、本地大脑、强化 Agent 三种模式随时切换。
- 本地推理：在设备端加载 GGUF 模型进行离线生成，首轮自动注入统一系统提示词。
- Agent 工具能力：心理评估、记忆查询、对话计数等工具在远端/本地 Agent 中统一可用。
- 兜底优化：检测空泛复读后替换为同理型回复，提升离线可用性（`LLamaAPI.java`）。

## 环境准备
- 安装 Android SDK，并在本机配置 `local.properties`：`sdk.dir=C:\Users\<你>\AppData\Local\Android\Sdk`（文件已存在，参考 `local.properties:8`）。
- CMake 固定版本：`3.22.1`；配置见 `build.gradle:58–63, 80–84`。
- Gradle Wrapper 已包含：`gradlew`、`gradlew.bat`。

## 构建
```bash
# Windows（推荐）
./gradlew.bat :app:assembleDebug -x lint --no-daemon

# 清理并重建
./gradlew.bat clean :app:assembleDebug -x lint --no-daemon
```
- 构建产物：`app/build/outputs/apk/debug/app-debug.apk`
- 构建缓存：`app/.cxx`、`app/build`；清理后会在下一次构建自动再生。

## 运行
- 使用 Android Studio 安装 `app-debug.apk` 到模拟器或真机。
- 首次运行需在应用内选择或加载本地模型（离线模式）。

## 模式与 Agent
- 模式在聊天页可切换：
  - 普通对话（不使用 Agent/工具）
  - 本地大脑（离线 Agent，走本地模型）
  - 高级 Agent（远端推理，支持工具调用）
- 初始化逻辑：`fragment/AiChatFragment.java:491–510` 中根据模式调用 `AgentManager.initialize(...)`。
- 统一系统提示词：`agent/AgentConfig.unifiedSystemPrompt()`，在 `fragment/AiChatFragment.java:494` 注入到 Agent 配置。

## Agent 工具（核心）
- psychological_assessment：基于对话历史进行心理状态评估，返回抑郁/焦虑/风险/困扰分。
- memory_query：查询历史对话，支持 `recent|keyword|summary` 三种查询类型。
- conversation_counter：查询或重置对话计数，辅助评估触发时机。

## 快速使用
- 长按发送按钮切换模式：普通 ↔ 本地大脑 ↔ 高级 Agent。
- 模型已加载时自动初始化 Agent（`fragment/AiChatFragment.java:491–510`）。
- 工具调用采用 JSON 代码块格式，Agent 自动识别并执行：
  ```json
  {
    "tool": "psychological_assessment",
    "parameters": { "trigger_reason": "简要说明触发原因" }
  }
  ```

## 离线直聊的系统提示与校验
- 离线直聊在首轮自动注入 `system` 提示词：`LLamaAPI.java:444–449`。
- 清理生成文本中的特殊标记：`LLamaAPI.java:279–295`。
- 兜底优化：检测空泛复读后替换为更同理的回复：`LLamaAPI.java:559–562` 与 `LLamaAPI.java:295` 的通用话术检测。

## 原生构建
- Gradle 调用 CMake：`build.gradle:58–63, 80–84`，入口 `src/main/cpp/CMakeLists.txt`。
- 构建出的库位于 `app/.cxx/Release/<abi>/bin/`，例如 `libllama.so`、`libggml.so`。

## 常见问题
- 构建后出现 CMake 版本/SDK XML 警告但成功：保持 Android SDK 与 CMake 版本一致即可。
- 无法生成回复或空泛重复：离线链路已加入系统提示与兜底；仍异常时，重置会话或重载模型。

## 重要文件
- 统一提示词：`agent/AgentConfig.java:105–107`
- Agent 配置与初始化：`fragment/AiChatFragment.java:491–510`
- 本地直聊 API：`LLamaAPI.java:435–516, 519–582`
- CMake 配置：`build.gradle:58–63, 80–84`；`src/main/cpp/CMakeLists.txt`
