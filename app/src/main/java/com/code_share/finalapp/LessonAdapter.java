package com.code_share.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LessonAdapter extends RecyclerView.Adapter<LessonAdapter.LessonViewHolder> {
    private List<Lesson> lessons;
    private OnLessonClickListener listener;

    public interface OnLessonClickListener {
        void onLessonClick(Lesson lesson);
    }

    public LessonAdapter(List<Lesson> lessons, OnLessonClickListener listener) {
        this.lessons = lessons;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LessonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lesson, parent, false);
        return new LessonViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LessonViewHolder holder, int position) {
        Lesson lesson = lessons.get(position);
        holder.titleView.setText(lesson.getTitle());
        holder.descriptionView.setText(lesson.getDescription());
        holder.topicsView.setText(lesson.getTopics());
        
        // Set item appearance based on availability
        if (lesson.isAvailable()) {
            holder.itemView.setAlpha(1.0f);
            holder.itemView.setEnabled(true);
        } else {
            holder.itemView.setAlpha(0.5f);
            holder.itemView.setEnabled(false);
        }

        holder.itemView.setOnClickListener(v -> {
            if (lesson.isAvailable() && listener != null) {
                listener.onLessonClick(lesson);
            }
        });
    }

    @Override
    public int getItemCount() {
        return lessons.size();
    }

    static class LessonViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;
        TextView descriptionView;
        TextView topicsView;

        LessonViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.lesson_item_title);
            descriptionView = itemView.findViewById(R.id.lesson_item_description);
            topicsView = itemView.findViewById(R.id.lesson_item_topics);
        }
    }
}
