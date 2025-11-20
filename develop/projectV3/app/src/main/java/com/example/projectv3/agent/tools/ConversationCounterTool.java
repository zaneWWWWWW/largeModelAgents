package com.example.projectv3.agent.tools;

import android.content.Context;
import android.util.Log;

import com.example.projectv3.agent.Tool;
import com.example.projectv3.utils.ConversationCounter;

import org.json.JSONObject;

/**
 * 对话计数工具
 * 用于查询和管理对话计数器状态
 */
public class ConversationCounterTool implements Tool {
    private static final String TAG = "ConversationCounterTool";
    
    private final Context context;
    private final ConversationCounter counter;
    
    public ConversationCounterTool(Context context) {
        this.context = context;
        this.counter = new ConversationCounter(context);
    }
    
    @Override
    public String getName() {
        return "conversation_counter";
    }
    
    @Override
    public String getDescription() {
        return "查询当前对话计数状态，了解距离下次心理评估还需要多少轮对话。" +
               "可以帮助用户了解评估频率和当前进度。";
    }
    
    @Override
    public String getParametersSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"action\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"enum\": [\"query\", \"reset\"],\n" +
               "      \"description\": \"操作类型: query-查询状态, reset-重置计数\",\n" +
               "      \"default\": \"query\"\n" +
               "    }\n" +
               "  }\n" +
               "}";
    }
    
    @Override
    public ToolResult execute(JSONObject parameters) {
        try {
            String action = parameters.optString("action", "query");
            
            Log.d(TAG, "执行对话计数工具，操作: " + action);
            
            if ("reset".equals(action)) {
                counter.resetCount();
                return ToolResult.success("对话计数已重置为0");
            } else {
                // 查询状态
                int currentCount = counter.getCurrentCount();
                int remaining = counter.getRemainingCountForNextAnalysis();
                boolean shouldAnalyze = counter.shouldPerformAnalysis();
                
                StringBuilder result = new StringBuilder();
                result.append("对话计数状态:\n");
                result.append("- 当前对话轮数: ").append(currentCount).append("\n");
                result.append("- 距离下次评估: ").append(remaining).append("轮对话\n");
                
                if (shouldAnalyze) {
                    result.append("- 状态: 已达到评估条件，可以进行心理状态评估");
                } else {
                    result.append("- 状态: 继续对话中");
                }
                
                return ToolResult.success(result.toString());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "执行对话计数工具异常", e);
            return ToolResult.error("执行异常: " + e.getMessage());
        }
    }
}

