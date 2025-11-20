package com.example.projectv3.fragment;

import android.app.AlertDialog;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.projectv3.LLamaAPI;
import com.example.projectv3.R;
import com.example.projectv3.adapter.MessageAdapter;
import com.example.projectv3.db.ChatDbHelper;
import com.example.projectv3.model.Message;
import com.example.projectv3.utils.ConversationCounter;
import com.example.projectv3.service.PsychologicalStatusService;
import com.example.projectv3.utils.ModelLoadingDialogManager;
import com.example.projectv3.agent.AgentConfig;
import com.example.projectv3.agent.AgentManager;
import com.example.projectv3.agent.AgentCore;
import com.example.projectv3.agent.llm.GeminiApiClient;
import com.example.projectv3.agent.llm.LocalLlamaProvider;
import com.example.projectv3.api.ApiClient;
import com.example.projectv3.api.PsychStatusApi;
import com.example.projectv3.dto.PsychStatusDTO;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AiChatFragment extends Fragment implements LLamaAPI.ModelStateListener {
    
    private static final String TAG = "AiChatFragment";
    
    private RecyclerView messagesRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ImageButton clearButton;
    private MessageAdapter messageAdapter;
    private ChatDbHelper dbHelper;
    private LLamaAPI chatLlamaApi; // 专用于聊天的LLamaAPI实例
    private Handler mainHandler;
    private boolean isGenerating = false;
    private long lastUIUpdateTime = 0;
    private static final long UI_UPDATE_INTERVAL = 50; // 毫秒
    private ConversationCounter counterManager;
    private PsychologicalStatusService psychologicalStatusService;
    private com.example.projectv3.utils.PsychologicalStatusManager psychologicalStatusManager;
    private SwipeRefreshLayout swipeRefreshLayout;
    // 上滑触发心理评测的控制变量
    private long lastAnalysisTriggerTime = 0;
    private static final long ANALYSIS_COOLDOWN_MS = 8000; // 8秒冷却，避免频繁触发
    private int upwardScrollAccum = 0;
    
    // 添加静态变量，用于跨Fragment共享模型实例
    private static LLamaAPI sharedChatLlamaApi;
    
    // Agent相关
    private AgentManager agentManager;
    private enum AgentBrainMode { NO_AGENT, LOCAL_BRAIN, GEMINI_BRAIN }
    private AgentBrainMode currentBrainMode = AgentBrainMode.NO_AGENT;

    // LLM Providers
    private com.example.projectv3.agent.llm.GeminiApiClient geminiBrain;
    private com.example.projectv3.agent.llm.LocalLlamaProvider localLlmProvider;
    private long sendButtonPressStartTime = 0;
    


    public static AiChatFragment newInstance() {
        return new AiChatFragment();
    }
    
    /**
     * 显示模型加载对话框
     */
    private void showModelLoadingDialog() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        ModelLoadingDialogManager.getInstance().showModelLoadingDialog(
            getContext(),
            () -> loadSelectedModel("default") // 立即加载（强制）
        );
    }
    
    /**
     * 加载选定的模型
     */
    private void loadSelectedModel(String modelType) {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        // 确保mainHandler已初始化
        if (mainHandler == null) {
            mainHandler = new Handler(Looper.getMainLooper());
        }
        
        // 创建加载进度对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("正在加载模型");
        builder.setMessage("正在检查模型文件...");
        builder.setCancelable(false);
        
        AlertDialog progressDialog = builder.create();
        progressDialog.show();
        
        // 在后台线程中加载模型
        new Thread(() -> {
            try {
                // 构建模型文件路径
                File filesDir = getContext().getFilesDir();
                File modelsDir = new File(filesDir, "models");
                String chatModelPath = new File(modelsDir, "XiangZhang_chat.gguf").getAbsolutePath();
                String statusModelPath = new File(modelsDir, "XiangZhang_status.gguf").getAbsolutePath();
                
                // 检查聊天模型文件是否存在
                File chatModelFile = new File(chatModelPath);
                File statusModelFile = new File(statusModelPath);
                
                // 如果聊天模型文件不存在或无效，从assets复制
                if (!chatModelFile.exists() || chatModelFile.length() < 10 * 1024 * 1024 || !chatModelFile.canRead()) {
                    mainHandler.post(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.setMessage("正在复制聊天模型文件...");
                        }
                    });
                    
                    // 确保目录存在
                    if (!modelsDir.exists()) {
                        modelsDir.mkdirs();
                    }
                    
                    // 从assets复制聊天模型文件
                    copyModelFromAssets("XiangZhang_chat.gguf", chatModelPath);
                }
                
                // 如果状态判断模型文件不存在或无效，从assets复制
                if (!statusModelFile.exists() || statusModelFile.length() < 10 * 1024 * 1024 || !statusModelFile.canRead()) {
                    mainHandler.post(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.setMessage("正在复制状态判断模型文件...");
                        }
                    });
                    
                    // 从assets复制状态判断模型文件
                    copyModelFromAssets("XiangZhang_status.gguf", statusModelPath);
                }
                
                // 更新进度信息
                mainHandler.post(() -> {
                    if (progressDialog.isShowing()) {
                        progressDialog.setMessage("正在加载AI模型到内存...");
                    }
                });
                
                // 添加模型状态监听器
                chatLlamaApi.addModelStateListener(new LLamaAPI.ModelStateListener() {
                    @Override
                    public void onModelLoaded() {
                        mainHandler.post(() -> {
                            if (isAdded() && getContext() != null) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Toast.makeText(getContext(), "AI模型加载成功！可以开始对话了", Toast.LENGTH_LONG).show();
                                
                                // 启用发送按钮
                                if (sendButton != null) {
                                    sendButton.setEnabled(true);
                                }
                                

                            }
                        });
                    }
                    
                    @Override
                    public void onModelUnloaded() {
                        // 模型卸载时的处理
                    }
                });
                
                // 加载模型
                chatLlamaApi.loadModel(chatModelPath);
                
            } catch (Exception e) {
                Log.e(TAG, "加载模型时发生异常", e);
                mainHandler.post(() -> {
                    if (isAdded() && getContext() != null) {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "模型加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
    
    // 从ProfileFragment获取共享的模型实例
    private LLamaAPI getSharedModelInstance() {
        try {
            // 尝试获取ProfileFragment中的共享实例
            Class<?> profileFragmentClass = Class.forName("com.example.projectv3.fragment.ProfileFragment");
            java.lang.reflect.Field field = profileFragmentClass.getDeclaredField("sharedProfileLlamaApi");
            field.setAccessible(true);
            Object instance = field.get(null);
            if (instance instanceof LLamaAPI) {
                Log.d(TAG, "成功获取ProfileFragment中的共享模型实例");
                return (LLamaAPI) instance;
            }
        } catch (Exception e) {
            Log.e(TAG, "获取ProfileFragment共享模型实例失败", e);
        }
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ai_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化Handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 优先使用ProfileFragment中的共享模型实例
        LLamaAPI profileSharedInstance = getSharedModelInstance();
        
        if (profileSharedInstance != null && profileSharedInstance.isModelLoaded()) {
            // 使用ProfileFragment中已加载的模型实例
            sharedChatLlamaApi = profileSharedInstance;
            Log.d(TAG, "使用ProfileFragment中已加载的模型实例");
        } else if (sharedChatLlamaApi == null) {
            // 如果没有可用的共享实例，创建新实例
            sharedChatLlamaApi = LLamaAPI.createInstance("chat_model");
            Log.d(TAG, "创建聊天专用LLamaAPI实例");
        }
        chatLlamaApi = sharedChatLlamaApi;
        
        // 注册监听器
        chatLlamaApi.addModelStateListener(this);
        
        // 设置更优的生成温度
        chatLlamaApi.setTemperature(0.7f);
        
        // 只有在首次创建时重置聊天会话，而不是每次进入页面
        if (savedInstanceState == null) {
            chatLlamaApi.resetChatSession();
        }
        
        // 检查模型状态并记录
        boolean modelLoaded = chatLlamaApi.isModelLoaded();
        Log.d(TAG, "聊天模型初始加载状态: " + modelLoaded);
        
        // 如果模型未加载，尝试自动加载
        if (!modelLoaded) {
            checkAndLoadModel();
        }
        
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);
        long userId = prefs.getLong("user_id", -1);
        dbHelper = new ChatDbHelper(requireContext(), userId);
        
        // 初始化对话计数器和心理状态评估服务
        counterManager = new ConversationCounter(requireContext());
        psychologicalStatusService = new PsychologicalStatusService(requireContext());
        psychologicalStatusManager = new com.example.projectv3.utils.PsychologicalStatusManager(requireContext());
        
        // 初始化视图
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayoutChat);
        messagesRecyclerView = view.findViewById(R.id.messagesRecyclerView);
        messageInput = view.findViewById(R.id.messageInput);
        sendButton = view.findViewById(R.id.sendButton);
        clearButton = view.findViewById(R.id.clearButton);

        // 设置RecyclerView
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<Message> messages = dbHelper.getAllMessages();
        messageAdapter = new MessageAdapter(messages);
        messagesRecyclerView.setAdapter(messageAdapter);

        // 关闭下拉触发心理评测（改为长按发送按钮触发）
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setEnabled(false);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                // 禁用刷新行为
                swipeRefreshLayout.setRefreshing(false);
            });
        }

        // 初始化LLM Providers
        localLlmProvider = new LocalLlamaProvider(chatLlamaApi);
        geminiBrain = new GeminiApiClient(
                "sk-ECevkrOnxPH565SxEeNeJ57CHvgdEh84IGTGlazQx9xUr6Dd",
                "https://api.vectorengine.ai/v1/chat/completions",
                "gemini-2.5-flash"
        );

        // 初始化Agent管理器
        agentManager = new AgentManager(requireContext());
        setupAgent(); // 根据默认模式设置Agent

        // 设置发送按钮的触摸事件监听器，以区分短按和长按
        sendButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    sendButtonPressStartTime = System.currentTimeMillis();
                    v.setPressed(true);
                    return true; // 消费事件
                case android.view.MotionEvent.ACTION_UP:
                    v.setPressed(false);
                    long pressDuration = System.currentTimeMillis() - sendButtonPressStartTime;
                    if (pressDuration > 6000) { // 长按超过6秒
                        toggleLocalBrainMode();
                    } else if (pressDuration > 3000) { // 长按3到6秒
                        toggleGeminiBrainMode();
                    } else { // 短按
                        handleSendMessage();
                    }
                    return true; // 消费事件
            }
            return false;
        });

        // 设置清空按钮点击事件
        clearButton.setOnClickListener(v -> clearChatHistory());

        // 清空按钮长按：从case.txt导入对话
        clearButton.setOnLongClickListener(v -> {
            importConversationFromPreferredLocation();
            return true;
        });
    }

    /**
     * 使用Agent模式发送消息
     */
    private void sendMessageWithAgent() {
        String content = messageInput.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }
        
        // Agent的初始化检查已在发送按钮的点击事件中完成
        
        // 避免重复生成
        if (isGenerating) {
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "AI正在思考中，请稍候...", Toast.LENGTH_SHORT).show();
                }
            return;
        }
        
        // 保存并显示用户消息
        Message userMessage = new Message(content, false);
        dbHelper.insertMessage(userMessage);
        messageAdapter.addMessage(userMessage);
        
        // 清空输入框
        messageInput.setText("");
        
        // 滚动到底部
        messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
        
        // 显示AI正在输入的状态
        Message aiMessage = new Message("AI正在思考中...", true);
        dbHelper.insertMessage(aiMessage);
        messageAdapter.addMessage(aiMessage);
        messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
        
        // 禁用发送按钮
        isGenerating = true;
        sendButton.setEnabled(false);
        
        // 使用Agent执行
        agentManager.run(content, new AgentCore.AgentCallback() {
            private StringBuilder responseBuilder = new StringBuilder();
            
            @Override
            public void onToolCall(String toolName, String parameters) {
                mainHandler.post(() -> {
                    if (isAdded()) {
                        Log.d(TAG, "Agent调用工具: " + toolName);
                        String toolDisplayName = getToolDisplayName(toolName);
                        String toolMsg = "🔧 正在使用工具: " + toolDisplayName;

                        // 为耗时较长的工具添加额外提示
                        if ("psychological_assessment".equals(toolName)) {
                            toolMsg += "\n(这可能需要一些时间，请稍候...)";
                        }

                        aiMessage.setContent(toolMsg);
                        messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                    }
                });
            }
            
            @Override
            public void onToolResult(String toolName, String result) {
                mainHandler.post(() -> {
                    if (isAdded()) {
                        Log.d(TAG, "工具执行结果: " + result);
                        String toolMsg = "✅ 工具执行完成\n正在整合结果...";
                        aiMessage.setContent(toolMsg);
                        messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);

                        if ("psychological_assessment".equals(toolName) && result != null) {
                            try {
                                int startIndex = result.indexOf('{');
                                int endIndex = result.indexOf('}', startIndex) + 1;
                                if (startIndex >= 0 && endIndex > startIndex) {
                                    String jsonStr = result.substring(startIndex, endIndex);
                                    java.util.regex.Pattern depressionPattern = java.util.regex.Pattern.compile("\"depression_level\"\\s*:\\s*(\\d)");
                                    java.util.regex.Pattern anxietyPattern = java.util.regex.Pattern.compile("\"anxiety_level\"\\s*:\\s*(\\d)");
                                    java.util.regex.Pattern riskPattern = java.util.regex.Pattern.compile("\"risk_flag\"\\s*:\\s*\"(\\w+)\"");
                                    java.util.regex.Pattern distressPattern = java.util.regex.Pattern.compile("\"student_distress_score\"\\s*:\\s*(\\d)");
                                    java.util.regex.Matcher dm = depressionPattern.matcher(jsonStr);
                                    java.util.regex.Matcher am = anxietyPattern.matcher(jsonStr);
                                    java.util.regex.Matcher rm = riskPattern.matcher(jsonStr);
                                    java.util.regex.Matcher sm = distressPattern.matcher(jsonStr);
                                    int d = dm.find() ? Integer.parseInt(dm.group(1)) : 0;
                                    int a = am.find() ? Integer.parseInt(am.group(1)) : 0;
                                    String r = rm.find() ? rm.group(1) : "none";
                                    int s = sm.find() ? Integer.parseInt(sm.group(1)) : 0;
                                    try {
                                        if (psychologicalStatusManager != null) {
                                            psychologicalStatusManager.saveStatusResult(jsonStr);
                                        }
                                    } catch (Exception se) {
                                        Log.w(TAG, "保存心理评估到SQLite失败: " + se.getMessage());
                                    }
                                    reportPsychStatusToBackend(d, a, r, s, "ADVANCED_AGENT");
                                } else {
                                    java.util.regex.Pattern depZh = java.util.regex.Pattern.compile("抑郁程度\\s*[:：].*?级别\\s*(\\d)");
                                    java.util.regex.Pattern anxZh = java.util.regex.Pattern.compile("焦虑程度\\s*[:：].*?级别\\s*(\\d)");
                                    java.util.regex.Pattern riskZh = java.util.regex.Pattern.compile("风险标记\\s*[:：]\\s*(\\S+)");
                                    java.util.regex.Pattern distZh = java.util.regex.Pattern.compile("困扰分数\\s*[:：]\\s*(\\d+)\\s*分");
                                    java.util.regex.Matcher dmZh = depZh.matcher(result);
                                    java.util.regex.Matcher amZh = anxZh.matcher(result);
                                    java.util.regex.Matcher rmZh = riskZh.matcher(result);
                                    java.util.regex.Matcher smZh = distZh.matcher(result);
                                    int d = dmZh.find() ? Integer.parseInt(dmZh.group(1)) : 0;
                                    int a = amZh.find() ? Integer.parseInt(amZh.group(1)) : 0;
                                    String rText = rmZh.find() ? rmZh.group(1) : "无风险";
                                    int s = smZh.find() ? Integer.parseInt(smZh.group(1)) : 0;
                                    String r;
                                    if ("无风险".equals(rText)) r = "none";
                                    else if ("自杀风险".equals(rText)) r = "suicidal";
                                    else if ("自伤风险".equals(rText)) r = "self_harm";
                                    else if ("暴力风险".equals(rText)) r = "violence";
                                    else r = "none";
                                    org.json.JSONObject obj = new org.json.JSONObject();
                                    obj.put("depression_level", d);
                                    obj.put("anxiety_level", a);
                                    obj.put("risk_flag", r);
                                    obj.put("student_distress_score", s);
                                    String jsonStr = obj.toString();
                                    try {
                                        if (psychologicalStatusManager != null) {
                                            psychologicalStatusManager.saveStatusResult(jsonStr);
                                        }
                                    } catch (Exception se) {
                                        Log.w(TAG, "保存心理评估到SQLite失败: " + se.getMessage());
                                    }
                                    reportPsychStatusToBackend(d, a, r, s, "ADVANCED_AGENT");
                                }
                            } catch (Exception ex) {
                                Log.w(TAG, "解析并上报心理评估结果失败: " + ex.getMessage());
                            }
                        }
                    }
                });
            }
            
            @Override
            public void onToken(String token) {
                if (token != null && !token.isEmpty()) {
                    responseBuilder.append(token);
                    mainHandler.post(() -> {
                        if (isAdded()) {
                            aiMessage.setContent(responseBuilder.toString());
                            messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                        }
                    });
                }
            }

            @Override
            public void onFinalResponse(String response) {
                mainHandler.post(() -> {
                    if (isAdded()) {
                        String finalContent = responseBuilder.toString();
                        if (finalContent.isEmpty()) {
                            finalContent = response;
                        }
                        aiMessage.setContent(finalContent);
                        dbHelper.updateMessage(aiMessage);
                        messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                        
                        // 增加对话计数
                        counterManager.incrementConversationCount();
                        
                        // 重新启用发送按钮
                        isGenerating = false;
                        sendButton.setEnabled(true);
                        
                        Log.d(TAG, "Agent回复完成");
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    if (isAdded() && getContext() != null) {
                        Log.e(TAG, "Agent执行错误", e);
                        String errorMessage = "生成失败: " + e.getMessage();
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                        
                        aiMessage.setContent(errorMessage);
                        dbHelper.updateMessage(aiMessage);
                        messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                        
                        isGenerating = false;
                        sendButton.setEnabled(true);
                    }
                });
            }
        });
    }
    
    /**
     * 获取工具的显示名称
     */
    private String getToolDisplayName(String toolName) {
        switch (toolName) {
            case "psychological_assessment":
                return "心理状态评估";
            case "memory_query":
                return "记忆查询";
            case "conversation_counter":
                return "对话计数";
            default:
                return toolName;
        }
    }
    
    /**
     * 根据当前大脑模式设置Agent
     */
    private void setupAgent() {
        if (agentManager == null) return;

        AgentConfig config = AgentConfig.builder()
                .systemPrompt(AgentConfig.unifiedSystemPrompt())
                .build();

        switch (currentBrainMode) {
            case LOCAL_BRAIN:
                if (localLlmProvider != null && localLlmProvider.isModelLoaded()) {
                    // 使用本地模型作为大脑，不注册local_chat工具
                    agentManager.initialize(localLlmProvider, config, null);
                    Log.d(TAG, "Agent setup with LOCAL BRAIN.");
                } else {
                    Log.w(TAG, "Local model not loaded, cannot set up Agent with local brain.");
                }
                break;

            case GEMINI_BRAIN:
                // 使用Gemini作为大脑，并将本地模型作为local_chat工具
                agentManager.initialize(geminiBrain, config, localLlmProvider);
                Log.d(TAG, "Agent setup with GEMINI BRAIN.");
                break;

            case NO_AGENT:
            default:
                // 不初始化Agent核心
                Log.d(TAG, "Agent disabled.");
                break;
        }
    }

    /**
     * 处理发送消息的逻辑（短按时调用）
     */
    private void handleSendMessage() {
        switch (currentBrainMode) {
            case LOCAL_BRAIN:
            case GEMINI_BRAIN:
                if (agentManager.isInitialized()) {
                    sendMessageWithAgent();
                } else {
                    Toast.makeText(getContext(), "Agent未初始化，请稍候或重启应用", Toast.LENGTH_SHORT).show();
                }
                break;
            case NO_AGENT:
            default:
                sendMessage();
                break;
        }
    }

    /**
     * 切换 无Agent <-> 本地大脑 模式（长按 > 6秒）
     */
    private void toggleLocalBrainMode() {
        if (!isAdded() || getContext() == null) return;

        if (currentBrainMode == AgentBrainMode.LOCAL_BRAIN) {
            currentBrainMode = AgentBrainMode.NO_AGENT;
        } else {
            currentBrainMode = AgentBrainMode.LOCAL_BRAIN;
        }
        setupAgent();

        String message;
        if (currentBrainMode == AgentBrainMode.LOCAL_BRAIN) {
            if (agentManager.isInitialized()) {
                message = "🧠 本地大脑模式\n纯离线，使用本地模型进行决策。";
            } else {
                message = "⚠️ 本地模型未加载，无法切换到本地大脑模式。";
                currentBrainMode = AgentBrainMode.NO_AGENT; // 切换失败，退回
            }
        } else {
            message = "💬 普通对话模式\nAI将直接回复，不使用工具。";
        }
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        Log.d(TAG, "Switched to mode: " + currentBrainMode.name());
    }

    /**
     * 切换 无Agent <-> Gemini大脑 模式（长按3-6秒）
     */
    private void toggleGeminiBrainMode() {
        if (!isAdded() || getContext() == null) return;

        if (currentBrainMode == AgentBrainMode.GEMINI_BRAIN) {
            currentBrainMode = AgentBrainMode.NO_AGENT;
        } else {
            currentBrainMode = AgentBrainMode.GEMINI_BRAIN;
        }
        setupAgent();

        String message;
        if (currentBrainMode == AgentBrainMode.GEMINI_BRAIN) {
            message = "✨ 高级Agent模式已启用\nAI将进行深度思考和推理。";
        } else {
            message = "💬 普通对话模式\nAI将直接回复，不使用工具。";
        }
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        Log.d(TAG, "Switched to mode: " + currentBrainMode.name());
    }

    private void sendMessage() {
        String content = messageInput.getText().toString().trim();
        if (!content.isEmpty()) {
            // 在发送消息前再次尝试获取共享模型实例
            if (!chatLlamaApi.isModelLoaded()) {
                // 尝试获取ProfileFragment中的共享实例
                LLamaAPI profileSharedInstance = getSharedModelInstance();
                if (profileSharedInstance != null && profileSharedInstance.isModelLoaded()) {
                    // 更新为已加载的模型实例
                    sharedChatLlamaApi = profileSharedInstance;
                    chatLlamaApi = sharedChatLlamaApi;
                    Log.d(TAG, "发送消息前更新为ProfileFragment中已加载的模型实例");
                }
            }
            
            // 再次检查模型是否已加载
            boolean modelLoaded = chatLlamaApi.isModelLoaded();
            Log.d(TAG, "聊天前检查模型: isModelLoaded = " + modelLoaded);
            
            if (!modelLoaded) {
                if (isAdded() && getContext() != null) {
                    showModelLoadingDialog();
                }
                return;
            }
            
            // 避免重复生成
            if (isGenerating) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "AI正在思考中，请稍候...", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            // 保存并显示用户消息到数据库
            Message userMessage = new Message(content, false);
            dbHelper.insertMessage(userMessage);
            messageAdapter.addMessage(userMessage);

            // 增加对话计数
            counterManager.incrementConversationCount();
            Log.d(TAG, "对话计数: " + counterManager.getCurrentCount() + 
                  ", 距离下次分析还需: " + counterManager.getRemainingCountForNextAnalysis() + "轮对话");
            
            // 清空输入框
            messageInput.setText("");

            // 滚动到底部
            messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);

            // 显示AI正在输入的状态，同时插入到数据库
            Message aiMessage = new Message("AI思考中...", true);
            // 先插入到数据库获取ID
            dbHelper.insertMessage(aiMessage);
            messageAdapter.addMessage(aiMessage);
            messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
            
            // 禁用发送按钮
            isGenerating = true;
            sendButton.setEnabled(false);
            
            // 使用LLamaAPI生成回复
            StringBuilder responseBuilder = new StringBuilder();
            long startTime = System.currentTimeMillis();
            final AtomicInteger tokenCount = new AtomicInteger(0);

            chatLlamaApi.chat(content, new LLamaAPI.CompletionCallback() {
                @Override
                public void onToken(String token) {
                    if (token != null && !token.isEmpty()) {
                        responseBuilder.append(token);
                        tokenCount.incrementAndGet();
                        
                        // 使用时间间隔控制UI更新频率
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUIUpdateTime > UI_UPDATE_INTERVAL) {
                            mainHandler.post(() -> {
                                if (isAdded()) {
                                    aiMessage.setContent(responseBuilder.toString());
                                    messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                                    
                                    // 只在需要时滚动
                                    LinearLayoutManager layoutManager = 
                                        (LinearLayoutManager) messagesRecyclerView.getLayoutManager();
                                    int position = layoutManager.findLastVisibleItemPosition();
                                    int count = messageAdapter.getItemCount();
                                    if (position >= count - 3) {
                                        messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                                    }
                                }
                            });
                            lastUIUpdateTime = currentTime;
                        }
                    }
                }

                @Override
                public void onComplete() {
                    long endTime = System.currentTimeMillis();
                    float seconds = (endTime - startTime) / 1000f;
                    float tokensPerSecond = tokenCount.get() / Math.max(seconds, 0.1f);
                    
                    // 立即清空所有待处理的UI更新
                    mainHandler.removeCallbacksAndMessages(null);
                    
                    mainHandler.post(() -> {
                        if (isAdded()) {
                            // 更新最终结果并保存到数据库
                            String finalContent = responseBuilder.toString();
                            aiMessage.setContent(finalContent);
                            dbHelper.updateMessage(aiMessage);
                            messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                            
                            // 日志记录性能
                            Log.d(TAG, String.format("生成完成，内容长度: %d字符, %d个token, 用时: %.1f秒, 速度: %.1f tokens/秒", 
                                  finalContent.length(), tokenCount.get(), seconds, tokensPerSecond));
                            
                            // 增加对话计数
                            int currentCount = counterManager.incrementConversationCount();
                            Log.d(TAG, "对话计数增加到: " + currentCount);
                            
                            // 更新对话状态指示器
                            showChatHistoryStatus();
                            
                            // 重新启用发送按钮
                            isGenerating = false;
                            sendButton.setEnabled(true);
                            
                            // 检查是否需要触发心理状态评估（每五轮对话）
                            if (counterManager.shouldPerformAnalysis()) {
                                Log.d(TAG, "达到五轮对话，触发心理状态评估");
                                performPsychologicalAnalysis();
                            } else {
                                Log.d(TAG, "距离下次心理状态评估还需: " + 
                                      counterManager.getRemainingCountForNextAnalysis() + "轮对话");
                            }
                        }
                    });
                }

                @Override
                public void onError(Exception e) {
                    mainHandler.post(() -> {
                        if (isAdded() && getContext() != null) {
                            Log.e(TAG, "Chat error", e);
                            String errorMessage = "生成失败: " + e.getMessage();
                            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                            
                            // 更新消息内容和数据库
                            aiMessage.setContent(errorMessage);
                            dbHelper.updateMessage(aiMessage);
                            messageAdapter.notifyItemChanged(messageAdapter.getItemCount() - 1);
                            
                            // 重新启用发送按钮
                            isGenerating = false;
                            sendButton.setEnabled(true);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        
        // 在页面恢复时尝试获取最新的共享模型实例
        LLamaAPI profileSharedInstance = getSharedModelInstance();
        if (profileSharedInstance != null && profileSharedInstance.isModelLoaded()) {
            // 如果ProfileFragment中有已加载的模型实例，使用该实例
            if (sharedChatLlamaApi != profileSharedInstance) {
                // 先移除旧实例的监听器
                if (chatLlamaApi != null) {
                    chatLlamaApi.removeModelStateListener(this);
                }
                
                // 更新为新的共享实例
                sharedChatLlamaApi = profileSharedInstance;
                chatLlamaApi = sharedChatLlamaApi;
                
                // 为新实例添加监听器
                chatLlamaApi.addModelStateListener(this);
                
                Log.d(TAG, "onResume: 更新为ProfileFragment中已加载的模型实例");
            }
        }
        
        // 检查并记录当前模型状态
        boolean modelLoaded = chatLlamaApi != null && chatLlamaApi.isModelLoaded();
        Log.d(TAG, "onResume: 当前模型加载状态 = " + modelLoaded);
        
        // 更新UI状态
        if (sendButton != null) {
            sendButton.setEnabled(modelLoaded);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清理资源
        if (dbHelper != null) {
            dbHelper.close();
        }
        // 移除监听器，但不卸载模型
        if (chatLlamaApi != null) {
            chatLlamaApi.removeModelStateListener(this);
        }
        
        // 释放Agent资源
        if (agentManager != null) {
            agentManager.release();
        }
        
        // 释放心理状态评估服务资源，但不卸载模型
        if (psychologicalStatusService != null) {
            // 不再调用release方法，避免卸载模型
            // psychologicalStatusService.release();
            Log.d(TAG, "保留心理状态评估服务资源，避免重新加载模型");
        }
    }
    
    // 实现ModelStateListener接口
    @Override
    public void onModelLoaded() {
        // 模型已加载，可以更新UI状态
        Log.d(TAG, "onModelLoaded callback received");
        mainHandler.post(() -> {
            if (isAdded()) {
                // 可以添加视觉提示表明模型已加载
                sendButton.setEnabled(true);
                
                if (getContext() != null) {
                    String modelName = chatLlamaApi != null ? chatLlamaApi.getCurrentModelName() : null;
                    String modelMessage;
                    
                    if (modelName != null) {
                        if (modelName.contains("QwQ")) {
                            modelMessage = "小模型 (QwQ-0.5B) 已加载完成，可以开始对话";
                        } else if (modelName.contains("Minicpm")) {
                            modelMessage = "大模型 (Minicpm-4B) 已加载完成，可以开始对话";
                        } else {
                            modelMessage = "模型 " + modelName + " 已加载完成，可以开始对话";
                        }
                    } else {
                        modelMessage = "AI模型已加载完成，可以开始对话";
                    }
                    
                    // 显示模型加载完成的提示
                    Toast.makeText(getContext(), modelMessage, Toast.LENGTH_SHORT).show();
                }
                

            }
        });
    }
    
    @Override
    public void onModelUnloaded() {
        // 模型已卸载，可以更新UI状态
        Log.d(TAG, "onModelUnloaded callback received");
        mainHandler.post(() -> {
            if (isAdded() && getContext() != null) {
                // 检查是否是用户主动卸载模型，只有在这种情况下才显示Toast
                // 页面跳转导致的监听器触发不应显示Toast
                if (sharedChatLlamaApi == null) {
                    Toast.makeText(getContext(), "模型已卸载，需要重新加载才能使用AI对话", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "模型实例仍然存在，不显示卸载提示");
                }
            }
        });
    }

    // 添加一个重置聊天历史的方法
    private void clearChatHistory() {
        new AlertDialog.Builder(requireContext())
            .setTitle("清除聊天历史")
            .setMessage("是否要清除所有聊天历史？AI将不再记得之前的对话内容。")
            .setPositiveButton("确定", (dialog, which) -> {
                // 清除LLamaAPI内部历史记录
                chatLlamaApi.resetChatSession(true);
                
                // 清除Agent历史记录
                if (agentManager != null) {
                    agentManager.reset();
                }
                
                // 清除数据库中的所有消息
                int deletedCount = dbHelper.deleteAllMessages();
                Log.d(TAG, "已从数据库中删除 " + deletedCount + " 条消息");
                
                // 重置对话计数器
                counterManager.resetCount();
                Log.d(TAG, "对话计数已重置");
                
                // 更新UI
                if (messageAdapter != null) {
                    messageAdapter.clearMessages();
                    messageAdapter.notifyDataSetChanged();
                }
                
                Toast.makeText(requireContext(), "聊天历史已清除", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // 显示当前对话状态
    private void showChatHistoryStatus() {
        if (isAdded() && chatLlamaApi != null) {
            int historySize = chatLlamaApi.getChatHistorySize();
            
            if (historySize > 2) {
                // 计算轮数（一轮是用户+AI的对话）
                int rounds = historySize / 2;
                String status = "AI已记忆" + rounds + "轮对话";
                
                // 只记录到日志，不打扰用户
                Log.d(TAG, status + ", 历史记录长度: " + historySize);
            }
        }
    }
    
    /**
     * 执行心理状态评估
     */
    private void performPsychologicalAnalysis() {
        // 显示加载对话框
        if (isAdded() && getContext() != null && psychologicalStatusService != null) {
            // 使用AlertDialog替代已弃用的ProgressDialog
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("正在进行心理状态评估...");
            builder.setCancelable(false);
            final AlertDialog dialog = builder.create();
            dialog.show();
            
            try {
                psychologicalStatusService.analyzeUserPsychologicalStatus(new PsychologicalStatusService.AnalysisCallback() {
                    @Override
                    public void onSuccess(String analysisResult) {
                        if (isAdded() && getContext() != null) {
                            // 关闭加载对话框
                            dialog.dismiss();
                            Log.d(TAG, "心理状态评估结果: " + analysisResult);
                            
                            // 保存模型输出的评估结果
                            boolean saved = psychologicalStatusManager.saveStatusResult(analysisResult);
                            if (saved) {
                                // 在日志中显示完整的评估结果历史JSON字符串
                                String historyJson = psychologicalStatusManager.getStatusHistory();
                                Log.d(TAG, "心理状态评估历史记录: " + historyJson);
                            } else {
                                Log.e(TAG, "保存心理状态评估结果失败");
                            }
                            
                            // 显示评估结果给用户
                            showPsychologicalAnalysisResult(analysisResult);
                        }
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        if (isAdded() && getContext() != null) {
                            // 关闭加载对话框
                            dialog.dismiss();
                            Log.e(TAG, "心理状态评估失败", e);
                            Toast.makeText(getContext(), "心理状态评估失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } catch (Exception e) {
                // 关闭加载对话框
                dialog.dismiss();
                // 显示错误信息
                Log.e(TAG, "心理状态评估失败", e);
                Toast.makeText(getContext(), "心理状态评估失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
    
    // 显示心理状态评估结果
    private void showPsychologicalAnalysisResult(String result) {
        Log.d(TAG, "心理状态评估结果: " + result);
        
        // 默认值
        int depressionLevel = 0; // 默认为无明显抑郁
        int anxietyLevel = 0;    // 默认为无明显焦虑
        String riskFlag = "none"; // 默认为无风险
        int distressScore = 0;    // 默认为0分
        
        String depressionState = "无明显抑郁";
        String anxietyState = "无明显焦虑";
        String riskState = "无风险";
        String distressState = "轻度困扰";
        
        try {
            // 尝试解析新的4字段JSON格式
            if (result != null && result.contains("depression_level")) {
                Log.d(TAG, "尝试解析4字段JSON格式的评估结果");
                // 提取JSON部分
                int startIndex = result.indexOf('{');
                int endIndex = result.indexOf('}', startIndex) + 1;
                
                if (startIndex >= 0 && endIndex > startIndex) {
                    String jsonStr = result.substring(startIndex, endIndex);
                    Log.d(TAG, "提取的JSON字符串: " + jsonStr);
                    
                    // 使用正则表达式提取4个字段的值
                    java.util.regex.Pattern depressionPattern = java.util.regex.Pattern.compile("\"depression_level\"\\s*:\\s*(\\d)");
                    java.util.regex.Pattern anxietyPattern = java.util.regex.Pattern.compile("\"anxiety_level\"\\s*:\\s*(\\d)");
                    java.util.regex.Pattern riskPattern = java.util.regex.Pattern.compile("\"risk_flag\"\\s*:\\s*\"(\\w+)\"");
                    java.util.regex.Pattern distressPattern = java.util.regex.Pattern.compile("\"student_distress_score\"\\s*:\\s*(\\d)");
                    
                    java.util.regex.Matcher depressionMatcher = depressionPattern.matcher(jsonStr);
                    java.util.regex.Matcher anxietyMatcher = anxietyPattern.matcher(jsonStr);
                    java.util.regex.Matcher riskMatcher = riskPattern.matcher(jsonStr);
                    java.util.regex.Matcher distressMatcher = distressPattern.matcher(jsonStr);
                    
                    // 解析depression_level
                    if (depressionMatcher.find()) {
                        depressionLevel = Integer.parseInt(depressionMatcher.group(1));
                        Log.d(TAG, "解析到抑郁级别: " + depressionLevel);
                    }
                    
                    // 解析anxiety_level
                    if (anxietyMatcher.find()) {
                        anxietyLevel = Integer.parseInt(anxietyMatcher.group(1));
                        Log.d(TAG, "解析到焦虑级别: " + anxietyLevel);
                    }
                    
                    // 解析risk_flag
                    if (riskMatcher.find()) {
                        riskFlag = riskMatcher.group(1);
                        Log.d(TAG, "解析到风险标记: " + riskFlag);
                    }
                    
                    // 解析student_distress_score
                if (distressMatcher.find()) {
                    distressScore = Integer.parseInt(distressMatcher.group(1));
                    Log.d(TAG, "解析到困扰分数: " + distressScore);
                }
                
                // 根据级别设置状态描述
                    switch (depressionLevel) {
                        case 0: depressionState = "无明显抑郁"; break;
                        case 1: depressionState = "轻度抑郁"; break;
                        case 2: depressionState = "中度抑郁"; break;
                        case 3: depressionState = "重度抑郁"; break;
                        default: depressionState = "未知"; break;
                    }
                    
                    switch (anxietyLevel) {
                        case 0: anxietyState = "无明显焦虑"; break;
                        case 1: anxietyState = "轻度焦虑"; break;
                        case 2: anxietyState = "中度焦虑"; break;
                        case 3: anxietyState = "重度焦虑"; break;
                        default: anxietyState = "未知"; break;
                    }
                    
                    switch (riskFlag) {
                        case "none": riskState = "无风险"; break;
                        case "suicidal": riskState = "自杀风险"; break;
                        case "self_harm": riskState = "自伤风险"; break;
                        case "violence": riskState = "暴力风险"; break;
                        default: riskState = "未知风险"; break;
                    }
                    
                    if (distressScore >= 0 && distressScore <= 3) {
                        distressState = "轻度困扰";
                    } else if (distressScore >= 4 && distressScore <= 6) {
                        distressState = "中度困扰";
                    } else if (distressScore >= 7 && distressScore <= 9) {
                        distressState = "重度困扰";
            } else {
                        distressState = "未知";
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "解析心理状态评估结果失败", e);
        }

        // 上报到后端（异步，不阻塞UI）
        try {
            reportPsychStatusToBackend(depressionLevel, anxietyLevel, riskFlag, distressScore, "LOCAL_AGENT");
        } catch (Exception ex) {
            Log.w(TAG, "上报心理状态失败(忽略): "+ ex.getMessage());
        }

        Log.d(TAG, String.format("最终评估结果 - 抑郁: %s, 焦虑: %s, 风险: %s, 困扰分数: %d(%s)", 
            depressionState, anxietyState, riskState, distressScore, distressState));
        
        // 使用final变量以便在lambda表达式中使用
        final String finalDepressionState = depressionState;
        final String finalAnxietyState = anxietyState;
        final String finalRiskState = riskState;
        final String finalDistressState = distressState;
        final int finalDistressScore = distressScore;
        
        // 使用requireActivity().runOnUiThread确保UI操作在主线程执行
        if (isAdded() && getContext() != null) {
            requireActivity().runOnUiThread(() -> {
                // 构建显示消息
                StringBuilder message = new StringBuilder();
                message.append("【情绪状态】\n");
                message.append("抑郁程度：").append(finalDepressionState).append("\n");
                message.append("焦虑程度：").append(finalAnxietyState).append("\n\n");
                message.append("【风险评估】\n");
                message.append("风险标记：").append(finalRiskState).append("\n\n");
                message.append("【困扰程度】\n");
                message.append("困扰分数：").append(finalDistressScore).append(" 分（").append(finalDistressState).append("）");
                
                // 显示4个维度的评估结果
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("心理状态评估报告")
                    .setMessage(message.toString())
                    .setPositiveButton("了解", null)
                    .create();
                dialog.show();
                
                // 上调对话框位置并取消灰色蒙版
                Window window = dialog.getWindow();
                if (window != null) {
                    WindowManager.LayoutParams params = window.getAttributes();
                    params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    int offsetDp = 48; // 距顶部约48dp
                    int offsetPx = (int) (offsetDp * getResources().getDisplayMetrics().density);
                    params.y = offsetPx;
                    window.setAttributes(params);
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    window.setDimAmount(0f);
                }
                Log.d(TAG, "显示心理状态评估对话框");
            });
        }
    }

    private void reportPsychStatusToBackend(int depressionLevel, int anxietyLevel, String riskFlag, int distressScore, String source) {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user_info", Context.MODE_PRIVATE);
        long userId = prefs.getLong("user_id", -1);
        Long uid = userId > 0 ? userId : null;

        PsychStatusDTO dto = new PsychStatusDTO(
                uid,
                depressionLevel,
                anxietyLevel,
                riskFlag,
                distressScore,
                System.currentTimeMillis(),
                source
        );

        PsychStatusApi api = ApiClient.getClient().create(PsychStatusApi.class);
        api.reportStatus(dto).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (isAdded() && getContext() != null) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "心理状态上报成功");
                    } else {
                        Log.w(TAG, "心理状态上报失败: " + response.code());
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "心理状态上报异常", t);
            }
        });
    }

    /**
     * 从优先位置导入对话：优先尝试绝对路径，其次尝试assets/case.txt
     */
    private void importConversationFromPreferredLocation() {
        if (!isAdded() || getContext() == null) return;

        AlertDialog loading = new AlertDialog.Builder(requireContext())
                .setMessage("正在导入case.txt对话...")
                .setCancelable(false)
                .create();
        loading.show();

        new Thread(() -> {
            int importedCount = 0;
            try {
                java.util.List<Message> parsed = null;

                // 1) 优先尝试绝对路径（Windows开发机路径）
                try {
                    java.io.File winFile = new java.io.File("d:\\Users\\Wangzeyu\\XiangZhang\\projectV3\\case.txt");
                    if (winFile.exists() && winFile.canRead()) {
                        parsed = parseCaseFile(new java.io.FileInputStream(winFile));
                        Log.d(TAG, "从Windows路径读取case.txt成功");
                    } else {
                        Log.d(TAG, "Windows路径不存在或不可读，尝试assets");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "读取Windows路径case.txt失败", e);
                }

                // 2) 退回到assets/case.txt
                if (parsed == null || parsed.isEmpty()) {
                    try (java.io.InputStream in = getContext().getAssets().open("case.txt")) {
                        parsed = parseCaseFile(in);
                        Log.d(TAG, "从assets/case.txt读取成功");
                    } catch (Exception e) {
                        Log.e(TAG, "读取assets/case.txt失败", e);
                    }
                }

                if (parsed == null || parsed.isEmpty()) {
                    throw new RuntimeException("未能读取或解析到任何对话内容");
                }

                // 清空旧记录后批量导入
                int deleted = dbHelper.deleteAllMessages();
                Log.d(TAG, "已清空旧聊天记录: " + deleted);

                for (Message m : parsed) {
                    dbHelper.insertMessage(m);
                    importedCount++;
                }

                // 刷新UI列表
                java.util.List<Message> all = dbHelper.getAllMessages();
                final int countLocal = importedCount;
                requireActivity().runOnUiThread(() -> {
                    messageAdapter.clearMessages();
                    for (Message m : all) {
                        messageAdapter.addMessage(m);
                    }
                    messagesRecyclerView.smoothScrollToPosition(messageAdapter.getItemCount() - 1);
                    Toast.makeText(getContext(), "导入完成，共 " + countLocal + " 条", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "导入case.txt失败", e);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "导入失败: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                requireActivity().runOnUiThread(loading::dismiss);
            }
        }).start();
    }

    /**
     * 解析case.txt为消息列表，支持常见格式：
     * - 前缀："用户:"/"User:"/"Q:" 为用户；"AI:"/"助理:"/"A:" 为AI
     * - 若无前缀，则延续上一条说话人；若仍未知则默认用户
     */
    private java.util.List<Message> parseCaseFile(java.io.InputStream in) throws java.io.IOException {
        java.util.List<Message> result = new java.util.ArrayList<>();

        // 完整读取文本
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        String fullText = sb.toString().trim();

        // 1) 尝试严格JSON解析：{ "messages": [ {"role":"user|assistant","content":"..."} ] }
        try {
            com.google.gson.JsonElement root = new com.google.gson.JsonParser().parse(fullText);
            com.google.gson.JsonArray arr = null;
            if (root.isJsonObject() && root.getAsJsonObject().has("messages")) {
                arr = root.getAsJsonObject().getAsJsonArray("messages");
            } else if (root.isJsonArray()) {
                arr = root.getAsJsonArray();
            }
            if (arr != null) {
                long baseTs = System.currentTimeMillis();
                int idx = 0;
                for (com.google.gson.JsonElement el : arr) {
                    if (!el.isJsonObject()) continue;
                    com.google.gson.JsonObject obj = el.getAsJsonObject();
                    String role = obj.has("role") ? obj.get("role").getAsString() : "user";
                    String content = obj.has("content") && !obj.get("content").isJsonNull() ? obj.get("content").getAsString() : null;
                    if (content == null || content.trim().isEmpty()) continue;
                    boolean isAi = "assistant".equalsIgnoreCase(role) || "ai".equalsIgnoreCase(role);
                    Message m = new Message(content.trim(), isAi);
                    m.setTimestamp(baseTs + idx * 60_000L);
                    result.add(m);
                    idx++;
                }
            }
        } catch (Throwable ignore) {}

        if (!result.isEmpty()) {
            return result;
        }

        // 2) 容错：用正则从文本中提取 role/content 成对项（允许存在无效逗号/缺失大括号）
        try {
            java.util.regex.Pattern itemPattern = java.util.regex.Pattern.compile("\"role\"\\s*:\\s*\"(user|assistant|ai)\"[\\s\\S]*?\"content\"\\s*:\\s*\"(.*?)\"", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = itemPattern.matcher(fullText);
            long baseTs = System.currentTimeMillis();
            int idx = 0;
            while (matcher.find()) {
                String role = matcher.group(1);
                String content = matcher.group(2);
                if (content == null || content.trim().isEmpty()) continue;
                boolean isAi = "assistant".equalsIgnoreCase(role) || "ai".equalsIgnoreCase(role);
                Message m = new Message(content.trim(), isAi);
                m.setTimestamp(baseTs + idx * 60_000L);
                result.add(m);
                idx++;
            }
        } catch (Throwable ignore) {}

        if (!result.isEmpty()) {
            return result;
        }

        // 3) 回退到行前缀解析，同时过滤JSON结构符号行
        java.io.BufferedReader reader2 = new java.io.BufferedReader(new java.io.StringReader(fullText));
        boolean lastIsAi = false;
        String line2;
        long base = System.currentTimeMillis();
        int idx2 = 0;
        while ((line2 = reader2.readLine()) != null) {
            String s = line2.trim();
            if (s.isEmpty()) continue;

            // 过滤明显的JSON结构行
            if (s.equals("{") || s.equals("}") || s.equals("[") || s.equals("]") || s.equals(",") || s.endsWith(",") || s.startsWith("\"messages\"")) {
                continue;
            }

            boolean isAi = lastIsAi;
            String content = s;
            String normalized = s.replace('：', ':');

            if (normalized.startsWith("AI:")) { isAi = true; content = normalized.substring(3).trim(); }
            else if (normalized.startsWith("A:")) { isAi = true; content = normalized.substring(2).trim(); }
            else if (normalized.startsWith("助理:")) { isAi = true; content = normalized.substring(3).trim(); }
            else if (normalized.startsWith("咨询师:")) { isAi = true; content = normalized.substring(4).trim(); }
            else if (normalized.startsWith("用户:")) { isAi = false; content = normalized.substring(3).trim(); }
            else if (normalized.startsWith("User:")) { isAi = false; content = normalized.substring(5).trim(); }
            else if (normalized.startsWith("Q:")) { isAi = false; content = normalized.substring(2).trim(); }
            else if (normalized.startsWith("U:")) { isAi = false; content = normalized.substring(2).trim(); }
            else if (normalized.startsWith("[AI]")) { isAi = true; content = normalized.substring(4).trim(); }
            else if (normalized.startsWith("[User]")) { isAi = false; content = normalized.substring(6).trim(); }

            // 若仍无前缀，则延续上一条（首条默认用户）
            if (content.equals(s)) {
                isAi = (idx2 == 0) ? false : lastIsAi;
            }

            Message m = new Message(content, isAi);
            m.setTimestamp(base + (idx2 * 60_000L));
            result.add(m);
            lastIsAi = isAi;
            idx2++;
        }

        return result;
    }
    
    /**
     * 检查模型状态并在需要时触发模型加载
     */
    private void checkAndLoadModel() {
        try {
            // 首先尝试获取ProfileFragment中的共享模型实例
            LLamaAPI profileSharedInstance = getSharedModelInstance();
            
            if (profileSharedInstance != null && profileSharedInstance.isModelLoaded()) {
                // 如果ProfileFragment中的模型已加载，直接使用
                sharedChatLlamaApi = profileSharedInstance;
                chatLlamaApi = sharedChatLlamaApi;
                Log.d(TAG, "使用ProfileFragment中已加载的模型实例");
                
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "模型已准备就绪，可以开始对话", Toast.LENGTH_SHORT).show();
                }
                return;
            }
            
            // 如果模型未加载，显示加载弹窗
            if (isAdded() && getContext() != null) {
                showModelLoadingDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "检查模型状态失败", e);
            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "检查模型状态失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * 从assets目录复制模型文件到应用内部存储
     */
    private void copyModelFromAssets(String assetFileName, String targetPath) throws IOException {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        
        try {
            inputStream = getContext().getAssets().open("models/" + assetFileName);
            outputStream = new FileOutputStream(targetPath);
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
                
                // 每复制10MB更新一次进度信息
                if (totalBytes % (10 * 1024 * 1024) == 0) {
                    final long currentBytes = totalBytes;
                    mainHandler.post(() -> {
                        if (isAdded() && getContext() != null) {
                            // 这里可以更新进度信息，但为了简化就不显示具体进度了
                        }
                    });
                }
            }
            
            Log.d(TAG, "成功复制模型文件: " + assetFileName + " (" + totalBytes + " bytes)");
            
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭输入流失败", e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "关闭输出流失败", e);
                }
            }
        }
    }
}
