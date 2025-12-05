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
        private String systemPrompt = "你是一个专业的心理健康助手，可以使用工具来帮助用户。";
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
}

