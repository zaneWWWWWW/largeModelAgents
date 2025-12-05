# 小香樟心理助手 - Android App

[![Android](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-28%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=28)

基于本地AI大模型的心理健康助手 Android 客户端，集成轻量级 Agent 框架，支持多种 LLM 后端。

## 📱 功能特性

### 本地AI推理
- 基于 llama.cpp 引擎的本地推理
- 支持离线运行，无需网络
- 支持 arm64-v8a 架构

### Agent智能框架
- **ReAct推理模式**：思考→行动→观察→回答
- **多 LLM 后端支持**：
  - 本地 LLaMA 模型 (`LocalLlamaProvider`)
  - Gemini API (`GeminiApiClient`)
  - 备用 Gemini 提供者 (`FallbackGeminiProvider`)
- **工具系统**：
  - 心理状态评估工具 (`PsychologicalAssessmentTool`)
  - 对话记忆查询工具 (`MemoryTool`)
  - 对话计数管理工具 (`ConversationCounterTool`)
  - 本地聊天工具 (`LocalChatTool`)

### 心理健康功能
- 多轮智能对话
- 情绪识别与支持
- 危机干预引导
- 心理测试评估（MBTI、SCL-90、统一测评）

## 🏗️ 项目结构

```
app/
├── build.gradle              # 项目根配置
├── settings.gradle           # Gradle设置
├── gradle.properties         # Gradle属性
├── gradlew                   # Gradle Wrapper
├── gradle/                   # Gradle Wrapper目录
├── AGENT_INTEGRATION_GUIDE.md # Agent集成指南
├── app/                      # App模块
│   ├── build.gradle          # App模块配置
│   ├── proguard-rules.pro    # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/projectv3/
│       │   ├── agent/              # Agent框架
│       │   │   ├── AgentConfig.java
│       │   │   ├── AgentCore.java
│       │   │   ├── AgentManager.java
│       │   │   ├── Tool.java
│       │   │   ├── ToolRegistry.java
│       │   │   ├── llm/            # LLM提供者
│       │   │   │   ├── LLMProvider.java
│       │   │   │   ├── LocalLlamaProvider.java
│       │   │   │   ├── GeminiApiClient.java
│       │   │   │   └── FallbackGeminiProvider.java
│       │   │   └── tools/          # 内置工具
│       │   │       ├── PsychologicalAssessmentTool.java
│       │   │       ├── MemoryTool.java
│       │   │       ├── ConversationCounterTool.java
│       │   │       └── LocalChatTool.java
│       │   ├── adapter/            # 列表适配器
│       │   ├── api/                # 网络接口
│       │   ├── db/                 # 本地数据库
│       │   ├── dto/                # 数据传输对象
│       │   ├── fragment/           # UI界面
│       │   ├── model/              # 数据模型
│       │   ├── service/            # 后台服务
│       │   ├── utils/              # 工具类
│       │   └── LLamaAPI.java       # AI推理接口
│       ├── cpp/                    # C++ JNI代码 (llama.cpp)
│       ├── assets/                 # 资源文件
│       │   └── agent_prompts.txt   # Agent提示词模板
│       └── res/                    # Android资源
└── splash_image.png
```

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Java |
| 最小SDK | API 28 (Android 9.0) |
| 目标SDK | API 33 (Android 13) |
| 架构 | MVVM |
| UI框架 | Material Design |
| AI引擎 | llama.cpp (NDK) |
| NDK版本 | 25.1.8937393 |
| CMake版本 | 3.22.1 |
| 网络请求 | Retrofit + OkHttp |

## 🚀 构建指南

### 环境要求

- Java JDK 17+
- Android SDK (API 32+)
- Android NDK 25.1.8937393
- CMake 3.22.1+

### 构建命令

```bash
# Debug版本
./gradlew assembleDebug

# Release版本
./gradlew assembleRelease

# 清理并重建
./gradlew clean assembleDebug

# 安装到设备
adb install app/build/outputs/apk/debug/app-debug.apk
```

### APK输出位置
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 📦 模型配置

应用使用本地AI模型，首次运行需要：
1. 准备 GGUF 格式的心理健康领域模型
2. 将模型文件放置到设备存储
3. 在应用中选择模型文件路径

推荐模型规格：
- 格式：GGUF
- 参数量：0.5B-1.5B（适配移动设备）
- 领域：心理健康微调版

> ⚠️ 模型文件（*.gguf）不包含在仓库中，需自行准备

## 🔧 Agent工具调用

工具调用采用 JSON 格式：

```json
{
  "tool": "psychological_assessment",
  "parameters": { "trigger_reason": "用户表达焦虑情绪" }
}
```

### 可用工具

| 工具名 | 功能 | 参数 |
|--------|------|------|
| `psychological_assessment` | 心理状态评估 | `trigger_reason`: 触发原因 |
| `memory_query` | 历史对话检索 | `query_type`: recent/keyword/summary |
| `conversation_counter` | 对话计数 | `action`: query/reset |
| `local_chat` | 本地模型对话 | `message`: 消息内容 |

## 📍 关键代码位置

| 功能 | 文件路径 |
|------|---------|
| Agent配置 | `app/.../agent/AgentConfig.java` |
| Agent核心 | `app/.../agent/AgentCore.java` |
| LLM接口 | `app/.../agent/llm/LLMProvider.java` |
| 工具注册 | `app/.../agent/ToolRegistry.java` |
| 聊天界面 | `app/.../fragment/AiChatFragment.java` |
| 本地推理 | `app/.../LLamaAPI.java` |
| 提示词模板 | `app/src/main/assets/agent_prompts.txt` |

## 📚 相关文档

- [Agent集成指南](AGENT_INTEGRATION_GUIDE.md) - 详细的 Agent 框架使用和扩展说明

## 🔒 隐私与安全

- ✅ 核心AI推理在本地完成
- ✅ 对话历史本地存储
- ✅ 支持清除历史数据
- ✅ 敏感配置不上传仓库

## ⚠️ 免责声明

本应用仅供心理健康辅助使用，不能替代专业心理治疗。如遇严重心理健康问题，请及时寻求专业帮助。

紧急求助热线：
- 全国心理援助热线：12320
- 全国危机干预热线：400-161-9995

---

**让每一位学生都能获得专业的心理健康支持 💚**
