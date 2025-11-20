package com.example.projectv3.fragment;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectv3.R;
import com.example.projectv3.adapter.UnifiedQuestionAdapter;
import com.example.projectv3.api.ApiClient;
import com.example.projectv3.api.UnifiedTestApi;
import com.example.projectv3.dto.unified.TestDetailDTO;
import com.example.projectv3.dto.unified.TestListDTO;
import com.example.projectv3.dto.unified.TestResultDTO;
import com.example.projectv3.dto.unified.TestSessionDTO;
import com.example.projectv3.dto.unified.TestSubmissionDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UnifiedTestFragment extends Fragment {

    private static final String ARG_TEST_ID = "arg_test_id";
    private static final String ARG_TEST_CODE = "arg_test_code";

    private TextView title;
    private RecyclerView recyclerView;
    private Button btnSubmit;
    private ProgressBar progress;

    private UnifiedQuestionAdapter adapter;
    private Long testId;
    private String testCode;
    private Long sessionId;
    private TestDetailDTO currentTest;

    public static UnifiedTestFragment newInstanceById(long id) {
        UnifiedTestFragment f = new UnifiedTestFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_TEST_ID, id);
        f.setArguments(b);
        return f;
    }

    public static UnifiedTestFragment newInstanceByCode(String code) {
        UnifiedTestFragment f = new UnifiedTestFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TEST_CODE, code);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_unified_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        title = view.findViewById(R.id.title);
        recyclerView = view.findViewById(R.id.recycler);
        btnSubmit = view.findViewById(R.id.btn_submit);
        progress = view.findViewById(R.id.progress);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UnifiedQuestionAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_TEST_ID)) {
                testId = getArguments().getLong(ARG_TEST_ID);
            }
            if (getArguments().containsKey(ARG_TEST_CODE)) {
                testCode = getArguments().getString(ARG_TEST_CODE);
            }
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setOnClickListener(v -> submit());

        loadTest();
    }

    private UnifiedTestApi api() {
        return ApiClient.getUnifiedTestApi();
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
    }

    private void loadTest() {
        setLoading(true);
        if (testId != null) {
            api().getTestDetails(testId).enqueue(new Callback<TestDetailDTO>() {
                @Override public void onResponse(Call<TestDetailDTO> call, Response<TestDetailDTO> response) {
                    setLoading(false);
                    if (response.isSuccessful() && response.body() != null) {
                        bindTest(response.body());
                    }
                }
                @Override public void onFailure(Call<TestDetailDTO> call, Throwable t) { setLoading(false);} 
            });
            return;
        }
        // 若未指定testId，按code或取第一个激活的
        api().getActiveTests().enqueue(new Callback<List<TestListDTO>>() {
            @Override public void onResponse(Call<List<TestListDTO>> call, Response<List<TestListDTO>> resp) {
                if (!resp.isSuccessful() || resp.body() == null || resp.body().isEmpty()) { setLoading(false); return; }
                List<TestListDTO> list = resp.body();
                Long id = null;
                if (!TextUtils.isEmpty(testCode)) {
                    for (TestListDTO t : list) {
                        if (testCode.equalsIgnoreCase(t.getCode())) { id = t.getId(); break; }
                    }
                }
                if (id == null) id = list.get(0).getId();
                testId = id;
                loadTest();
            }
            @Override public void onFailure(Call<List<TestListDTO>> call, Throwable t) { setLoading(false);} 
        });
    }

    private void bindTest(TestDetailDTO dto) {
        currentTest = dto;
        title.setText(dto.getName());
        adapter = new UnifiedQuestionAdapter(dto.getQuestions());
        recyclerView.setAdapter(adapter);

        requestSession(dto.getId(), false);
    }

    private void requestSession(long targetTestId, boolean showToast) {
        sessionId = null;
        setLoading(true);
        api().createTestSession(targetTestId).enqueue(new Callback<TestSessionDTO>() {
            @Override public void onResponse(Call<TestSessionDTO> call, Response<TestSessionDTO> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    sessionId = response.body().getId();
                    btnSubmit.setEnabled(true);
                    btnSubmit.setText("提交");
                    if (showToast && getContext() != null) {
                        Toast.makeText(getContext(), "已为您创建新的答题会话", Toast.LENGTH_SHORT).show();
                    }
                } else if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "无法创建会话：" + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<TestSessionDTO> call, Throwable t) {
                setLoading(false);
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "创建会话失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void submit() {
        if (sessionId == null) {
            if (getContext() != null) {
                android.widget.Toast.makeText(getContext(), "会话尚未创建，请稍后重试", android.widget.Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (adapter.getAnswers().isEmpty()) {
            if (getContext() != null) {
                Toast.makeText(getContext(), "请先作答后再提交", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        Map<Long, Long> map = adapter.getAnswers();
        TestSubmissionDTO submission = new TestSubmissionDTO();
        java.util.List<TestSubmissionDTO.AnswerDTO> answers = new java.util.ArrayList<>();
        for (Map.Entry<Long, Long> e : map.entrySet()) {
            TestSubmissionDTO.AnswerDTO a = new TestSubmissionDTO.AnswerDTO();
            a.setQuestionId(e.getKey());
            a.setOptionId(e.getValue());
            answers.add(a);
        }
        submission.setAnswers(answers);
        setLoading(true);
        api().submitAnswers(sessionId, submission).enqueue(new Callback<TestResultDTO>() {
            @Override public void onResponse(Call<TestResultDTO> call, Response<TestResultDTO> response) {
                setLoading(false);
                if (getContext() == null) return;
                if (response.isSuccessful() && response.body() != null) {
                    TestResultDTO r = response.body();
                    btnSubmit.setEnabled(false);
                    btnSubmit.setText("已提交");
                    new android.app.AlertDialog.Builder(getContext())
                            .setTitle("测试结果")
                            .setMessage("总分：" + r.getTotalScore() + (r.getLevel() != null ? "\n等级：" + r.getLevel() : ""))
                            .setPositiveButton("重新测试", (dialog, which) -> restartTest())
                            .setNegativeButton("关闭", null)
                            .show();
                } else {
                    String reason = response.code() == 400 ? "会话已结束，请重新开始测评" : String.valueOf(response.code());
                    android.widget.Toast.makeText(getContext(), "提交失败：" + reason, android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<TestResultDTO> call, Throwable t) {
                setLoading(false);
                if (getContext() != null) {
                    android.widget.Toast.makeText(getContext(), "提交出错：" + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                }
            } 
        });
    }

    private void restartTest() {
        if (currentTest == null) {
            return;
        }
        sessionId = null;
        if (adapter != null) {
            adapter.clearAnswers();
        }
        btnSubmit.setEnabled(false);
        btnSubmit.setText("准备中...");
        requestSession(currentTest.getId(), true);
    }
}

