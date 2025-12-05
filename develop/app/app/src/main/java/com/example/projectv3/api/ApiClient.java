package com.example.projectv3.api;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    public static final String BASE_URL = "http://47.99.47.144:8080/"; // Wi‑Fi 直连宿主机（需同一网络，Windows 防火墙放行 8080）

    private static Retrofit retrofit = null;
    private static Context appContext = null;

    /**
     * 初始化ApiClient，必须在Application中调用
     */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor);

            // 如果有应用上下文，添加JWT认证拦截器
            if (appContext != null) {
                clientBuilder.addInterceptor(new AuthInterceptor(appContext));
            }

            OkHttpClient client = clientBuilder.build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
        return retrofit;
    }

    public static UserApi getUserApi() {
        return getClient().create(UserApi.class);
    }

    public static UnifiedTestApi getUnifiedTestApi() {
        return getClient().create(UnifiedTestApi.class);
    }

    public static TestAdminApi getTestAdminApi() {
        return getClient().create(TestAdminApi.class);
    }
}