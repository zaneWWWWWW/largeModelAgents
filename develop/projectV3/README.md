# 项目总览（Camphor Tree）

## 概览
- 前端（Android App）：本地 LLaMA 推理 + 强化 Agent 框架，支持心理评估、记忆查询与对话计数等工具。
- 后端（Spring Boot）：用户认证、头像上传、MBTI/SCL90/统一测评引擎等业务接口。
- 统一系统提示词：本地与远端链路共享同一 `system` 提示词，离线直聊首轮自动注入。

## 目录结构
```
projectV3/
├── app/          # Android 前端
│   ├── src/main/java/com/example/projectv3/
│   │   ├── agent/         # Agent 框架与工具
│   │   ├── fragment/      # AiChatFragment 等界面
│   │   └── ...
│   ├── src/main/cpp/      # CMake/NDK 原生部分（llama.cpp 等）
│   ├── build.gradle       # 前端构建配置
│   └── README.md          # 前端说明
└── server/       # Spring Boot 后端
    ├── src/main/java/com/example/bluecat/
    ├── src/main/resources/
    ├── pom.xml            # Maven 配置（产物名来源）
    └── README.md          # 后端说明
```

## 构建与运行
### 后端
```bash
# 构建（跳过测试）
mvn -q -f server/pom.xml clean package -DskipTests

# 运行
java -jar server/target/camphor-tree-server-1.0.0.jar
```
- 构建输出目录：`server/target/`
- JAR 命名来源：`server/pom.xml` 的 `artifactId` 与 `version`。

### 前端（Windows）
```bash
# 仅构建 Debug 包
cd app
../gradlew.bat :app:assembleDebug -x lint --no-daemon

# 清理并重建
../gradlew.bat clean :app:assembleDebug -x lint --no-daemon
```
- APK 输出：`app/build/outputs/apk/debug/app-debug.apk`
- 首次运行需选择/加载本地 GGUF 模型以启用离线模式。

## 核心业务功能
### 后端
- 用户认证（JWT）：注册、登录、信息查询与修改、改密。
- 文件上传（头像）：类型校验、保存与用户资料更新。
- 心理测评：
  - MBTI：拉取题目/类型查询/更新用户类型。
  - SCL90：题目、结果保存/查询/删除、因子查询。
  - 统一测评引擎：问卷列表/详情、会话创建、作答提交、结果查询；支持管理员新建/导入/导出。

### 前端
- 三种对话模式：普通对话、本地大脑（离线 Agent）、高级 Agent（远端+工具）。
- Agent 工具：心理评估、记忆查询、对话计数。
- 离线直聊优化：首轮注入统一系统提示词；检测空泛复读并同理兜底回复。

## Agent 概览
- 推理范式：ReAct（Thought → Action → Observation → Answer）。
- 工具调用格式（JSON 代码块）：
```json
{
  "tool": "psychological_assessment",
  "parameters": { "trigger_reason": "简要说明触发原因" }
}
```
- 常用工具：
  - `psychological_assessment`：心理状态评估（抑郁/焦虑/风险/困扰分）。
  - `memory_query`：历史对话检索（`recent|keyword|summary`）。
  - `conversation_counter`：对话计数查询/重置。

## 关键位置参考
- 统一提示词：`app/src/main/java/com/example/projectv3/agent/AgentConfig.java`
- 离线直聊注入与兜底：`app/src/main/java/com/example/projectv3/LLamaAPI.java`
- 聊天页与模式切换：`app/src/main/java/com/example/projectv3/fragment/AiChatFragment.java`
- 后端控制器示例：`server/src/main/java/com/example/bluecat/controller/*`
- JAR 命名与构建：`server/pom.xml`、`server/target/`

## 注意事项
- `server/target/` 为构建产物，每次构建都会再生；不应提交到版本库。
- 如需修改 JAR 名称，可在 `server/pom.xml` 调整 `artifactId` 或设置 `<build><finalName>` 固定输出名。
- 前端 CMake 版本固定为 `3.22.1`；请保持 Android SDK 与 NDK 版本一致，避免构建警告。

