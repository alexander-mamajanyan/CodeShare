package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class PracticeProgressFragment extends Fragment {
    private DatabaseReference solvedExercisesRef;
    private ValueEventListener progressListener;
    private TextView solvedCount;
    private TextView streakCount;
    private TextView topics;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_practice_progress, container, false);
        MaterialToolbar toolbar = view.findViewById(R.id.progress_toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        solvedCount = view.findViewById(R.id.progress_solved_count);
        streakCount = view.findViewById(R.id.progress_streak_count);
        topics = view.findViewById(R.id.progress_topics);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Sign in to view your progress.", Toast.LENGTH_SHORT).show();
            return view;
        }

        solvedExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("practiceProgress")
                .child("solved");
        progressListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                Map<String, Integer> topicCounts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                Set<Long> solvedDays = new TreeSet<>();
                for (DataSnapshot solvedExercise : snapshot.getChildren()) {
                    String category = solvedExercise.child("category").getValue(String.class);
                    topicCounts.put(category == null || category.trim().isEmpty() ? "Other" : category,
                            topicCounts.getOrDefault(category == null || category.trim().isEmpty()
                                    ? "Other" : category, 0) + 1);
                    Long completedAt = solvedExercise.child("completedAt").getValue(Long.class);
                    if (completedAt != null) {
                        solvedDays.add(dayKey(completedAt));
                    }
                }

                solvedCount.setText(String.valueOf(snapshot.getChildrenCount()));
                streakCount.setText(String.valueOf(currentStreak(solvedDays)));
                topics.setText(formatTopics(topicCounts));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Couldn't load learning progress: " + error.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        solvedExercisesRef.addValueEventListener(progressListener);
        return view;
    }

    private static String formatTopics(Map<String, Integer> topicCounts) {
        if (topicCounts.isEmpty()) {
            return "Solve an exercise to start your learning history.";
        }

        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, Integer> topic : topicCounts.entrySet()) {
            if (text.length() > 0) text.append('\n');
            text.append(topic.getKey()).append("  ·  ")
                    .append(topic.getValue())
                    .append(topic.getValue() == 1 ? " exercise" : " exercises");
        }
        return text.toString();
    }

    private static int currentStreak(Set<Long> solvedDays) {
        if (solvedDays.isEmpty()) return 0;

        Calendar cursor = Calendar.getInstance();
        normalizeDay(cursor);
        long today = dayKey(cursor.getTimeInMillis());
        if (!solvedDays.contains(today)) {
            cursor.add(Calendar.DAY_OF_YEAR, -1);
            if (!solvedDays.contains(dayKey(cursor.getTimeInMillis()))) return 0;
        }

        int streak = 0;
        while (solvedDays.contains(dayKey(cursor.getTimeInMillis()))) {
            streak++;
            cursor.add(Calendar.DAY_OF_YEAR, -1);
        }
        return streak;
    }

    private static long dayKey(long timestamp) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(timestamp);
        return date.get(Calendar.YEAR) * 1000L + date.get(Calendar.DAY_OF_YEAR);
    }

    private static void normalizeDay(Calendar date) {
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
    }

    @Override
    public void onDestroyView() {
        if (solvedExercisesRef != null && progressListener != null) {
            solvedExercisesRef.removeEventListener(progressListener);
        }
        solvedCount = null;
        streakCount = null;
        topics = null;
        super.onDestroyView();
    }
}
