package com.example.projectv3.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectv3.R;
import com.example.projectv3.dto.unified.TestDetailDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnifiedQuestionAdapter extends RecyclerView.Adapter<UnifiedQuestionAdapter.QVH> {

    private final List<TestDetailDTO.QuestionDTO> questions;
    // questionId -> optionId
    private final Map<Long, Long> answers = new HashMap<>();

    public UnifiedQuestionAdapter(List<TestDetailDTO.QuestionDTO> questions) {
        this.questions = questions;
    }

    public Map<Long, Long> getAnswers() {
        return answers;
    }

    public void clearAnswers() {
        answers.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public QVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_unified_question, parent, false);
        return new QVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull QVH holder, int position) {
        TestDetailDTO.QuestionDTO q = questions.get(position);
        holder.stem.setText((position + 1) + ". " + q.getStem());

        holder.radioGroup.removeAllViews();
        if (q.getOptions() != null) {
            for (TestDetailDTO.OptionDTO opt : q.getOptions()) {
                RadioButton rb = new RadioButton(holder.itemView.getContext());
                rb.setText(opt.getLabel());
                rb.setId(View.generateViewId());
                rb.setTag(opt.getId());
                holder.radioGroup.addView(rb);

                Long selected = answers.get(q.getId());
                if (selected != null && selected.equals(opt.getId())) {
                    rb.setChecked(true);
                }
            }
        }

        holder.radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View v = group.findViewById(checkedId);
            if (v != null && v.getTag() instanceof Long) {
                answers.put(q.getId(), (Long) v.getTag());
            }
        });
    }

    @Override
    public int getItemCount() {
        return questions == null ? 0 : questions.size();
    }

    static class QVH extends RecyclerView.ViewHolder {
        TextView stem;
        RadioGroup radioGroup;
        QVH(@NonNull View itemView) {
            super(itemView);
            stem = itemView.findViewById(R.id.text_stem);
            radioGroup = itemView.findViewById(R.id.radio_group);
        }
    }
}

