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
    private final com.example.projectv3.db.ChatDbHelper dbHelper;
    private final long userId;
    
    public PsychologicalStatusManager(Context context) {
        this.context = context;
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences userPrefs = context.getSharedPreferences("user_info", Context.MODE_PRIVATE);
        long uid = userPrefs.getLong("user_id", -1);
        this.userId = uid;
        if (uid > 0) {
            this.dbHelper = new com.example.projectv3.db.ChatDbHelper(context, uid);
        } else {
            this.dbHelper = new com.example.projectv3.db.ChatDbHelper(context);
        }
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
        String toSave = null;
        try {
            new JSONObject(resultJson);
            toSave = resultJson;
        } catch (JSONException e1) {
            try {
                int startIndex = resultJson.indexOf('{');
                int endIndex = resultJson.indexOf('}', startIndex) + 1;
                if (startIndex >= 0 && endIndex > startIndex) {
                    String jsonStr = resultJson.substring(startIndex, endIndex);
                    new JSONObject(jsonStr);
                    toSave = jsonStr;
                }
            } catch (Exception ignore) {}
            if (toSave == null) {
                try {
                    java.util.regex.Pattern depressionPattern = java.util.regex.Pattern.compile("\"depression_level\"\\s*:\\s*(\\d)");
                    java.util.regex.Pattern anxietyPattern = java.util.regex.Pattern.compile("\"anxiety_level\"\\s*:\\s*(\\d)");
                    java.util.regex.Pattern riskPattern = java.util.regex.Pattern.compile("\"risk_flag\"\\s*:\\s*\"(\\w+)\"");
                    java.util.regex.Pattern distressPattern = java.util.regex.Pattern.compile("\"student_distress_score\"\\s*:\\s*(\\d)");
                    java.util.regex.Matcher dm = depressionPattern.matcher(resultJson);
                    java.util.regex.Matcher am = anxietyPattern.matcher(resultJson);
                    java.util.regex.Matcher rm = riskPattern.matcher(resultJson);
                    java.util.regex.Matcher sm = distressPattern.matcher(resultJson);
                    int d = dm.find() ? Integer.parseInt(dm.group(1)) : 0;
                    int a = am.find() ? Integer.parseInt(am.group(1)) : 0;
                    String r = rm.find() ? rm.group(1) : "none";
                    int s = sm.find() ? Integer.parseInt(sm.group(1)) : 0;
                    JSONObject obj = new JSONObject();
                    obj.put("depression_level", d);
                    obj.put("anxiety_level", a);
                    obj.put("risk_flag", r);
                    obj.put("student_distress_score", s);
                    toSave = obj.toString();
                } catch (Exception e2) {
                    Log.e(TAG, "无法从结果中构建JSON: " + resultJson, e2);
                    return false;
                }
            }
        }
        long ts = System.currentTimeMillis();
        long id = dbHelper.insertPsychStatus(toSave, ts);
        boolean success = id > 0;
        if (success) {
            Log.d(TAG, "心理状态评估结果已保存");
        } else {
            Log.e(TAG, "保存心理状态评估结果失败");
        }
        return success;
    }
    
    /**
     * 获取所有心理状态评估结果历史
     * @return 评估结果历史的JSON字符串
     */
    public String getStatusHistory() {
        try {
            java.util.List<java.util.Map<String, Object>> history = dbHelper.getPsychStatusHistory();
            JSONArray arr = new JSONArray();
            for (java.util.Map<String, Object> record : history) {
                JSONObject obj = new JSONObject();
                Object tso = record.get("timestamp");
                long ts = tso instanceof Long ? (Long) tso : 0L;
                String tsStr = formatDate(ts);
                obj.put("timestamp", tsStr);
                obj.put("result", String.valueOf(record.get("result")));
                arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) {
            Log.e(TAG, "构建历史记录JSON失败", e);
            return "[]";
        }
    }
    
    /**
     * 获取所有心理状态评估结果历史列表（静态方法）
     * @param context 上下文
     * @return 评估结果历史的列表，每个元素是一个Map，包含timestamp和result
     */
    public static java.util.List<java.util.Map<String, Object>> getStatusHistoryList(Context context) {
        PsychologicalStatusManager manager = new PsychologicalStatusManager(context);
        java.util.List<java.util.Map<String, Object>> raw = manager.dbHelper.getPsychStatusHistory();
        java.util.List<java.util.Map<String, Object>> historyList = new java.util.ArrayList<>();
        for (java.util.Map<String, Object> record : raw) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            Object tso = record.get("timestamp");
            long ts = tso instanceof Long ? (Long) tso : 0L;
            map.put("timestamp", manager.formatDate(ts));
            map.put("result", String.valueOf(record.get("result")));
            historyList.add(map);
        }
        return historyList;
    }
    
    /**
     * 获取心理状态评估结果历史JSONArray对象
     * @return 评估结果历史的JSONArray对象
     */
    private JSONArray getStatusHistoryArray() {
        try {
            String json = getStatusHistory();
            return new JSONArray(json);
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
        int rows = dbHelper.deleteAllPsychStatus();
        boolean success = rows >= 0;
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

    private String formatDate(long ts) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(new Date(ts));
    }
}
