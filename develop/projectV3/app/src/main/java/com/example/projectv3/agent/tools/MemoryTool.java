package com.example.projectv3.agent.tools;

import android.content.Context;
import android.util.Log;

import com.example.projectv3.agent.Tool;
import com.example.projectv3.db.ChatDbHelper;
import com.example.projectv3.model.Message;

import org.json.JSONObject;

import java.util.List;

/**
 * 记忆管理工具
 * 用于查询和管理对话历史记忆
 */
public class MemoryTool implements Tool {
    private static final String TAG = "MemoryTool";
    
    private final Context context;
    private final ChatDbHelper dbHelper;
    
    public MemoryTool(Context context) {
        this.context = context;
        this.dbHelper = new ChatDbHelper(context);
    }
    
    @Override
    public String getName() {
        return "memory_query";
    }
    
    @Override
    public String getDescription() {
        return "查询历史对话记忆，可以检索用户之前提到的信息、话题或情绪状态。" +
               "当需要回顾之前的对话内容或了解用户历史情况时使用此工具。";
    }
    
    @Override
    public String getParametersSchema() {
        return "{\n" +
               "  \"type\": \"object\",\n" +
               "  \"properties\": {\n" +
               "    \"query_type\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"enum\": [\"recent\", \"keyword\", \"summary\"],\n" +
               "      \"description\": \"查询类型: recent-最近对话, keyword-关键词搜索, summary-对话摘要\"\n" +
               "    },\n" +
               "    \"keyword\": {\n" +
               "      \"type\": \"string\",\n" +
               "      \"description\": \"搜索关键词（当query_type为keyword时必需）\"\n" +
               "    },\n" +
               "    \"limit\": {\n" +
               "      \"type\": \"integer\",\n" +
               "      \"description\": \"返回的消息数量限制，默认10条\",\n" +
               "      \"default\": 10\n" +
               "    }\n" +
               "  },\n" +
               "  \"required\": [\"query_type\"]\n" +
               "}";
    }
    
    @Override
    public ToolResult execute(JSONObject parameters) {
        try {
            String queryType = parameters.optString("query_type", "recent");
            String keyword = parameters.optString("keyword", "");
            int limit = parameters.optInt("limit", 10);
            
            Log.d(TAG, "执行记忆查询，类型: " + queryType + ", 关键词: " + keyword);
            
            String result;
            switch (queryType) {
                case "recent":
                    result = queryRecentMessages(limit);
                    break;
                case "keyword":
                    if (keyword.isEmpty()) {
                        return ToolResult.error("关键词搜索需要提供keyword参数");
                    }
                    result = queryByKeyword(keyword, limit);
                    break;
                case "summary":
                    result = generateSummary(limit);
                    break;
                default:
                    return ToolResult.error("不支持的查询类型: " + queryType);
            }
            
            return ToolResult.success(result);
            
        } catch (Exception e) {
            Log.e(TAG, "执行记忆查询工具异常", e);
            return ToolResult.error("执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 查询最近的对话
     */
    private String queryRecentMessages(int limit) {
        List<Message> messages = dbHelper.getAllMessages();
        
        if (messages.isEmpty()) {
            return "暂无历史对话记录";
        }
        
        // 获取最近的消息
        int startIdx = Math.max(0, messages.size() - limit);
        List<Message> recentMessages = messages.subList(startIdx, messages.size());
        
        StringBuilder sb = new StringBuilder();
        sb.append("最近").append(recentMessages.size()).append("条对话:\n\n");
        
        for (Message msg : recentMessages) {
            String role = msg.isAi() ? "助手" : "用户";
            sb.append(role).append(": ").append(msg.getContent()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 根据关键词搜索对话
     */
    private String queryByKeyword(String keyword, int limit) {
        List<Message> allMessages = dbHelper.getAllMessages();
        
        if (allMessages.isEmpty()) {
            return "暂无历史对话记录";
        }
        
        // 简单的关键词匹配
        StringBuilder sb = new StringBuilder();
        sb.append("包含关键词 \"").append(keyword).append("\" 的对话:\n\n");
        
        int count = 0;
        for (Message msg : allMessages) {
            if (msg.getContent().contains(keyword)) {
                String role = msg.isAi() ? "助手" : "用户";
                sb.append(role).append(": ").append(msg.getContent()).append("\n\n");
                count++;
                
                if (count >= limit) {
                    break;
                }
            }
        }
        
        if (count == 0) {
            return "未找到包含关键词 \"" + keyword + "\" 的对话";
        }
        
        return sb.toString();
    }
    
    /**
     * 生成对话摘要
     */
    private String generateSummary(int limit) {
        List<Message> messages = dbHelper.getAllMessages();
        
        if (messages.isEmpty()) {
            return "暂无历史对话记录";
        }
        
        // 获取最近的消息用于摘要
        int startIdx = Math.max(0, messages.size() - limit);
        List<Message> recentMessages = messages.subList(startIdx, messages.size());
        
        // 统计信息
        int userMsgCount = 0;
        int aiMsgCount = 0;
        int totalChars = 0;
        
        for (Message msg : recentMessages) {
            if (msg.isAi()) {
                aiMsgCount++;
            } else {
                userMsgCount++;
            }
            totalChars += msg.getContent().length();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("对话摘要:\n");
        sb.append("- 总对话轮数: ").append(recentMessages.size()).append("\n");
        sb.append("- 用户消息: ").append(userMsgCount).append("条\n");
        sb.append("- 助手消息: ").append(aiMsgCount).append("条\n");
        sb.append("- 总字符数: ").append(totalChars).append("\n\n");
        
        // 添加最近3条对话作为上下文
        sb.append("最近对话片段:\n");
        int snippetStart = Math.max(0, recentMessages.size() - 6);
        for (int i = snippetStart; i < recentMessages.size(); i++) {
            Message msg = recentMessages.get(i);
            String role = msg.isAi() ? "助手" : "用户";
            String content = msg.getContent();
            if (content.length() > 100) {
                content = content.substring(0, 100) + "...";
            }
            sb.append(role).append(": ").append(content).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}

