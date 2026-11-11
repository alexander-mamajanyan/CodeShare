package com.code_share.finalapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class LessonFragment extends Fragment implements LessonAdapter.OnLessonClickListener {

    private RecyclerView lessonList;
    private List<Lesson> lessons;
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lesson, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "Please log in to view lessons", Toast.LENGTH_SHORT).show();
            return view;
        }
        String userId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        // Initialize RecyclerView
        lessonList = view.findViewById(R.id.lesson_list);
        lessonList.setLayoutManager(new LinearLayoutManager(getContext()));

        // Create lessons list
        lessons = createLessonList();

        // Set up adapter
        LessonAdapter adapter = new LessonAdapter(lessons, this);
        lessonList.setAdapter(adapter);

        // Check ru.myitschool.finalproject.User's progress and update ru.myitschool.finalproject.Lesson availability
        checkUserProgress();

        return view;
    }

    private List<com.code_share.finalapp.Lesson> createLessonList() {
        List<Lesson> lessons = new ArrayList<>();
        
        // Basic Data Structures
        lessons.add(new com.code_share.finalapp.Lesson(
            "Introduction to DSA",
            "Get started with Data Structures and Algorithms",
            "Learn the fundamentals of data structures and algorithms, including:\n\n" +
            "• What are data structures?\n" +
            "• Why study algorithms?\n" +
            "• Time and space complexity\n" +
            "• Basic algorithm analysis\n" +
            "• Problem-solving approaches",
            "lessons/lesson1.html",
            true,  // First ru.myitschool.finalproject.Lesson is always available
            "DSA Basics • Complexity Analysis • Problem Solving"
        ));

        lessons.add(new Lesson(
            "Arrays and Strings",
            "Master array and string manipulation",
            "Learn about fundamental data structures:\n\n" +
            "• Array operations and manipulation\n" +
            "• String algorithms\n" +
            "• 2D arrays and matrices\n" +
            "• Common array/string problems\n" +
            "• Time complexity analysis",
            "lessons/lesson2.html",
            false,
            "Arrays • Strings • Matrix Operations"
        ));

        lessons.add(new Lesson(
            "Linked Lists",
            "Understanding linked data structures",
            "Explore linked list implementations:\n\n" +
            "• Singly linked lists\n" +
            "• Doubly linked lists\n" +
            "• Circular linked lists\n" +
            "• Common linked list problems\n" +
            "• Memory management",
            "lessons/lesson3.html",
            false,
            "Linked Lists • Memory Management • List Operations"
        ));

        // Advanced Data Structures
        lessons.add(new Lesson(
            "Stacks and Queues",
            "LIFO and FIFO data structures",
            "Learn about stack and queue implementations:\n\n" +
            "• Stack operations (push, pop)\n" +
            "• Queue operations (enqueue, dequeue)\n" +
            "• Applications in real-world problems\n" +
            "• Implementation using arrays and linked lists\n" +
            "• Common interview problems",
            "lessons/lesson4.html",
            false,
            "Stacks • Queues • LIFO • FIFO"
        ));

        lessons.add(new Lesson(
            "Trees and Graphs",
            "Hierarchical and network data structures",
            "Master tree and graph data structures:\n\n" +
            "• Binary trees and BST\n" +
            "• Tree traversals\n" +
            "• Graph representations\n" +
            "• Graph algorithms (DFS, BFS)\n" +
            "• Common tree/graph problems",
            "lessons/lesson5.html",
            false,
            "Trees • Graphs • BST • Traversals"
        ));

        lessons.add(new Lesson(
            "Hash Tables",
            "Efficient key-value storage",
            "Learn about hash table implementations:\n\n" +
            "• Hash functions\n" +
            "• Collision resolution\n" +
            "• Load factor and rehashing\n" +
            "• Applications and use cases\n" +
            "• Time complexity analysis",
            "lessons/lesson6.html",
            false,
            "Hash Tables • Hash Functions • Collision Handling"
        ));

        // Algorithm Design
        lessons.add(new com.code_share.finalapp.Lesson(
            "Sorting Algorithms",
            "Efficient data organization",
            "Master various sorting algorithms:\n\n" +
            "• Bubble, Selection, Insertion sort\n" +
            "• Merge and Quick sort\n" +
            "• Heap sort\n" +
            "• Time complexity comparison\n" +
            "• Space complexity analysis",
            "lessons/lesson7.html",
            false,
            "Sorting • Time Complexity • Space Complexity"
        ));

        lessons.add(new com.code_share.finalapp.Lesson(
            "Searching Algorithms",
            "Finding data efficiently",
            "Learn about searching techniques:\n\n" +
            "• Linear search\n" +
            "• Binary search\n" +
            "• Depth-first search\n" +
            "• Breadth-first search\n" +
            "• Applications in real problems",
            "lessons/lesson8.html",
            false,
            "Searching • DFS • BFS • Binary Search"
        ));

        // Advanced Algorithms
        lessons.add(new com.code_share.finalapp.Lesson(
            "Dynamic Programming",
            "Solving complex problems efficiently",
            "Master dynamic programming:\n\n" +
            "• Memoization and tabulation\n" +
            "• State transitions\n" +
            "• Common DP problems\n" +
            "• Optimization techniques\n" +
            "• Space optimization",
            "lessons/lesson9.html",
            false,
            "Dynamic Programming • Memoization • Optimization"
        ));

        lessons.add(new com.code_share.finalapp.Lesson(
            "Greedy Algorithms",
            "Making optimal choices",
            "Learn about greedy algorithms:\n\n" +
            "• Greedy choice property\n" +
            "• Optimal substructure\n" +
            "• Common greedy problems\n" +
            "• When to use greedy approach\n" +
            "• Limitations and pitfalls",
            "lessons/lesson10.html",
            false,
            "Greedy Algorithms • Optimization • Problem Solving"
        ));

        return lessons;
    }

    private void checkUserProgress() {
        if (userRef == null) {
            return;
        }

        userRef.child("completed_lessons").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> completedLessons = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot lessonSnapshot : dataSnapshot.getChildren()) {
                        completedLessons.add(lessonSnapshot.getKey());
                    }
                }

                // Update ru.myitschool.finalproject.Lesson availability based on completion
                for (int i = 0; i < lessons.size(); i++) {
                    Lesson lesson = lessons.get(i);
                    if (i == 0) {
                        lesson.setAvailable(true);  // First ru.myitschool.finalproject.Lesson is always available
                    } else {
                        // A ru.myitschool.finalproject.Lesson is available if the previous ru.myitschool.finalproject.Lesson is completed
                        String previousLessonTitle = lessons.get(i - 1).getTitle();
                        lesson.setAvailable(completedLessons.contains(previousLessonTitle));
                    }
                }

                // Notify adapter of changes
                if (lessonList.getAdapter() != null) {
                    lessonList.getAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Error checking progress", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onLessonClick(Lesson lesson) {
        if (!lesson.isAvailable()) {
            Toast.makeText(getContext(), "Complete previous lessons first!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create new instance of LessonContentFragment with Lesson data
        LessonContentFragment contentFragment = new LessonContentFragment();
        Bundle args = new Bundle();
        args.putString("title", lesson.getTitle());
        args.putString("content", lesson.getContent());
        args.putString("html_file", lesson.getHtmlFile());
        contentFragment.setArguments(args);

        // Replace current fragment with content fragment
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, contentFragment)
                .addToBackStack(null)  // Add to back stack so ru.myitschool.finalproject.User can return
                .commit();
    }
} 





