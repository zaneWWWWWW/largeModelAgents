package com.example.projectv3;

import android.app.Application;
import com.example.projectv3.api.ApiClient;

public class ProjectApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 初始化ApiClient
        ApiClient.init(this);
    }
} 