package com.example.projectv3.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

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
                    .baseUrl(resolveBaseUrl())
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

    public static String getBaseUrl() {
        return resolveBaseUrl();
    }

    private static String resolveBaseUrl() {
        String preferred = null;
        if (appContext != null) {
            SharedPreferences prefs = appContext.getSharedPreferences("server_config", Context.MODE_PRIVATE);
            preferred = prefs.getString("base_url", null);
        }

        if (preferred != null && preferred.startsWith("http")) {
            return normalizeBaseUrl(preferred);
        }

        if (isGenymotion()) {
            return "http://10.0.3.2:8080/";
        }
        if (isEmulator()) {
            return "http://10.0.2.2:8080/";
        }
        return "http://192.168.3.46:8080/";
    }

    private static String normalizeBaseUrl(String url) {
        return url.endsWith("/") ? url : (url + "/");
    }

    private static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("emulator")
                || Build.BRAND.contains("google_sdk")
                || Build.DEVICE.contains("generic");
    }

    private static boolean isGenymotion() {
        return Build.MANUFACTURER != null && Build.MANUFACTURER.toLowerCase().contains("genymotion");
    }
}
