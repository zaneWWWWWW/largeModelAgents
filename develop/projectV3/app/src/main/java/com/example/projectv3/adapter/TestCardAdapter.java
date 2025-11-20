package com.example.projectv3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectv3.R;
import com.example.projectv3.dto.unified.TestListDTO;

import java.util.ArrayList;
import java.util.List;

public class TestCardAdapter extends RecyclerView.Adapter<TestCardAdapter.TestViewHolder> {

    public interface OnTestClickListener {
        void onTestClick(TestListDTO dto);
    }

    private final List<TestListDTO> data = new ArrayList<>();
    private final OnTestClickListener listener;

    public TestCardAdapter(OnTestClickListener listener) {
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
    public TestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_test_card, parent, false);
        return new TestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TestViewHolder holder, int position) {
        TestListDTO item = data.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class TestViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView category;
        private final TextView description;

        TestViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.testName);
            category = itemView.findViewById(R.id.testCategory);
            description = itemView.findViewById(R.id.testDescription);
        }

        void bind(TestListDTO item, OnTestClickListener listener) {
            name.setText(item.getName());
            category.setText(item.getCategory() != null ? item.getCategory() : "未分类");
            description.setText(item.getDescription() != null ? item.getDescription() : "暂无描述");
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTestClick(item);
                }
            });
        }
    }
}

