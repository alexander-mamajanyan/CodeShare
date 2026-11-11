package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class PractiseFragment extends Fragment {
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ExercisePagerAdapter pagerAdapter;
    private FloatingActionButton createExerciseFab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_practise, container, false);
        
        viewPager = view.findViewById(R.id.view_pager);
        tabLayout = view.findViewById(R.id.tab_layout);
        createExerciseFab = view.findViewById(R.id.create_exercise_fab);
        
        pagerAdapter = new ExercisePagerAdapter(requireActivity());
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Official");
                    break;
                case 1:
                    tab.setText("Community");
                    break;
            }
        }).attach();

        // Show FAB only in Community tab
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                createExerciseFab.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
            }
        });

        // Set up FAB click listener
        createExerciseFab.setOnClickListener(v -> {
            CreateExerciseFragment fragment = new CreateExerciseFragment();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
        });
        
        return view;
    }
}






