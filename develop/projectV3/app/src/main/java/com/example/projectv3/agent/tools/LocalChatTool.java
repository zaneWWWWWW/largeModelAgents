package com.example.projectv3.agent.tools;

import android.util.Log;

import com.example.projectv3.agent.Tool;
import com.example.projectv3.agent.llm.LLMProvider;
import com.example.projectv3.agent.llm.LocalLlamaProvider;

import org.json.JSONObject;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 本地聊天工具
 * 将本地的LLaMA模型封装成一个工具，供Agent在需要时调用。
 * 这对于处理简单、快速、需要保护隐私的对话非常有用。
 */
public class LocalChatTool implements Tool {
    private static final String TAG = "LocalChatTool";

    private final LocalLlamaProvider localLlmProvider;

    public LocalChatTool(LocalLlamaProvider localLlmProvider) {
        this.localLlmProvider = localLlmProvider;
    }

    @Override
    public String getName() {
        return "local_chat";
    }

    @Override
    public String getDescription() {
        return "与用户进行一次基础的、快速的对话。当用户的意图只是简单的聊天、问候或不需要复杂推理时，使用此工具。";
    }

    @Override
    public String getParametersSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"user_input\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"用户的原始输入内容。\"\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"user_input\"]\n" +
               "}";
    }

    @Override
    public ToolResult execute(JSONObject parameters) {
        if (localLlmProvider == null || !localLlmProvider.isModelLoaded()) {
            return ToolResult.error("本地聊天模型未加载，无法使用此工具。");
        }

        try {
            String userInput = parameters.getString("user_input");
            Log.d(TAG, "Executing local chat with input: " + userInput);

            final StringBuilder responseBuilder = new StringBuilder();
            final CountDownLatch latch = new CountDownLatch(1);
            final Exception[] error = {null};

            // 使用本地模型生成回复
            localLlmProvider.generateResponse(userInput, new LLMProvider.CompletionCallback() {
                @Override
                public void onToken(String token) {
                    if (token != null) {
                        responseBuilder.append(token);
                    }
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    error[0] = e;
                    latch.countDown();
                }
            });

            // 等待本地模型响应，设置超时
            boolean completed = latch.await(20, TimeUnit.SECONDS);

            if (!completed) {
                return ToolResult.error("本地聊天工具响应超时。");
            }

            if (error[0] != null) {
                return ToolResult.error("本地聊天工具执行失败: " + error[0].getMessage());
            }

            String finalResponse = responseBuilder.toString().trim();
            if (finalResponse.isEmpty()) {
                return ToolResult.error("本地聊天工具生成了空回复。");
            }

            Log.d(TAG, "Local chat response: " + finalResponse);
            return ToolResult.success(finalResponse);

        } catch (Exception e) {
            Log.e(TAG, "Error executing LocalChatTool", e);
            return ToolResult.error("执行本地聊天工具时发生异常: " + e.getMessage());
        }
    }
}

