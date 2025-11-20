package com.example.projectv3.agent.llm;

import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 通过API调用Gemini模型的LLMProvider实现。
 * 负责与远程模型服务进行通信。
 */
public class GeminiApiClient implements LLMProvider {

    private static final String TAG = "GeminiApiClient";
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String apiKey;
    private final String baseUrl;
    private final String modelName;
    private float temperature = 0.7f;

    /**
     * 构造函数
     * @param apiKey 你的API Key
     * @param baseUrl API的Base URL
     * @param modelName 要使用的模型名称
     */
    public GeminiApiClient(String apiKey, String baseUrl, String modelName) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
    }

    @Override
    public void generateResponse(String prompt, CompletionCallback callback) {
        try {
            // 1. 构建请求体
            JSONObject jsonBody = new JSONObject();
            JSONArray messages = new JSONArray();
            JSONObject userMessage = new JSONObject();

            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            jsonBody.put("model", this.modelName);
            jsonBody.put("messages", messages);
            jsonBody.put("temperature", this.temperature);
            jsonBody.put("stream", true);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

            // 2. 构建请求
            Request request = new Request.Builder()
                    .url(this.baseUrl)
                    .addHeader("Authorization", "Bearer " + this.apiKey)
                    .post(body)
                    .build();

            // 3. 异步执行请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    Log.e(TAG, "API call failed", e);
                    callback.onError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    if (!response.isSuccessful()) {
                        try {
                            String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                            Log.e(TAG, "API call unsuccessful: " + response.code() + " - " + errorBody);
                            callback.onError(new IOException("Unexpected code " + response + "\nBody: " + errorBody));
                        } catch (IOException e) {
                            Log.e(TAG, "Failed to read error body", e);
                            callback.onError(new IOException("Unexpected code " + response));
                        }
                        return;
                    }

                    try (ResponseBody responseBody = response.body()) {
                        if (responseBody == null) {
                            callback.onError(new IOException("Response body is null"));
                            return;
                        }

                        okio.BufferedSource source = responseBody.source();
                        while (!source.exhausted()) {
                            String line = source.readUtf8Line();
                            if (line == null || !line.startsWith("data: ")) {
                                continue;
                            }

                            String jsonData = line.substring(6).trim();
                            if ("[DONE]".equalsIgnoreCase(jsonData)) {
                                break;
                            }

                            try {
                                JSONObject responseObject = new JSONObject(jsonData);
                                String token = responseObject.getJSONArray("choices")
                                        .getJSONObject(0)
                                        .getJSONObject("delta")
                                        .optString("content", "");

                                if (token != null && !token.isEmpty()) {
                                    callback.onToken(token);
                                }
                            } catch (JSONException e) {
                                Log.w(TAG, "Could not parse streaming JSON chunk: " + jsonData, e);
                            }
                        }
                        callback.onComplete();

                    } catch (Exception e) {
                        Log.e(TAG, "Error while reading streaming response", e);
                        callback.onError(e);
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to build API request", e);
            callback.onError(e);
        }
    }

    @Override
    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
}

