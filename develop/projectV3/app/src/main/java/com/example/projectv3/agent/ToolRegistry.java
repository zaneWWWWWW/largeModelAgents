package com.example.projectv3.agent;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 工具注册中心
 * 管理所有可用的工具
 */
public class ToolRegistry {
    private static final String TAG = "ToolRegistry";
    
    private final Map<String, Tool> tools = new HashMap<>();
    
    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        if (tool == null) {
            Log.w(TAG, "尝试注册空工具");
            return;
        }
        
        String name = tool.getName();
        if (name == null || name.isEmpty()) {
            Log.w(TAG, "工具名称为空，无法注册");
            return;
        }
        
        tools.put(name, tool);
        Log.d(TAG, "注册工具: " + name);
    }
    
    /**
     * 获取工具
     */
    public Tool getTool(String name) {
        return tools.get(name);
    }
    
    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
    
    /**
     * 获取所有工具名称
     */
    public String[] getToolNames() {
        return tools.keySet().toArray(new String[0]);
    }
    
    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return tools.size();
    }
    
    /**
     * 生成工具描述（用于提示词）
     * 格式化为LLM可理解的工具列表
     */
    public String generateToolsDescription() {
        if (tools.isEmpty()) {
            return "当前没有可用的工具。";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("你可以使用以下工具:\n\n");
        
        for (Tool tool : tools.values()) {
            sb.append("工具名称: ").append(tool.getName()).append("\n");
            sb.append("功能描述: ").append(tool.getDescription()).append("\n");
            sb.append("参数格式: ").append(tool.getParametersSchema()).append("\n");
            sb.append("\n");
        }
        
        sb.append("使用工具的格式:\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"tool\": \"工具名称\",\n");
        sb.append("  \"parameters\": {\n");
        sb.append("    \"参数名\": \"参数值\"\n");
        sb.append("  }\n");
        sb.append("}\n");
        sb.append("```\n");
        
        return sb.toString();
    }
    
    /**
     * 生成工具列表的JSON格式（OpenAI Function Calling风格）
     */
    public JSONArray generateToolsJSON() {
        JSONArray toolsArray = new JSONArray();
        
        try {
            for (Tool tool : tools.values()) {
                JSONObject toolObj = new JSONObject();
                toolObj.put("type", "function");
                
                JSONObject function = new JSONObject();
                function.put("name", tool.getName());
                function.put("description", tool.getDescription());
                
                // 解析参数schema
                try {
                    JSONObject parameters = new JSONObject(tool.getParametersSchema());
                    function.put("parameters", parameters);
                } catch (Exception e) {
                    Log.e(TAG, "解析工具参数schema失败: " + tool.getName(), e);
                }
                
                toolObj.put("function", function);
                toolsArray.put(toolObj);
            }
        } catch (Exception e) {
            Log.e(TAG, "生成工具JSON失败", e);
        }
        
        return toolsArray;
    }
    
    /**
     * 清空所有工具
     */
    public void clear() {
        tools.clear();
        Log.d(TAG, "清空所有工具");
    }
}

