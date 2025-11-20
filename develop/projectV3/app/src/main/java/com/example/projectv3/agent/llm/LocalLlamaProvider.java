package com.example.projectv3.agent.llm;

import com.example.projectv3.LLamaAPI;

/**
 * 本地LLaMA模型的LLMProvider实现。
 * 这是一个适配器，将现有的LLamaAPI包装成符合LLMProvider接口的形式。
 */
public class LocalLlamaProvider implements LLMProvider {

    private final LLamaAPI llamaApi;

    public LocalLlamaProvider(LLamaAPI llamaApi) {
        this.llamaApi = llamaApi;
    }

    @Override
    public void generateResponse(String prompt, CompletionCallback callback) {
        // 将LLMProvider的回调转换为LLamaAPI的回调
        llamaApi.chat(prompt, new LLamaAPI.CompletionCallback() {
            private final StringBuilder responseBuilder = new StringBuilder();

            @Override
            public void onToken(String token) {
                if (token != null) {
                    responseBuilder.append(token);
                    // 对于本地模型，我们也可以选择流式返回
                    callback.onToken(token);
                }
            }

            @Override
            public void onComplete() {
                callback.onComplete();
            }

            @Override
            public void onError(Exception e) {
                callback.onError(e);
            }
        });
    }

    @Override
    public void setTemperature(float temperature) {
        llamaApi.setTemperature(temperature);
    }
    
    /**
     * 检查模型是否已加载
     */
    public boolean isModelLoaded() { 
        return llamaApi != null && llamaApi.isModelLoaded();
    }
}

