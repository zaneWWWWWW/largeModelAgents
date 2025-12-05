# Light Agent Framework 架构分析与 0.5B Brain 模型训练指南

## 1. Light Agent Framework 架构深度解析

### 核心组件关系

在你的 Light Agent Framework 中，存在三个核心实体：**Brain LLM (决策大脑)**、**Tools (工具集)** 和 **Task LLM (执行模型/对话模型)**。

```mermaid
graph TD
    User[用户输入] --> AgentCore[Agent核心引擎]
    
    subgraph "Light Agent Framework"
        AgentCore -->|Prompt + History| BrainLLM[Brain LLM (0.5B)]
        
        BrainLLM -->|JSON指令| AgentCore
        
        AgentCore -->|解析指令| ToolRegistry[工具注册中心]
        
        ToolRegistry -->|调用| T1[心理评估工具]
        ToolRegistry -->|调用| T2[记忆查询工具]
        ToolRegistry -->|调用| T3[Local Chat工具 (Task LLM)]
        
        T1 -->|结果| AgentCore
        T2 -->|结果| AgentCore
        T3 -->|回复| AgentCore
    end
    
    AgentCore -->|最终回复| User
```

### 关键角色定义

1.  **Brain LLM (0.5B)**:
    *   **职责**: 纯粹的**路由(Router)**和**决策者**。
    *   **输入**: 系统提示词 + 工具列表描述 + 对话历史 + 用户当前输入。
    *   **输出**: 严格的 JSON 格式工具调用指令，或者直接回复的标记。
    *   **特点**: 参数量小(0.5B)，推理速度极快，专注于逻辑判断和格式依从性，不需要丰富的通用知识或优美的文笔。

2.  **Tools (工具)**:
    *   `psychological_assessment`: 功能性工具，执行特定逻辑。
    *   `memory_query`: 功能性工具，查库。
    *   **`local_chat` (关键)**: 这是一个特殊的工具。它封装了原本的“对话模型”。当 Brain 认为用户只是在闲聊，或者需要情感陪伴时，它**不应该自己生成回复**（因为它只有0.5B，聊得不好），而是应该调用 `local_chat` 工具，把任务通过参数转交给更强大的对话模型（如 MiniCPM 4B）。

3.  **Task LLM (Local Chat Tool)**:
    *   **职责**: 负责具体的对话生成、心理咨询、情感陪伴。
    *   **特点**: 参数量较大(2B-7B)，生成质量高，有垂类知识。

---

## 2. 训练目标：打造极致轻量化的 Router 模型

现在的目标是训练一个 0.5B 的模型（例如基于 Qwen2.5-0.5B-Instruct 或 Qwen2-0.5B），让它成为一个精准的工具调用者。

### 核心能力要求
1.  **格式依从性**: 必须100%输出合法的 JSON。
2.  **意图识别**: 能准确判断用户的意图是“查历史”、“做评估”还是“闲聊”。
3.  **参数提取**: 能从用户的话语中提取出工具所需的参数。

### 训练数据策略

你需要构建一个专门针对 **Function Calling / Tool Use** 的指令微调数据集。

#### 数据集格式 (ShareGPT 格式示例)

推荐使用类似以下结构的对话数据进行微调：

```json
[
  {
    "conversations": [
      {
        "from": "system",
        "value": "你是一个智能助手。你可以使用以下工具：\n\n1. local_chat: 当用户进行日常对话、闲聊或寻求心理咨询时调用。参数: {'user_input': '用户的原始输入'}\n2. psychological_assessment: 当用户表达明显的情绪困扰(如'我很焦虑')需要专业评估时调用。参数: {'trigger_reason': '触发原因'}\n3. memory_query: 当用户询问历史对话内容时调用。参数: {'query': '查询关键词'}\n\n请根据用户输入输出对应的JSON工具调用。"
      },
      {
        "from": "user",
        "value": "我觉得最近压力好大，整晚睡不着。"
      },
      {
        "from": "assistant",
        "value": "```json\n{\"tool\": \"psychological_assessment\", \"parameters\": {\"trigger_reason\": \"用户表达压力大且失眠\"}}\n```"
      },
      {
        "from": "tool",
        "value": "{\"result\": \"评估完成，建议...\"}"
      },
      {
        "from": "assistant",
        "value": "根据评估结果，你现在的压力水平较高..."
      }
    ]
  },
  {
    "conversations": [
      {
        "from": "system",
        "value": "..."
      },
      {
        "from": "user",
        "value": "你好，今天天气真不错。"
      },
      {
        "from": "assistant",
        "value": "```json\n{\"tool\": \"local_chat\", \"parameters\": {\"user_input\": \"你好，今天天气真不错。\"}}\n```"
      }
    ]
  }
]
```

### 关键微调技巧

1.  **Overfitting 也是一种策略**: 对于 0.5B 模型，为了保证格式稳定，甚至可以适当让它在格式上“过拟合”。确保它在任何情况下都优先输出 JSON。
2.  **负样本训练**: 在训练数据中加入一些不需要调用工具的场景（虽然在你的架构里，建议把“闲聊”也作为一个工具 `local_chat`，这样 Brain 就变成了一个纯粹的分类器，这对于 0.5B 模型来说最容易学）。
3.  **System Prompt 强化**: 在训练数据的 system prompt 中，始终包含工具定义。这样模型在推理时，你只需要动态替换 system prompt 里的工具列表，它就能学会调用新工具（In-context Learning）。

### 推荐基座模型

*   **Qwen2.5-0.5B-Instruct**: 目前最强的 0.5B 级别指令模型，逻辑能力惊人，非常适合做 Router。
*   **Qwen2-0.5B**: 上一代，依然很强。

### 训练环境与参数建议

*   **框架**: LLaMA-Factory (强烈推荐，支持 WebUI，傻瓜式操作)。
*   **显存需求**: 0.5B 模型非常小，6GB 显存甚至 Colab 免费版都能跑全量微调（Full Fine-tuning），不需要 LoRA，效果更好。
*   **学习率**: 2e-5 到 5e-5。
*   **Epoch**: 3-5 轮，观察 loss 收敛情况。

## 3. 在 Android 端集成

训练好并量化为 GGUF 后，在 Android 端的集成逻辑如下：

1.  **初始化**:
    ```java
    // 加载 0.5B 的 Brain 模型
    LLamaAPI brainModel = new LLamaAPI();
    brainModel.loadModel("brain-0.5b-q4.gguf");
    
    // 加载 4B 的 Chat 模型 (作为工具使用)
    LocalLlamaProvider chatModelProvider = new LocalLlamaProvider("chat-4b-q4.gguf");
    
    // 初始化 Manager
    AgentConfig config = new AgentConfig();
    config.setSystemPrompt("你是一个决策助手..."); // 即使模型微调过，最好也加上
    
    AgentManager manager = new AgentManager(context);
    // 传入 brainModel 作为核心，chatModelProvider 作为工具
    manager.initialize(new LocalLlamaProvider(brainModel), config, chatModelProvider);
    ```

2.  **LocalChatTool 的逻辑**:
    在 `LocalChatTool.java` 中，你需要确保它的 `execute` 方法是调用那个大的 Chat 模型生成一段文本返回。

通过这种 **"大模型干重活，小模型做指挥"** 的架构，你可以完美平衡手机端的性能与智能度。0.5B 的 Router 响应极快，用户几乎感觉不到延迟，只有在真正需要深度对话时才加载大模型计算。

