package com.example.projectv3.fragment;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.projectv3.R;
import com.example.projectv3.adapter.TestManagementAdapter;
import com.example.projectv3.api.ApiClient;
import com.example.projectv3.dto.unified.TestCreateRequest;
import com.example.projectv3.dto.unified.TestListDTO;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

    public class TestManagementFragment extends Fragment implements TestManagementAdapter.OnActionListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private View emptyView;
    private MaterialButton createButton;
    private TestManagementAdapter adapter;
    private Long pendingImportTestId;
    private String pendingImportTestName;

    private final ActivityResultLauncher<String[]> importDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportUri);

    public static TestManagementFragment newInstance() {
        return new TestManagementFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = view.findViewById(R.id.adminSwipe);
        recyclerView = view.findViewById(R.id.adminRecyclerView);
        emptyView = view.findViewById(R.id.adminEmptyView);
        createButton = view.findViewById(R.id.btnCreateTest);

        adapter = new TestManagementAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.xiangzhang_primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadTests);

        createButton.setOnClickListener(v -> showCreateDialog());

        loadTests();
    }

    private void loadTests() {
        swipeRefreshLayout.setRefreshing(true);
        ApiClient.getUnifiedTestApi().getActiveTests().enqueue(new Callback<List<TestListDTO>>() {
            @Override
            public void onResponse(Call<List<TestListDTO>> call, Response<List<TestListDTO>> response) {
                swipeRefreshLayout.setRefreshing(false);
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<TestListDTO> list = response.body();
                    adapter.submitList(list);
                    emptyView.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    showToast("加载失败：" + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<TestListDTO>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                if (!isAdded()) return;
                showToast("加载失败：" + t.getMessage());
            }
        });
    }

    private void showCreateDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_test, null, false);
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("创建新问卷")
                .setView(dialogView)
                .setPositiveButton("创建", (d, which) -> {
                    TestCreateRequest request = new TestCreateRequest();
                    request.setCode(readInputText(dialogView, R.id.inputCode));
                    request.setName(readInputText(dialogView, R.id.inputName));
                    request.setCategory(readInputText(dialogView, R.id.inputCategory));
                    request.setDescription(readInputText(dialogView, R.id.inputDescription));
                    request.setTotalScoreThresholds(readInputText(dialogView, R.id.inputThresholds));
                    createTest(request);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void createTest(TestCreateRequest request) {
        if (request.getCode().isEmpty() || request.getName().isEmpty()) {
            showToast("编码和名称不能为空");
            return;
        }
        ApiClient.getTestAdminApi().createTest(request).enqueue(new Callback<TestListDTO>() {
            @Override
            public void onResponse(Call<TestListDTO> call, Response<TestListDTO> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    showToast("创建成功");
                    loadTests();
                } else {
                    showToast("创建失败：" + response.code());
                }
            }

            @Override
            public void onFailure(Call<TestListDTO> call, Throwable t) {
                if (!isAdded()) return;
                showToast("创建失败：" + t.getMessage());
            }
        });
    }

    @Override
    public void onPreview(TestListDTO dto) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment, UnifiedTestFragment.newInstanceById(dto.getId()));
        transaction.addToBackStack(null);
        transaction.commit();
    }

    @Override
    public void onImport(TestListDTO dto) {
        pendingImportTestId = dto.getId();
        pendingImportTestName = dto.getName();
        importDocumentLauncher.launch(new String[]{"text/*", "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"});
    }

    @Override
    public void onExport(TestListDTO dto) {
        ApiClient.getTestAdminApi().exportResponses(dto.getId()).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    saveExportFile(response.body(), dto);
                } else {
                    showToast("导出失败：" + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (!isAdded()) return;
                showToast("导出失败：" + t.getMessage());
            }
        });
    }

    @Override
    public void onDeactivate(TestListDTO dto) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("下线问卷")
                .setMessage("确认将该问卷下线并删除？删除后不可恢复。")
                .setPositiveButton("确认", (d, which) -> {
                    ApiClient.getTestAdminApi().offlineTest(dto.getId()).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            if (!isAdded()) return;
                            if (response.isSuccessful()) {
                                showToast("已删除");
                                loadTests();
                            } else {
                                showToast("下线失败：" + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            if (!isAdded()) return;
                            showToast("下线失败：" + t.getMessage());
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void handleImportUri(Uri uri) {
        if (uri == null || pendingImportTestId == null) {
            return;
        }
        try {
            ContentResolver resolver = requireContext().getContentResolver();
            String mime = resolver.getType(uri);
            if (mime == null) mime = "text/csv";
            byte[] bytes = readAllBytes(resolver.openInputStream(uri));
            String filename = resolveDisplayName(uri);
            if (filename == null) {
                filename = pendingImportTestName + ".csv";
            }
            RequestBody body = RequestBody.create(MediaType.parse(mime), bytes);
            MultipartBody.Part part = MultipartBody.Part.createFormData("file", filename, body);

            ApiClient.getTestAdminApi().importQuestions(pendingImportTestId, part).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful()) {
                        showToast("导入成功");
                        loadTests();
                    } else {
                        showToast("导入失败：" + response.code());
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    if (!isAdded()) return;
                    showToast("导入失败：" + t.getMessage());
                }
            });
        } catch (IOException e) {
            showToast("读取文件失败：" + e.getMessage());
        } finally {
            pendingImportTestId = null;
            pendingImportTestName = null;
        }
    }

    private String resolveDisplayName(Uri uri) {
        try (Cursor cursor = requireContext().getContentResolver()
                .query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex);
                }
            }
        }
        return null;
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new IOException("无法读取文件");
        }
        try (InputStream in = inputStream; ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int n;
            while ((n = in.read(data)) != -1) {
                buffer.write(data, 0, n);
            }
            return buffer.toByteArray();
        }
    }

    private void saveExportFile(ResponseBody body, TestListDTO dto) {
        try {
            File dir = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = requireContext().getFilesDir();
            File exports = new File(dir, "exports");
            if (!exports.exists()) exports.mkdirs();
            String safeName = dto.getCode() != null ? dto.getCode() : String.valueOf(dto.getId());
            File file = new File(exports, safeName + "-responses.csv");
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(body.bytes());
            }
            showToast("已导出到：" + file.getAbsolutePath());
        } catch (IOException e) {
            showToast("保存导出文件失败：" + e.getMessage());
        }
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private String readInputText(View root, int id) {
        TextInputEditText input = root.findViewById(id);
        if (input != null && input.getText() != null) {
            return input.getText().toString().trim();
        }
        return "";
    }
}
