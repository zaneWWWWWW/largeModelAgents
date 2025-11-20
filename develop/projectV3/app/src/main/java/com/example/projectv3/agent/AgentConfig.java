package com.example.projectv3.agent;

/**
 * Agent配置类
 */
public class AgentConfig {
    private String systemPrompt;
    private int maxIterations;
    private float temperature;
    private int maxTokens;
    private boolean enableToolUse;
    private boolean enableMemory;
    
    private AgentConfig(Builder builder) {
        this.systemPrompt = builder.systemPrompt;
        this.maxIterations = builder.maxIterations;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.enableToolUse = builder.enableToolUse;
        this.enableMemory = builder.enableMemory;
    }
    
    public String getSystemPrompt() {
        return systemPrompt;
    }
    
    public int getMaxIterations() {
        return maxIterations;
    }
    
    public float getTemperature() {
        return temperature;
    }
    
    public int getMaxTokens() {
        return maxTokens;
    }
    
    public boolean isEnableToolUse() {
        return enableToolUse;
    }
    
    public boolean isEnableMemory() {
        return enableMemory;
    }
    
    public static class Builder {
        private String systemPrompt = "你是一个乐于助人的AI助手。\n\n" +
               "你的任务是根据用户的输入，决定是否需要使用工具。你的回复必须是以下两种格式之一：\n\n" +
               "1. 调用工具: 如果你需要使用工具，你的回复只能是一个JSON代码块，格式如下：\n" +
               "```json\n" +
               "{\n" +
               "  \"tool\": \"工具名称\",\n" +
               "  \"parameters\": {}\n" +
               "}\n" +
               "```\n" +
               "绝对不要在JSON前后添加任何文字。\n\n" +
               "2. 直接回复: 如果你不需要使用工具，就直接对用户进行回复。\n" +
               "你的回复必须是给用户的最终答复，不能包含任何'思考'、'回答'之类的标签。";
        private int maxIterations = 5;
        private float temperature = 0.7f;
        private int maxTokens = 512;
        private boolean enableToolUse = true;
        private boolean enableMemory = true;
        
        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }
        
        public Builder maxIterations(int maxIterations) {
            this.maxIterations = maxIterations;
            return this;
        }
        
        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder maxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }
        
        public Builder enableToolUse(boolean enableToolUse) {
            this.enableToolUse = enableToolUse;
            return this;
        }
        
        public Builder enableMemory(boolean enableMemory) {
            this.enableMemory = enableMemory;
            return this;
        }
        
        public AgentConfig build() {
            return new AgentConfig(this);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static String unifiedSystemPrompt() {
        return "你是一位面向学生的心理咨询助理。请以同理、支持、具体可行建议的中文口吻回应，避免空泛复读。出现抑郁、焦虑、自伤、自杀或暴力等风险表述时，优先进行心理状态评估。\n\n需要使用工具时，回复必须是包裹在三反引号内的 JSON 代码块，格式如下：\n```json\n{\n  \"tool\": \"psychological_assessment\",\n  \"parameters\": { \n    \"trigger_reason\": \"简要说明触发原因\" \n  }\n}\n```\n否则请直接给予面向用户的最终中文回复。不要输出思考或规则说明。";
    }
}

