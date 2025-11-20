package com.example.projectv3.agent.llm;

/**
 * 通用语言模型提供者接口
 * 定义了Agent与任何语言模型（本地或云端）交互的标准方式。
 */
public interface LLMProvider {

    /**
     * 用于处理模型生成响应的回调接口。
     * 支持流式和一次性返回。
     */
    interface CompletionCallback {
        /**
         * 当模型生成一个新的token时调用（用于流式输出）。
         * @param token 新生成的文本片段。
         */
        void onToken(String token);

        /**
         * 当模型完成所有响应生成时调用。
         */
        void onComplete();

        /**
         * 当生成过程中发生错误时调用。
         * @param e 发生的异常。
         */
        void onError(Exception e);
    }

    /**
     * 向语言模型发送一个提示词并获取响应。
     * @param prompt 发送给模型的完整提示词。
     * @param callback 用于处理响应的回调。
     */
    void generateResponse(String prompt, CompletionCallback callback);

    /**
     * 设置模型的生成温度（temperature）。
     * @param temperature 温度值，通常在0.0到1.0之间。
     */
    void setTemperature(float temperature);
}

