package com.code_share.finalapp;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ExerciseWorkspacePagerAdapter extends FragmentStateAdapter {
    private final Exercise exercise;

    public ExerciseWorkspacePagerAdapter(@NonNull Fragment fragment, @NonNull Exercise exercise) {
        super(fragment);
        this.exercise = exercise;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Bundle args = new Bundle();
        args.putParcelable("exercise", exercise);

        if (position == 0) {
            ExerciseDescriptionFragment fragment = new ExerciseDescriptionFragment();
            fragment.setArguments(args);
            return fragment;
        }
        if (position == 1) {
            args.putBoolean("embedded", true);
            CodeEditorFragment fragment = new CodeEditorFragment();
            fragment.setArguments(args);
            return fragment;
        }
        throw new IllegalArgumentException("Invalid exercise page: " + position);
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
