package com.code_share.finalapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private float swipeStartX;
    private float swipeStartY;
    private long swipeStartTime;
    private boolean trackingSwipe;
    private int selectedMainTabId = R.id.navigation_lessons;
    private long lastBackPressedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        
        // Set up auth state listener
        mAuthListener = firebaseAuth -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser == null) {
                // User is signed out
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(loginIntent);
                finish();
            }
        };

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new LessonFragment())
                    .commit();
        }

        setupNavigation();
        getSupportFragmentManager().addOnBackStackChangedListener(this::syncBottomNavigationVisibility);
        navView.setSelectedItemId(R.id.navigation_lessons);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof CodeEditorFragment
                        && ((CodeEditorFragment) currentFragment).isPlayground()) {
                    if (fragmentManager.getBackStackEntryCount() > 0) {
                        fragmentManager.popBackStack();
                    } else {
                        fragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, new LessonFragment())
                                .commit();
                    }
                } else if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                } else if (navView.getSelectedItemId() != R.id.navigation_lessons) {
                    navView.setSelectedItemId(R.id.navigation_lessons);
                } else {
                    long now = SystemClock.elapsedRealtime();
                    if (now - lastBackPressedAt < 2000) {
                        finishAffinity();
                    } else {
                        lastBackPressedAt = now;
                        Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    public void setBottomNavigationVisible(boolean visible) {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        if (navView != null) {
            navView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private void syncBottomNavigationVisibility() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        boolean hideNavigation = currentFragment instanceof ExerciseWorkspaceFragment
                || (currentFragment instanceof CodeEditorFragment
                && ((CodeEditorFragment) currentFragment).isPlayground());
        setBottomNavigationVisible(!hideNavigation);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            swipeStartX = event.getRawX();
            swipeStartY = event.getRawY();
            swipeStartTime = event.getEventTime();
            trackingSwipe = true;
        } else if (event.getActionMasked() == MotionEvent.ACTION_UP && trackingSwipe) {
            navigateForSwipe(event);
            trackingSwipe = false;
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            trackingSwipe = false;
        }
        return super.dispatchTouchEvent(event);
    }

    private void navigateForSwipe(MotionEvent event) {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        if (navView == null || navView.getVisibility() != View.VISIBLE
                || getSupportFragmentManager().getBackStackEntryCount() > 0) {
            return;
        }

        float distanceX = event.getRawX() - swipeStartX;
        float distanceY = event.getRawY() - swipeStartY;
        int minimumDistance = Math.round(72 * getResources().getDisplayMetrics().density);
        if (Math.abs(distanceX) < minimumDistance
                || Math.abs(distanceX) < Math.abs(distanceY) * 1.25f
                || event.getEventTime() - swipeStartTime > 1000) {
            return;
        }

        int selectedItemId = navView.getSelectedItemId();
        if (selectedItemId == R.id.navigation_practise && shouldKeepPracticePagerSwipe(distanceX)) {
            return;
        }

        int selectedPosition = -1;
        for (int i = 0; i < navView.getMenu().size(); i++) {
            if (navView.getMenu().getItem(i).getItemId() == selectedItemId) {
                selectedPosition = i;
                break;
            }
        }

        int targetPosition = selectedPosition + (distanceX < 0 ? 1 : -1);
        if (selectedPosition >= 0 && targetPosition >= 0 && targetPosition < navView.getMenu().size()) {
            navView.setSelectedItemId(navView.getMenu().getItem(targetPosition).getItemId());
        }
    }

    private boolean shouldKeepPracticePagerSwipe(float distanceX) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (!(fragment instanceof PractiseFragment) || fragment.getView() == null) {
            return false;
        }

        View pagerView = fragment.getView().findViewById(R.id.view_pager);
        if (!(pagerView instanceof ViewPager2)) {
            return false;
        }

        int[] pagerLocation = new int[2];
        pagerView.getLocationOnScreen(pagerLocation);
        if (swipeStartX < pagerLocation[0] || swipeStartX > pagerLocation[0] + pagerView.getWidth()
                || swipeStartY < pagerLocation[1] || swipeStartY > pagerLocation[1] + pagerView.getHeight()) {
            return false;
        }

        ViewPager2 pager = (ViewPager2) pagerView;
        RecyclerView.Adapter<?> adapter = pager.getAdapter();
        if (adapter == null) {
            return false;
        }

        return distanceX < 0
                ? pager.getCurrentItem() < adapter.getItemCount() - 1
                : pager.getCurrentItem() > 0;
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void setupNavigation() {
        BottomNavigationView navView = findViewById(R.id.bottom_navigation);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_lessons) {
                selectedFragment = new LessonFragment();
            } else if (itemId == R.id.navigation_practise) {
                selectedFragment = new PractiseFragment();
            } else if (itemId == R.id.navigation_community) {
                selectedFragment = new CommunityFragment();
            } else if (itemId == R.id.navigation_messages) {
                selectedFragment = new MessagesFragment();
            } else if (itemId == R.id.navigation_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                if (itemId == selectedMainTabId) {
                    return true;
                }

                int currentPosition = findMenuPosition(navView, selectedMainTabId);
                int nextPosition = findMenuPosition(navView, itemId);
                int enterAnimation = nextPosition > currentPosition
                        ? R.anim.main_tab_enter_from_right : R.anim.main_tab_enter_from_left;
                int exitAnimation = nextPosition > currentPosition
                        ? R.anim.main_tab_exit_to_left : R.anim.main_tab_exit_to_right;
                int popEnterAnimation = nextPosition > currentPosition
                        ? R.anim.main_tab_enter_from_left : R.anim.main_tab_enter_from_right;
                int popExitAnimation = nextPosition > currentPosition
                        ? R.anim.main_tab_exit_to_right : R.anim.main_tab_exit_to_left;

                getSupportFragmentManager().beginTransaction()
                        .setCustomAnimations(enterAnimation, exitAnimation,
                                popEnterAnimation, popExitAnimation)
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                selectedMainTabId = itemId;
                return true;
            }
            return false;
        });
    }

    private int findMenuPosition(BottomNavigationView navView, int itemId) {
        for (int i = 0; i < navView.getMenu().size(); i++) {
            if (navView.getMenu().getItem(i).getItemId() == itemId) {
                return i;
            }
        }
        return -1;
    }
}
