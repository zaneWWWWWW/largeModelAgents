package com.example.projectv3.agent.tools;

import android.content.Context;
import android.util.Log;

import com.example.projectv3.agent.Tool;
import com.example.projectv3.service.PsychologicalStatusService;

import org.json.JSONObject;

/**
 * 心理状态评估工具
 * 基于对话历史评估用户的心理状态
 */
public class PsychologicalAssessmentTool implements Tool {
    private static final String TAG = "PsychAssessmentTool";
    
    private final Context context;
    private final PsychologicalStatusService statusService;
    
    public PsychologicalAssessmentTool(Context context) {
        this.context = context;
        this.statusService = new PsychologicalStatusService(context);
    }
    
    @Override
    public String getName() {
        return "psychological_assessment";
    }
    
    @Override
    public String getDescription() {
        return "评估用户当前的心理状态，包括抑郁程度、焦虑程度、风险标记和困扰分数。" +
               "当用户表达情绪困扰、心理问题或需要评估时使用此工具。";
    }
    
    @Override
    public String getParametersSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"trigger_reason\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"触发评估的原因，例如：用户表达焦虑、抑郁情绪等\"\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"trigger_reason\"]\n" +
               "}";
    }
    
    @Override
    public ToolResult execute(JSONObject parameters) {
        try {
            String triggerReason = parameters.optString("trigger_reason", "用户请求评估");
            Log.d(TAG, "执行心理评估，触发原因: " + triggerReason);
            
            // 使用同步方式执行评估
            final String[] result = {null};
            final Exception[] error = {null};
            final Object lock = new Object();
            final boolean[] completed = {false};
            
            statusService.analyzeUserPsychologicalStatus(new PsychologicalStatusService.AnalysisCallback() {
                @Override
                public void onSuccess(String analysisResult) {
                    synchronized (lock) {
                        result[0] = analysisResult;
                        completed[0] = true;
                        lock.notifyAll();
                    }
                }
                
                @Override
                public void onError(Exception e) {
                    synchronized (lock) {
                        error[0] = e;
                        completed[0] = true;
                        lock.notifyAll();
                    }
                }
            });
            
            // 等待评估完成（最多30秒）
            synchronized (lock) {
                long startTime = System.currentTimeMillis();
                while (!completed[0]) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    if (elapsed > 30000) {
                        return ToolResult.error("心理评估超时");
                    }
                    try {
                        lock.wait(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return ToolResult.error("心理评估被中断");
                    }
                }
            }
            
            if (error[0] != null) {
                Log.e(TAG, "心理评估失败", error[0]);
                return ToolResult.error("心理评估失败: " + error[0].getMessage());
            }
            
            if (result[0] != null) {
                Log.d(TAG, "心理评估完成: " + result[0]);
                
                // 解析评估结果
                String formattedResult = formatAssessmentResult(result[0]);
                return ToolResult.success(formattedResult);
            } else {
                return ToolResult.error("心理评估返回空结果");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "执行心理评估工具异常", e);
            return ToolResult.error("执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 格式化评估结果
     */
    private String formatAssessmentResult(String rawResult) {
        try {
            // 尝试解析JSON格式的评估结果
            if (rawResult.contains("depression_level")) {
                // 提取JSON部分
                int startIndex = rawResult.indexOf('{');
                int endIndex = rawResult.indexOf('}', startIndex) + 1;
                
                if (startIndex >= 0 && endIndex > startIndex) {
                    String jsonStr = rawResult.substring(startIndex, endIndex);
                    JSONObject json = new JSONObject(jsonStr);
                    
                    int depressionLevel = json.optInt("depression_level", 0);
                    int anxietyLevel = json.optInt("anxiety_level", 0);
                    String riskFlag = json.optString("risk_flag", "none");
                    int distressScore = json.optInt("student_distress_score", 0);
                    
                    // 转换为可读格式
                    String depressionDesc = getDepressionDescription(depressionLevel);
                    String anxietyDesc = getAnxietyDescription(anxietyLevel);
                    String riskDesc = getRiskDescription(riskFlag);
                    String distressDesc = getDistressDescription(distressScore);
                    
                    return String.format(
                        "心理状态评估结果:\n" +
                        "- 抑郁程度: %s (级别%d)\n" +
                        "- 焦虑程度: %s (级别%d)\n" +
                        "- 风险标记: %s\n" +
                        "- 困扰分数: %d分 (%s)",
                        depressionDesc, depressionLevel,
                        anxietyDesc, anxietyLevel,
                        riskDesc,
                        distressScore, distressDesc
                    );
                }
            }
            
            // 如果解析失败，返回原始结果
            return "心理状态评估结果: " + rawResult;
            
        } catch (Exception e) {
            Log.e(TAG, "格式化评估结果失败", e);
            return "心理状态评估结果: " + rawResult;
        }
    }
    
    private String getDepressionDescription(int level) {
        switch (level) {
            case 0: return "无明显抑郁";
            case 1: return "轻度抑郁";
            case 2: return "中度抑郁";
            case 3: return "重度抑郁";
            default: return "未知";
        }
    }
    
    private String getAnxietyDescription(int level) {
        switch (level) {
            case 0: return "无明显焦虑";
            case 1: return "轻度焦虑";
            case 2: return "中度焦虑";
            case 3: return "重度焦虑";
            default: return "未知";
        }
    }
    
    private String getRiskDescription(String flag) {
        switch (flag) {
            case "none": return "无风险";
            case "suicidal": return "自杀风险";
            case "self_harm": return "自伤风险";
            case "violence": return "暴力风险";
            default: return "未知风险";
        }
    }
    
    private String getDistressDescription(int score) {
        if (score >= 0 && score <= 3) {
            return "轻度困扰";
        } else if (score >= 4 && score <= 6) {
            return "中度困扰";
        } else if (score >= 7 && score <= 9) {
            return "重度困扰";
        } else {
            return "未知";
        }
    }
}

