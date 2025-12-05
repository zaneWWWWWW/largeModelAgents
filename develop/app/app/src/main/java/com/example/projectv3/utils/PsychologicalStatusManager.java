package com.example.projectv3.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 心理状态评估结果管理工具类
 * 负责存储和管理用户的心理状态评估结果
 */
public class PsychologicalStatusManager {
    private static final String TAG = "PsychologicalStatusManager";
    private static final String PREF_NAME = "psychological_status";
    private static final String KEY_STATUS_HISTORY = "status_history";
    
    private final Context context;
    private final SharedPreferences preferences;
    
    public PsychologicalStatusManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 保存心理状态评估结果
     * @param result 评估结果JSON字符串
     * @return 是否保存成功
     */
    public boolean saveStatusResult(String resultJson) {
        if (resultJson == null || resultJson.trim().isEmpty()) {
            Log.w(TAG, "尝试保存空的评估结果，已忽略");
            return false;
        }

        try {
            // 验证resultJson是否为有效的JSON
            new JSONObject(resultJson);

            JSONArray historyArray = getStatusHistoryArray();
            
            // 创建新的评估记录
            JSONObject newRecord = new JSONObject();
            newRecord.put("timestamp", getCurrentDateString());
            newRecord.put("result", resultJson); // 直接保存原始的JSON结果
            
            // 将新记录添加到历史数组
            historyArray.put(newRecord);
            
            // 保存更新后的历史记录
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_STATUS_HISTORY, historyArray.toString());
            boolean success = editor.commit();
            
            if (success) {
                Log.d(TAG, "心理状态评估结果已保存: " + newRecord.toString());
            } else {
                Log.e(TAG, "保存心理状态评估结果失败");
            }
            return success;

        } catch (JSONException e) {
            Log.e(TAG, "保存心理状态评估结果时出错，无效的JSON格式: " + resultJson, e);
            return false;
        }
    }
    
    /**
     * 获取所有心理状态评估结果历史
     * @return 评估结果历史的JSON字符串
     */
    public String getStatusHistory() {
        return preferences.getString(KEY_STATUS_HISTORY, "[]");
    }
    
    /**
     * 获取所有心理状态评估结果历史列表（静态方法）
     * @param context 上下文
     * @return 评估结果历史的列表，每个元素是一个Map，包含timestamp和result
     */
    public static java.util.List<java.util.Map<String, Object>> getStatusHistoryList(Context context) {
        java.util.List<java.util.Map<String, Object>> historyList = new java.util.ArrayList<>();
        try {
            SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String historyStr = preferences.getString(KEY_STATUS_HISTORY, "[]");
            JSONArray historyArray = new JSONArray(historyStr);
            
            // 从最近的开始遍历，这样最新的记录在列表最前面
            for (int i = historyArray.length() - 1; i >= 0; i--) {
                JSONObject record = historyArray.getJSONObject(i);
                java.util.Map<String, Object> recordMap = new java.util.HashMap<>();
                
                // 检查是新格式还是旧格式，以实现向后兼容
                if (record.has("result")) {
                    // 新格式: { "timestamp": "...", "result": "{...}" }
                    recordMap.put("timestamp", record.getString("timestamp"));
                    recordMap.put("result", record.getString("result"));
                } else if (record.has("depression")) {
                    // 旧格式: { "depression": 1, "anxiety": 2, "date": "..." }
                    // 为了兼容，手动重建一个result JSON字符串
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("depression", record.getInt("depression"));
                    resultJson.put("anxiety", record.getInt("anxiety"));
                    
                    recordMap.put("timestamp", record.getString("date"));
                    recordMap.put("result", resultJson.toString());
            }
            
                if (!recordMap.isEmpty()) {
                    historyList.add(recordMap);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "获取心理状态评估历史列表失败", e);
        }
        return historyList;
    }
    
    /**
     * 获取心理状态评估结果历史JSONArray对象
     * @return 评估结果历史的JSONArray对象
     */
    private JSONArray getStatusHistoryArray() {
        try {
            String historyStr = preferences.getString(KEY_STATUS_HISTORY, "[]");
            return new JSONArray(historyStr);
        } catch (JSONException e) {
            Log.e(TAG, "解析心理状态评估历史记录失败", e);
            return new JSONArray();
        }
    }
    
    /**
     * 清除所有心理状态评估结果历史
     * @return 是否清除成功
     */
    public boolean clearStatusHistory() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_STATUS_HISTORY);
        boolean success = editor.commit();
        
        if (success) {
            Log.d(TAG, "心理状态评估历史记录已清除");
        } else {
            Log.e(TAG, "清除心理状态评估历史记录失败");
        }
        
        return success;
    }
    
    /**
     * 获取当前日期字符串
     * @return 格式化的日期字符串
     */
    private String getCurrentDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date());
    }
}