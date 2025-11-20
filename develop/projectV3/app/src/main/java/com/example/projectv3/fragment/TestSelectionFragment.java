package com.example.projectv3.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.projectv3.R;
import com.example.projectv3.adapter.TestCardAdapter;
import com.example.projectv3.api.ApiClient;
import com.example.projectv3.dto.unified.TestListDTO;

import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestSelectionFragment extends Fragment {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private View emptyView;
    private TestCardAdapter adapter;

    public static TestSelectionFragment newInstance() {
        return new TestSelectionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_test_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        recyclerView = view.findViewById(R.id.testRecyclerView);
        emptyView = view.findViewById(R.id.emptyView);

        adapter = new TestCardAdapter(this::openTest);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        swipeRefreshLayout.setColorSchemeResources(R.color.xiangzhang_primary);
        swipeRefreshLayout.setOnRefreshListener(this::loadTests);

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
                    showError("加载失败：" + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<TestListDTO>> call, Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                if (!isAdded()) return;
                showError("加载失败：" + t.getMessage());
            }
        });
    }

    private void openTest(TestListDTO dto) {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        if (dto.getId() != null) {
            transaction.replace(R.id.nav_host_fragment, UnifiedTestFragment.newInstanceById(dto.getId()));
        } else if (dto.getCode() != null) {
            transaction.replace(R.id.nav_host_fragment, UnifiedTestFragment.newInstanceByCode(dto.getCode()));
        } else {
            showError("无效的问卷信息");
            return;
        }
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
        adapter.submitList(Collections.emptyList());
        emptyView.setVisibility(View.VISIBLE);
    }
}