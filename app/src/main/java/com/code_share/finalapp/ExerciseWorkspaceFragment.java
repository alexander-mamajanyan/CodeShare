package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ExerciseWorkspaceFragment extends Fragment {
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ((MainActivity) requireActivity()).setBottomNavigationVisible(false);
        View view = inflater.inflate(R.layout.fragment_exercise_workspace, container, false);
        Bundle args = getArguments();
        Exercise exercise = args == null ? null : args.getParcelable("exercise");
        if (exercise == null) {
            throw new IllegalStateException("Exercise details are required.");
        }

        MaterialToolbar toolbar = view.findViewById(R.id.workspace_toolbar);
        toolbar.setTitle(exercise.getTitle());
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        viewPager = view.findViewById(R.id.workspace_pager);
        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new ExerciseWorkspacePagerAdapter(this, exercise));

        TabLayout tabs = view.findViewById(R.id.workspace_tabs);
        new TabLayoutMediator(tabs, viewPager, (tab, position) ->
                tab.setText(position == 0 ? "Problem" : "Code")).attach();
        return view;
    }

    @Override
    public void onDestroyView() {
        if (isAdded()) {
            ((MainActivity) requireActivity()).setBottomNavigationVisible(true);
        }
        viewPager = null;
        super.onDestroyView();
    }

    public void showEditor() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
    }
}
