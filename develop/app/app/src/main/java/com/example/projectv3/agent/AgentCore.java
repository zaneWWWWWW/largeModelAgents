package com.example.projectv3.agent;

import android.content.Context;
import android.util.Log;

import com.example.projectv3.LLamaAPI;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Agent核心类
 * 实现ReAct (Reasoning + Acting) 模式的Agent
 */
public class AgentCore {
    private static final String TAG = "AgentCore";
    
    private final Context context;
    private final com.example.projectv3.agent.llm.LLMProvider llmProvider;
    private final ToolRegistry toolRegistry;
    private final AgentConfig config;
    private final List<String> conversationHistory;
    
    // 工具调用的正则表达式模式 (旧格式)
    private static final Pattern TOOL_CALL_PATTERN = Pattern.compile(
        "```json\\s*\\{\\s*\"tool\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"parameters\"\\s*:\\s*\\{([^}]*)\\}\\s*\\}\\s*```",
        Pattern.DOTALL
    );
    
    // 多工具调用的正则表达式模式 (新格式 V2) - 更宽松的匹配
    private static final Pattern MULTI_TOOL_PATTERN = Pattern.compile(
        "\\{\\s*\"tools\"\\s*:\\s*\\[",
        Pattern.DOTALL
    );
    
    public AgentCore(Context context, com.example.projectv3.agent.llm.LLMProvider llmProvider, AgentConfig config) {
        this.context = context;
        this.llmProvider = llmProvider;
        this.config = config;
        this.toolRegistry = new ToolRegistry();
        this.conversationHistory = new ArrayList<>();
        
        // 设置LLM参数
        if (llmProvider != null) {
            llmProvider.setTemperature(config.getTemperature());
        }
    }
    
    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        toolRegistry.registerTool(tool);
    }
    
    /**
     * 获取工具注册中心
     */
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }
    
    /**
     * 执行Agent推理循环
     * @param userInput 用户输入
     * @param callback 回调接口
     */
    public void run(String userInput, AgentCallback callback) {
        if (llmProvider == null) {
            callback.onError(new Exception("LLMProvider未设置"));
            return;
        }
        
        // 在后台线程执行
        new Thread(() -> {
            try {
                // 构建系统提示词
                String systemPrompt = buildSystemPrompt();
                
                // 添加用户输入到历史
                conversationHistory.add("用户: " + userInput);
                
                // ReAct循环
                long agentStartTime = System.currentTimeMillis();
                long timeoutMillis = 150000; // 150秒超时

                int iteration = 0;
                String currentThought = userInput;
                StringBuilder finalResponse = new StringBuilder();
                boolean timedOut = false;

                while (iteration < config.getMaxIterations()) {
                    // 检查是否超时
                    if (System.currentTimeMillis() - agentStartTime > timeoutMillis) {
                        Log.w(TAG, "Agent执行超时");
                        callback.onError(new Exception("AI思考超时，请稍后重试"));
                        timedOut = true;
                        break;
                    }
                    iteration++;
                    Log.d(TAG, "Agent迭代 " + iteration + "/" + config.getMaxIterations());
                    
                    // 构建完整提示词
                    String prompt = buildPrompt(systemPrompt, currentThought);
                    
                    // 调用LLM
                    StringBuilder llmResponse = new StringBuilder();
                    final Object lock = new Object();
                    final boolean[] completed = {false};
                    final Exception[] error = {null};
                    
                    llmProvider.generateResponse(prompt, new com.example.projectv3.agent.llm.LLMProvider.CompletionCallback() {
                        private boolean isFinalAnswerStreaming = false;
                        private boolean isToolCallDetected = false;

                        @Override
                        public void onToken(String token) {
                            if (token != null) {
                                llmResponse.append(token);
                                String currentText = llmResponse.toString();
                                
                                // 检测是否是工具调用格式（新格式或旧格式）
                                if (!isToolCallDetected) {
                                    boolean hasToolsKeyword = currentText.contains("\"tools\"") || currentText.contains("\"tool\"");
                                    boolean hasJsonBlock = currentText.contains("```json") || currentText.contains("{\"tools\"") || currentText.contains("{\"tool\"");
                                    
                                    if (hasToolsKeyword || hasJsonBlock) {
                                        isToolCallDetected = true;
                                        isFinalAnswerStreaming = false;
                                    }
                                }
                                
                                // 只有确定不是工具调用时才流式输出
                                if (!isToolCallDetected && !isFinalAnswerStreaming) {
                                    // 等待足够的内容来判断
                                    if (currentText.length() > 50 && !currentText.contains("{") && !currentText.contains("json")) {
                                        isFinalAnswerStreaming = true;
                                    }
                                }

                                if (isFinalAnswerStreaming) {
                                    callback.onToken(token);
                                }
                            }
                        }
                        
                        @Override
                        public void onComplete() {
                            synchronized (lock) {
                                completed[0] = true;
                                lock.notifyAll();
                            }
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            synchronized (lock) {
                                error[0] = e;
                                completed[0] = true;
                                lock.notifyAll();
                            }
                        }
                    });
                    
                    // 等待LLM完成
                    synchronized (lock) {
                        while (!completed[0]) {
                            try {
                                lock.wait();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                callback.onError(e);
                                return;
                            }
                        }
                    }
                    
                    if (error[0] != null) {
                        callback.onError(error[0]);
                        return;
                    }
                    
                    String response = llmResponse.toString().trim();
                    Log.d(TAG, "LLM响应: " + response);
                    
                    // 优先检查新格式的多工具调用 {"tools": [...]}
                    if (config.isEnableToolUse() && (response.contains("\"tools\"") || MULTI_TOOL_PATTERN.matcher(response).find())) {
                        try {
                            // 提取JSON部分
                            String jsonStr = extractJsonFromResponse(response);
                            if (jsonStr != null) {
                                Log.d(TAG, "检测到多工具调用JSON: " + jsonStr);
                                
                                JSONObject json = new JSONObject(jsonStr);
                                JSONArray toolsArray = json.getJSONArray("tools");
                                
                                // 存储结果
                                List<String> toolResults = new ArrayList<>();
                                String chatResponse = null;
                                
                                // 执行每个工具
                                for (int i = 0; i < toolsArray.length(); i++) {
                                    JSONObject toolCall = toolsArray.getJSONObject(i);
                                    String toolName = toolCall.getString("name");
                                    JSONObject parameters = toolCall.getJSONObject("parameters");
                                    
                                    Log.d(TAG, "执行工具 " + (i+1) + "/" + toolsArray.length() + ": " + toolName);
                                    callback.onToolCall(toolName, parameters.toString());
                                    
                                    Tool tool = toolRegistry.getTool(toolName);
                                    if (tool != null) {
                                        Tool.ToolResult result = tool.execute(parameters);
                                        if (result.isSuccess()) {
                                            Log.d(TAG, "工具 " + toolName + " 执行成功");
                                            callback.onToolResult(toolName, result.getResult());
                                            
                                            if ("local_chat".equals(toolName)) {
                                                chatResponse = result.getResult();
                                            } else {
                                                toolResults.add("\n" + getToolDisplayName(toolName) + "：\n\n" + result.getResult());
                                            }
                                        } else {
                                            Log.e(TAG, "工具 " + toolName + " 执行失败: " + result.getError());
                                        }
                                    } else {
                                        Log.w(TAG, "工具不存在: " + toolName);
                                    }
                                }
                                
                                // 整合结果
                                StringBuilder finalResponseBuilder = new StringBuilder();
                                if (chatResponse != null && !chatResponse.trim().isEmpty()) {
                                    finalResponseBuilder.append(chatResponse.trim());
                                }
                                for (String toolResult : toolResults) {
                                    finalResponseBuilder.append(toolResult);
                                }
                                
                                String finalResponseStr = finalResponseBuilder.toString();
                                if (!finalResponseStr.isEmpty()) {
                                    conversationHistory.add("助手: " + finalResponseStr);
                                    callback.onFinalResponse(finalResponseStr);
                                } else {
                                    callback.onError(new Exception("工具执行未产生有效输出"));
                                }
                                break; // 结束推理循环
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "解析多工具调用失败: " + e.getMessage(), e);
                            // 继续尝试旧格式
                        }
                    }
                    
                    // 检查旧格式的单工具调用
                    Matcher matcher = TOOL_CALL_PATTERN.matcher(response);
                    if (matcher.find() && config.isEnableToolUse()) {
                        // 提取工具调用信息
                        String toolName = matcher.group(1);
                        String parametersStr = matcher.group(2);
                        
                        Log.d(TAG, "检测到工具调用: " + toolName);
                        callback.onToolCall(toolName, parametersStr);
                        
                        // 执行工具
                        Tool tool = toolRegistry.getTool(toolName);
                        if (tool != null) {
                            try {
                                // 构建参数JSON
                                JSONObject parameters = new JSONObject("{" + parametersStr + "}");
                                
                                // 执行工具
                                Tool.ToolResult result = tool.execute(parameters);
                                
                                if (result.isSuccess()) {
                                    Log.d(TAG, "工具执行成功: " + result.getResult());
                                    callback.onToolResult(toolName, result.getResult());

                                    // [临时方案] 不再将结果发回给LLM，直接使用模板回复
                                    String finalResponseFromTool = "工具 '" + toolName + "' 执行完成，结果如下：\n\n" + result.getResult();
                                    conversationHistory.add("助手: " + finalResponseFromTool);
                                    callback.onFinalResponse(finalResponseFromTool);
                                    break; // 结束推理循环

                                } else {
                                    Log.e(TAG, "工具执行失败: " + result.getError());
                                    callback.onToolResult(toolName, "执行失败: " + result.getError());
                                    
                                    // 工具失败，让LLM知道
                                    currentThought = "工具执行失败: " + result.getError() + "，请尝试其他方法。";
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "工具执行异常", e);
                                callback.onError(e);
                                currentThought = "工具执行异常: " + e.getMessage();
                            }
                        } else {
                            Log.w(TAG, "工具不存在: " + toolName);
                            currentThought = "工具 " + toolName + " 不存在，请使用其他可用工具。";
                        }
                    } else {
                        // 没有工具调用，这是最终回复
                        finalResponse.append(response);
                        conversationHistory.add("助手: " + response);
                        
                        // 清理并输出最终回复
                        String cleanedResponse = cleanFinalResponse(response);
                        
                        // 增加对无效回复的检查
                        if (isValidResponse(cleanedResponse)) {
                            finalResponse.append(cleanedResponse);
                            conversationHistory.add("助手: " + cleanedResponse);
                            callback.onFinalResponse(cleanedResponse);
                        } else {
                            Log.w(TAG, "检测到无效回复，已过滤: " + response);
                            callback.onError(new Exception("AI未能生成有效回复，请重试"));
                        }
                        break;
                    }
                }
                
                // 检查是否达到最大迭代次数（并且没有超时）
                if (!timedOut && iteration >= config.getMaxIterations()) {
                    Log.w(TAG, "达到最大迭代次数");
                    String maxIterationsMsg = "AI进行了多次思考仍未得出结论，请尝试换个问题或简化描述。";
                    // 如果已经有部分回复，则附加上
                    if (finalResponse.length() > 0) {
                        maxIterationsMsg = finalResponse.toString() + "\n\n(AI已达到最大思考次数)";
                    }
                    callback.onFinalResponse(maxIterationsMsg);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Agent执行异常", e);
                callback.onError(e);
            }
        }).start();
    }
    
    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        // AgentManager中的getSystemPrompt()现在包含了所有核心指令
        sb.append(config.getSystemPrompt()).append("\n\n");
        
        // 只附加工具列表描述
        if (config.isEnableToolUse() && toolRegistry.getToolCount() > 0) {
            sb.append("【可用工具列表】\n");
            sb.append(toolRegistry.generateToolsDescription());
        }
        
        return sb.toString();
    }
    
    /**
     * 构建完整提示词
     */
    private String buildPrompt(String systemPrompt, String currentInput) {
        StringBuilder sb = new StringBuilder();
        
        // 添加系统提示词
        sb.append(systemPrompt).append("\n\n");
        
        // 添加对话历史（最近的几轮）
        if (config.isEnableMemory() && !conversationHistory.isEmpty()) {
            sb.append("对话历史:\n");
            int startIdx = Math.max(0, conversationHistory.size() - 6); // 保留最近3轮对话
            for (int i = startIdx; i < conversationHistory.size(); i++) {
                sb.append(conversationHistory.get(i)).append("\n");
            }
            sb.append("\n");
        }
        
        // 添加当前输入
        sb.append("当前任务: ").append(currentInput).append("\n\n");
        sb.append("请开始推理:");
        
        return sb.toString();
    }
    
    /**
     * 重置对话历史
     */
    public void resetHistory() {
        conversationHistory.clear();
        // LLMProvider的状态由其自身或外部管理，AgentCore只负责重置自己的历史记录。
        Log.d(TAG, "重置对话历史");
    }
    
    /**
     * 获取对话历史
     */
    public List<String> getConversationHistory() {
        return new ArrayList<>(conversationHistory);
    }

    /**
     * 清理最终回复，移除不必要的标签和前缀
     */
    private String cleanFinalResponse(String response) {
        if (response == null) {
            return "";
        }
        // 移除常见的思考/回答标签，不区分大小写，支持中文和英文冒号
        String cleaned = response.replaceAll("(?i)^\\s*(回答|思考|答案|回复|Answer|Thought|Response)[:：]\\s*", "");
        return cleaned.trim();
    }

    /**
     * 检查回复是否有效
     */
    private boolean isValidResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return false; // 空回复或只有空白
        }
        // 过滤已知的特定乱码
        if (response.trim().equalsIgnoreCase("dfor #")) {
            return false;
        }
        // 可以根据需要添加更多过滤规则
        return true;
    }
    
    /**
     * Agent回调接口
     */
    /**
     * 从响应中提取JSON字符串
     */
    private String extractJsonFromResponse(String response) {
        // 尝试提取 ```json ... ``` 块
        int jsonStart = response.indexOf("```json");
        if (jsonStart != -1) {
            int contentStart = response.indexOf("{", jsonStart);
            int contentEnd = response.lastIndexOf("}");
            if (contentStart != -1 && contentEnd > contentStart) {
                return response.substring(contentStart, contentEnd + 1);
            }
        }
        
        // 尝试直接找 {"tools": 格式
        int toolsStart = response.indexOf("{\"tools\"");
        if (toolsStart == -1) {
            toolsStart = response.indexOf("{ \"tools\"");
        }
        if (toolsStart != -1) {
            int endBrace = response.lastIndexOf("}");
            if (endBrace > toolsStart) {
                return response.substring(toolsStart, endBrace + 1);
            }
        }
        
        return null;
    }
    
    /**
     * 获取工具的显示名称
     */
    private String getToolDisplayName(String toolName) {
        switch (toolName) {
            case "psychological_assessment":
                return "状态评估结果";
            case "memory_query":
                return "历史记忆";
            case "conversation_counter":
                return "对话统计";
            default:
                return toolName;
        }
    }
    
    public interface AgentCallback {
        /**
         * 工具调用时触发
         */
        void onToolCall(String toolName, String parameters);
        
        /**
         * 工具执行结果
         */
        void onToolResult(String toolName, String result);
        
        /**
         * 流式响应，返回部分内容
         */
        void onToken(String token);
        
        /**
         * 最终回复
         */
        void onFinalResponse(String response);
        
        /**
         * 错误处理
         */
        void onError(Exception e);
    }
}

