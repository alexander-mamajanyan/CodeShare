package com.code_share.finalapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OfficialExercisesFragment extends Fragment {
    private static final String TAG = "OfficialExercises";
    private RecyclerView recyclerView;
    private ExerciseAdapter adapter;
    private List<com.code_share.finalapp.Exercise> exercises;
    private DatabaseReference officialExercisesRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exercises = new ArrayList<>();
        officialExercisesRef = FirebaseDatabase.getInstance().getReference("official_exercises");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_official_exercises, container, false);
        
        // Initialize RecyclerView
        recyclerView = view.findViewById(R.id.exercise_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize adapter
        adapter = new ExerciseAdapter(exercises, this::onExerciseClick);
        recyclerView.setAdapter(adapter);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadExercises();
    }

    private void loadExercises() {
        Log.d(TAG, "Starting to load exercises...");
        
        // Create all exercises in memory first
        exercises.clear();
        
        // Create Easy exercises (1-5)
        for (int i = 1; i <= 5; i++) {
            Exercise exercise = createEasyExercise(i);
            exercises.add(exercise);
            Log.d(TAG, "Created easy Exercise: " + exercise.getTitle());
        }
        
        // Create Medium exercises (1-2)
        for (int i = 1; i <= 2; i++) {
            Exercise exercise = createMediumExercise(i);
            exercises.add(exercise);
            Log.d(TAG, "Created medium Exercise: " + exercise.getTitle());
        }
        
        // Create Hard exercises (1-2)
        for (int i = 1; i <= 2; i++) {
            Exercise exercise = createHardExercise(i);
            exercises.add(exercise);
            Log.d(TAG, "Created hard Exercise: " + exercise.getTitle());
        }
        
        Log.d(TAG, "Total exercises created: " + exercises.size());
        
        // Sort exercises by difficulty
        exercises.sort((e1, e2) -> {
            String d1 = e1.getDifficulty();
            String d2 = e2.getDifficulty();
            if (d1.equals(d2)) return 0;
            if (d1.equals("Easy")) return -1;
            if (d2.equals("Easy")) return 1;
            if (d1.equals("Medium")) return -1;
            return 1;
        });
        
        // Update UI
        adapter.notifyDataSetChanged();
        
        // Save to Firebase in background
        saveToFirebase();
    }

    private void saveToFirebase() {
        Log.d(TAG, "Saving exercises to Firebase...");
        
        // Clear existing exercises in Firebase
        officialExercisesRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Cleared existing Firebase exercises");
                
                // Save all exercises
                for (Exercise exercise : exercises) {
                    String key = officialExercisesRef.push().getKey();
                    if (key != null) {
                        officialExercisesRef.child(key).setValue(exercise)
                            .addOnSuccessListener(aVoid -> 
                                Log.d(TAG, "Saved Exercise to Firebase: " + exercise.getTitle()))
                            .addOnFailureListener(e -> 
                                Log.e(TAG, "Failed to save Exercise: " + exercise.getTitle()));
                    }
                }
            } else {
                Log.e(TAG, "Failed to clear Firebase exercises");
            }
        });
    }

    private void onExerciseClick(Exercise exercise) {
        CodeEditorFragment fragment = new CodeEditorFragment();
        Bundle args = new Bundle();
        args.putParcelable("Exercise", exercise);
        fragment.setArguments(args);
        
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private Exercise createEasyExercise(int number) {
        String title, description, initialCode, solutionMethodName;
        ArrayList<String> hints = new ArrayList<>();
        ArrayList<Exercise.TestCase> testCases = new ArrayList<>();
        
        switch (number) {
            case 1:
                title = "Hello World";
                description = "Write a function called Hello_World that returns the string 'Hello, World!'";
                initialCode = "def Hello_World():\n    # Write your code here\n    pass";
                solutionMethodName = "Hello_World";
                hints.add("Use string literals");
                hints.add("Remember to use quotes");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList(), "Hello, World!"));
                break;
                
            case 2:
                title = "Sum Two Numbers";
                description = "Write a function called sum_two_numbers that takes two numbers and returns their sum";
                initialCode = "def sum_two_numbers(a, b):\n    # Write your code here\n    pass";
                solutionMethodName = "sum_two_numbers";
                hints.add("Use the + operator");
                hints.add("Make sure to return the result");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("5", "3"), "8"));
                testCases.add(new Exercise.TestCase(Arrays.asList("-1", "1"), "0"));
                testCases.add(new Exercise.TestCase(Arrays.asList("0", "0"), "0"));
                testCases.add(new Exercise.TestCase(Arrays.asList("10", "-5"), "5"));
                break;
                
            case 3:
                title = "Even or Odd";
                description = "Write a function called check_even_odd that takes a number and returns 'Even' if the number is even, 'Odd' if it's odd";
                initialCode = "def check_even_odd(number):\n    # Write your code here\n    pass";
                solutionMethodName = "check_even_odd";
                hints.add("Use the modulo operator %");
                hints.add("Use conditional expression");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("4"), "Even"));
                testCases.add(new Exercise.TestCase(Arrays.asList("7"), "Odd"));
                testCases.add(new Exercise.TestCase(Arrays.asList("0"), "Even"));
                testCases.add(new Exercise.TestCase(Arrays.asList("-3"), "Odd"));
                testCases.add(new Exercise.TestCase(Arrays.asList("-2"), "Even"));
                break;
                
            case 4:
                title = "Reverse String";
                description = "Write a function that takes a string and returns it reversed";
                initialCode = "def solution(text):\n    # Write your code here\n    pass";
                solutionMethodName = "solution";
                hints.add("Use string slicing");
                hints.add("Remember Python's slice syntax");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("'hello'"), "olleh"));
                testCases.add(new Exercise.TestCase(Arrays.asList("'python'"), "nohtyp"));
                testCases.add(new Exercise.TestCase(Arrays.asList("''"), ""));
                testCases.add(new Exercise.TestCase(Arrays.asList("'12345'"), "54321"));
                break;
                
            case 5:
                title = "Count Vowels";
                description = "Write a function that counts the number of vowels in a string";
                initialCode = "def solution(text):\n    # Write your code here\n    pass";
                solutionMethodName = "solution";
                hints.add("Use string methods");
                hints.add("Consider using a list comprehension");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("'hello'"), "2"));
                testCases.add(new Exercise.TestCase(Arrays.asList("'PYTHON'"), "1"));
                testCases.add(new Exercise.TestCase(Arrays.asList("'aeiou'"), "5"));
                testCases.add(new Exercise.TestCase(Arrays.asList("''"), "0"));
                testCases.add(new Exercise.TestCase(Arrays.asList("'xyz'"), "0"));
                break;
                
            default:
                title = "Easy Exercise " + number;
                description = "Complete this simple Python Exercise.";
                initialCode = "def solution():\n    # Your code here\n    pass";
                solutionMethodName = "solution";
                hints.add("Think about the basic Python concepts");
                hints.add("Check the documentation if needed");
                
                // Add default test case
                testCases.add(new Exercise.TestCase(Arrays.asList(), "True"));
        }
        
        String id = "easy_" + number;
        return new Exercise(id, title, description, "Easy", "Official", initialCode, testCases, hints, solutionMethodName, 5, "admin", "System");
    }

    private Exercise createMediumExercise(int number) {
        String title, description, initialCode, solutionMethodName;
        ArrayList<String> hints = new ArrayList<>();
        ArrayList<Exercise.TestCase> testCases = new ArrayList<>();
        
        switch (number) {
            case 1:
                title = "Fibonacci Number";
                description = "Write a function called fibonacci that takes a number n and returns the nth Fibonacci number.";
                initialCode = "def fibonacci(n):\n    # Write your code here\n    pass";
                solutionMethodName = "fibonacci";
                hints.add("Consider using a loop");
                hints.add("Remember: F(n) = F(n-1) + F(n-2)");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("0"), "0"));
                testCases.add(new Exercise.TestCase(Arrays.asList("1"), "1"));
                testCases.add(new Exercise.TestCase(Arrays.asList("2"), "1"));
                testCases.add(new Exercise.TestCase(Arrays.asList("5"), "5"));
                testCases.add(new Exercise.TestCase(Arrays.asList("10"), "55"));
                break;
                
            case 2:
                title = "List Comprehension";
                description = "Write a function called square_evens that takes a list of numbers and returns a new list containing squares of only even numbers.";
                initialCode = "def square_evens(numbers):\n    # Write your code here\n    pass";
                solutionMethodName = "square_evens";
                hints.add("Use list comprehension");
                hints.add("Check for even numbers using % 2");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("[1,2,3,4]"), "[4,16]"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[2,4,6,8]"), "[4,16,36,64]"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[1,3,5,7]"), "[]"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[]"), "[]"));
                break;
                
            default:
                title = "Medium Exercise " + number;
                description = "Complete this intermediate Python Exercise.";
                initialCode = "def solution():\n    # Your code here\n    pass";
                solutionMethodName = "solution";
                hints.add("Think about efficient algorithms");
                hints.add("Consider edge cases");
                
                // Add default test case
                testCases.add(new Exercise.TestCase(Arrays.asList(), "True"));
        }
        
        String id = "medium_" + number;
        return new Exercise(id, title, description, "Medium", "Official", initialCode, testCases, hints, solutionMethodName, 10, "admin", "System");
    }

    private Exercise createHardExercise(int number) {
        String title, description, initialCode, solutionMethodName;
        ArrayList<String> hints = new ArrayList<>();
        ArrayList<Exercise.TestCase> testCases = new ArrayList<>();
        
        switch (number) {
            case 1:
                title = "Binary Search";
                description = "Write a function called binary_search that takes a sorted list and a target value, returns the index if found or -1 if not found.";
                initialCode = "def binary_search(arr, target):\n    # Write your code here\n    pass";
                solutionMethodName = "binary_search";
                hints.add("Use two pointers: left and right");
                hints.add("Compare with middle element");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("[1,3,5,7,9]", "5"), "2"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[1,3,5,7,9]", "1"), "0"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[1,3,5,7,9]", "9"), "4"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[1,3,5,7,9]", "4"), "-1"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[]", "1"), "-1"));
                break;
                
            case 2:
                title = "Merge Sort";
                description = "Write a function called merge_sort that takes a list and returns a new sorted list using the merge sort algorithm.";
                initialCode = "def merge_sort(arr):\n    # Write your code here\n    pass";
                solutionMethodName = "merge_sort";
                hints.add("Divide array into two halves");
                hints.add("Recursively sort and merge");
                
                // Add test cases
                testCases.add(new Exercise.TestCase(Arrays.asList("[4,2,1,3]"), "[1,2,3,4]"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[1]"), "[1]"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[]"), "[]"));
                testCases.add(new Exercise.TestCase(Arrays.asList("[3,3,1,1,2,2]"), "[1,1,2,2,3,3]"));
                break;
                
            default:
                title = "Hard Exercise " + number;
                description = "Complete this advanced Python Exercise.";
                initialCode = "def solution():\n    # Your code here\n    pass";
                solutionMethodName = "solution";
                hints.add("Consider time complexity");
                hints.add("Think about space optimization");
                
                // Add default test case
                testCases.add(new Exercise.TestCase(Arrays.asList(), "True"));
        }
        
        String id = "hard_" + number;
        return new Exercise(id, title, description, "Hard", "Official", initialCode, testCases, hints, solutionMethodName, 15, "admin", "System");
    }
} 




