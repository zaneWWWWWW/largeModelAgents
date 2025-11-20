package com.example.projectv3.agent;

import android.content.Context;
import android.util.Log;

import com.example.projectv3.LLamaAPI;
import com.example.projectv3.agent.tools.ConversationCounterTool;
import com.example.projectv3.agent.tools.MemoryTool;
import com.example.projectv3.agent.tools.PsychologicalAssessmentTool;

/**
 * Agent管理器
 * 简化Agent的创建和配置
 */
public class AgentManager {
    private static final String TAG = "AgentManager";
    
    private final Context context;
    private AgentCore agentCore;
    private MemoryTool memoryTool;
    
    public AgentManager(Context context) {
        this.context = context;
    }
    
    /**
     * 初始化Agent（使用默认配置）
     */
    /**
     * 初始化Agent
     * @param llmProvider Agent的“大脑”，可以是本地模型或云端模型
     * @param config Agent的配置
     * @param localChatProvider (可选) 用于创建LocalChatTool的本地模型提供者。当大脑是云端模型时，应提供此项。
     */
    public void initialize(com.example.projectv3.agent.llm.LLMProvider llmProvider, AgentConfig config, @androidx.annotation.Nullable com.example.projectv3.agent.llm.LocalLlamaProvider localChatProvider) {
        Log.d(TAG, "Initializing Agent...");

        // 创建Agent核心
        agentCore = new AgentCore(context, llmProvider, config);

        // 注册工具
        registerBuiltInTools(localChatProvider);

        Log.d(TAG, "Agent initialized with " + agentCore.getToolRegistry().getToolCount() + " tools.");
    }

    /**
     * 注册内置工具
     * @param localChatProvider 如果不为null，则会额外注册LocalChatTool
     */
    private void registerBuiltInTools(@androidx.annotation.Nullable com.example.projectv3.agent.llm.LocalLlamaProvider localChatProvider) {
        // 心理评估工具
        PsychologicalAssessmentTool assessmentTool = new PsychologicalAssessmentTool(context);
        agentCore.registerTool(assessmentTool);
        
        // 记忆管理工具
        memoryTool = new MemoryTool(context);
        agentCore.registerTool(memoryTool);
        
        // 对话计数工具
        ConversationCounterTool counterTool = new ConversationCounterTool(context);
        agentCore.registerTool(counterTool);

        // 如果提供了本地聊天模型，则将其注册为一个工具
        if (localChatProvider != null) {
            com.example.projectv3.agent.tools.LocalChatTool localChatTool = new com.example.projectv3.agent.tools.LocalChatTool(localChatProvider);
            agentCore.registerTool(localChatTool);
            Log.d(TAG, "Registered built-in tools: psychological_assessment, memory_query, conversation_counter, local_chat");
        } else {
            Log.d(TAG, "Registered built-in tools: psychological_assessment, memory_query, conversation_counter");
        }
    }
    
    /**
     * 获取Agent核心实例
     */
    public AgentCore getAgentCore() {
        return agentCore;
    }
    
    /**
     * 检查Agent是否已初始化
     */
    public boolean isInitialized() {
        return agentCore != null;
    }
    
    /**
     * 执行Agent
     */
    public void run(String userInput, AgentCore.AgentCallback callback) {
        if (agentCore == null) {
            callback.onError(new Exception("Agent未初始化"));
            return;
        }
        
        agentCore.run(userInput, callback);
    }
    
    /**
     * 重置Agent状态
     */
    public void reset() {
        if (agentCore != null) {
            agentCore.resetHistory();
            Log.d(TAG, "Agent状态已重置");
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (memoryTool != null) {
            memoryTool.release();
        }
        Log.d(TAG, "Agent资源已释放");
    }
    
    /**
     * 获取默认系统提示词
     */
    private String getDefaultSystemPrompt() {
        return "你是一个乐于助人的AI助手。\n\n" +
               "你的任务是根据用户的输入，决定是否需要使用工具。你的回复必须是以下两种格式之一：\n\n" +
               "1. **调用工具**: 如果你需要使用工具，你的回复**只能**是一个JSON代码块，格式如下：\n" +
               "```json\n" +
               "{\n" +
               "  \"tool\": \"工具名称\",\n" +
               "  \"parameters\": {}\n" +
               "}\n" +
               "```\n" +
               "**绝对不要在JSON前后添加任何文字。**\n\n" +
               "2. **直接回复**: 如果你不需要使用工具，就直接对用户进行回复。\n" +
               "**你的回复必须是给用户的最终答复，不能包含任何'思考'、'回答'之类的标签。**";
    }
    
    /**
     * 获取工具使用统计
     */
    public String getToolUsageStats() {
        if (agentCore == null) {
            return "Agent未初始化";
        }
        
        ToolRegistry registry = agentCore.getToolRegistry();
        return "已注册工具数量: " + registry.getToolCount() + "\n" +
               "工具列表: " + String.join(", ", registry.getToolNames());
    }
}

