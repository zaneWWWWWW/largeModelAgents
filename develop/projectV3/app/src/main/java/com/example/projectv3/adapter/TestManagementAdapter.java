package com.example.projectv3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectv3.R;
import com.example.projectv3.dto.unified.TestListDTO;

import java.util.ArrayList;
import java.util.List;

public class TestManagementAdapter extends RecyclerView.Adapter<TestManagementAdapter.AdminViewHolder> {

    public interface OnActionListener {
        void onPreview(TestListDTO dto);
        void onImport(TestListDTO dto);
        void onExport(TestListDTO dto);
        void onDeactivate(TestListDTO dto);
    }

    private final List<TestListDTO> data = new ArrayList<>();
    private final OnActionListener listener;

    public TestManagementAdapter(OnActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<TestListDTO> list) {
        data.clear();
        if (list != null) {
            data.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_admin, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        holder.bind(data.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView category;
        private final TextView description;
        private final Button importBtn;
        private final Button exportBtn;
        private final Button deactivateBtn;

        AdminViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.adminTestName);
            category = itemView.findViewById(R.id.adminTestCategory);
            description = itemView.findViewById(R.id.adminTestDescription);
            importBtn = itemView.findViewById(R.id.btnImport);
            exportBtn = itemView.findViewById(R.id.btnExport);
            deactivateBtn = itemView.findViewById(R.id.btnDeactivate);
        }

        void bind(TestListDTO item, OnActionListener listener) {
            name.setText(item.getName());
            category.setText(item.getCategory() != null ? item.getCategory() : "未分类");
            description.setText(item.getDescription() != null ? item.getDescription() : "暂无描述");

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onPreview(item);
            });
            importBtn.setOnClickListener(v -> {
                if (listener != null) listener.onImport(item);
            });
            exportBtn.setOnClickListener(v -> {
                if (listener != null) listener.onExport(item);
            });
            deactivateBtn.setOnClickListener(v -> {
                if (listener != null) listener.onDeactivate(item);
            });
        }
    }
}

