package com.example.projectv3.agent;

import org.json.JSONObject;

/**
 * Agent工具接口
 * 所有工具都需要实现此接口
 */
public interface Tool {
    /**
     * 获取工具名称
     * @return 工具的唯一标识名称
     */
    String getName();
    
    /**
     * 获取工具描述
     * @return 工具功能的详细描述，用于LLM理解工具用途
     */
    String getDescription();
    
    /**
     * 获取工具参数schema（JSON Schema格式）
     * @return 参数定义的JSON字符串
     */
    String getParametersSchema();
    
    /**
     * 执行工具
     * @param parameters 工具参数（JSON格式）
     * @return 工具执行结果
     * @throws Exception 执行过程中的异常
     */
    ToolResult execute(JSONObject parameters) throws Exception;
    
    /**
     * 工具执行结果
     */
    class ToolResult {
        private boolean success;
        private String result;
        private String error;
        
        public ToolResult(boolean success, String result, String error) {
            this.success = success;
            this.result = result;
            this.error = error;
        }
        
        public static ToolResult success(String result) {
            return new ToolResult(true, result, null);
        }
        
        public static ToolResult error(String error) {
            return new ToolResult(false, null, error);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getResult() {
            return result;
        }
        
        public String getError() {
            return error;
        }
        
        @Override
        public String toString() {
            if (success) {
                return "Success: " + result;
            } else {
                return "Error: " + error;
            }
        }
    }
}

