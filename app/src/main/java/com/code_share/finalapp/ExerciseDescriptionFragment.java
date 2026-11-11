package com.code_share.finalapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

import java.util.List;

public class ExerciseDescriptionFragment extends Fragment {
    private Exercise exercise;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise_description, container, false);
        Bundle args = getArguments();
        exercise = args == null ? null : args.getParcelable("exercise");
        if (exercise == null) {
            throw new IllegalStateException("Exercise details are required.");
        }

        ((TextView) view.findViewById(R.id.description_title))
                .setText(valueOrFallback(exercise.getTitle(), "Python exercise"));
        ((TextView) view.findViewById(R.id.description_difficulty))
                .setText(valueOrFallback(exercise.getDifficulty(), "Practice"));
        ((TextView) view.findViewById(R.id.description_category))
                .setText(valueOrFallback(exercise.getCategory(), "General"));
        ((TextView) view.findViewById(R.id.description_body))
                .setText(valueOrFallback(exercise.getDescription(), "No problem description provided."));
        ((TextView) view.findViewById(R.id.description_hints))
                .setText(formatHints(exercise.getHints()));

        int testCount = exercise.getTestCases() == null ? 0 : exercise.getTestCases().size();
        TextView tests = view.findViewById(R.id.description_tests);
        tests.setText(testCount == 0
                ? "No automatic test cases. Run your code to see its output."
                : testCount + " test cases will check your solution when you run it.");

        MaterialButton startCodingButton = view.findViewById(R.id.start_coding_button);
        startCodingButton.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            if (parent instanceof ExerciseWorkspaceFragment) {
                ((ExerciseWorkspaceFragment) parent).showEditor();
            }
        });
        return view;
    }

    private String formatHints(List<String> hints) {
        if (hints == null || hints.isEmpty()) {
            return "No hints for this exercise.";
        }
        StringBuilder text = new StringBuilder();
        for (String hint : hints) {
            if (TextUtils.isEmpty(hint)) continue;
            if (text.length() > 0) text.append("\n\n");
            text.append("-  ").append(hint);
        }
        return text.length() == 0 ? "No hints for this exercise." : text.toString();
    }

    private String valueOrFallback(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
