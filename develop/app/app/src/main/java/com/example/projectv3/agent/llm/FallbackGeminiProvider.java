package com.example.projectv3.agent.llm;

import android.util.Log;

import java.util.Arrays;

/**
 * 带模型级联降级的 LLMProvider。
 * 首选 gemini-2.5-flash，失败（请求报错或未产生任何流式token）时，
 * 依次回退到 gemini-2.0-flash、gemini-2.0-flash-lite。
 */
public class FallbackGeminiProvider implements LLMProvider {
    private static final String TAG = "FallbackGeminiProvider";

    private final String apiKey;
    private final String baseUrl;
    private final String[] modelOrder;
    private float temperature = 0.7f;

    public FallbackGeminiProvider(String apiKey, String baseUrl, String[] modelOrder) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelOrder = modelOrder != null ? modelOrder.clone() : new String[]{};
        Log.d(TAG, "初始化模型回退顺序: " + Arrays.toString(this.modelOrder));
    }

    @Override
    public void generateResponse(String prompt, CompletionCallback callback) {
        if (modelOrder.length == 0) {
            callback.onError(new IllegalStateException("未配置任何模型顺序"));
            return;
        }
        attemptWithIndex(0, prompt, callback);
    }

    private void attemptWithIndex(int idx, String prompt, CompletionCallback callback) {
        if (idx >= modelOrder.length) {
            callback.onError(new IllegalStateException("所有模型均调用失败"));
            return;
        }
        final String model = modelOrder[idx];
        Log.d(TAG, "尝试调用模型: " + model + " (idx=" + idx + ")");

        GeminiApiClient client = new GeminiApiClient(apiKey, baseUrl, model);
        client.setTemperature(temperature);

        final boolean[] gotAnyToken = {false};

        client.generateResponse(prompt, new CompletionCallback() {
            @Override
            public void onToken(String token) {
                gotAnyToken[0] = true; // 一旦收到token则认为本次调用成功，不再降级
                callback.onToken(token);
            }

            @Override
            public void onComplete() {
                // 如果本模型一次 token 都没产生且不是最后一个，则降级到下一个模型
                if (!gotAnyToken[0] && idx < modelOrder.length - 1) {
                    Log.w(TAG, "模型无流式输出，降级到下一个: " + modelOrder[idx + 1]);
                    attemptWithIndex(idx + 1, prompt, callback);
                } else {
                    callback.onComplete();
                }
            }

            @Override
            public void onError(Exception e) {
                // 若还未产出任何token，尝试降级；否则认为已部分成功，直接结束
                if (!gotAnyToken[0] && idx < modelOrder.length - 1) {
                    Log.w(TAG, "模型调用失败，降级到下一个: " + modelOrder[idx + 1] + ", 错误: " + e.getMessage());
                    attemptWithIndex(idx + 1, prompt, callback);
                } else if (!gotAnyToken[0]) {
                    Log.e(TAG, "所有模型均失败，最后错误:", e);
                    callback.onError(e);
                } else {
                    Log.w(TAG, "模型在产生部分输出后出错，视为完成: " + e.getMessage());
                    callback.onComplete();
                }
            }
        });
    }

    @Override
    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
}


