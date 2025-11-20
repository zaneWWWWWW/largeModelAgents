package com.example.projectv3.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * JWT认证拦截器
 * 自动在所有API请求中添加JWT令牌
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String requestUrl = originalRequest.url().toString();

        // 获取存储的JWT令牌
        SharedPreferences prefs = context.getSharedPreferences("user_info", Context.MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token == null) {
            Log.w(TAG, "当前无可用token，直接请求: " + requestUrl);
        } else {
            Log.d(TAG, "检测到token，长度=" + token.length());
        }

        // 如果没有token或者是登录/注册请求，直接执行原始请求
        if (token == null || isAuthRequest(requestUrl)) {
            if (isAuthRequest(requestUrl)) {
                Log.d(TAG, "认证相关请求，不附加Authorization: " + requestUrl);
            }
            return chain.proceed(originalRequest);
        }

        // 添加Authorization头部
        String shortToken = token.length() > 12 ? token.substring(0, 12) + "..." : token;
        Log.d(TAG, "为请求附加Authorization，token前缀=" + shortToken + " url=" + requestUrl);

        Request authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer " + token)
                .build();

        return chain.proceed(authenticatedRequest);
    }

    /**
     * 判断是否为认证相关的请求（登录、注册）
     */
    private boolean isAuthRequest(String url) {
        if (TextUtils.isEmpty(url)) {
            return false;
        }
        return url.contains("/api/user/login") ||
               url.contains("/api/user/register");
    }
}