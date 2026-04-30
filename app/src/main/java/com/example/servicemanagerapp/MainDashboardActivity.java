package com.example.servicemanagerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainDashboardActivity extends AppCompatActivity {

    String role;
    String userName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        // Read role – prefer Intent extra, fall back to SharedPreferences
        role = getIntent().getStringExtra("role");
        if (role == null) {
            SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
            role = prefs.getString(LoginActivity.KEY_ROLE, "staff");
        }

        userName = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                .getString(LoginActivity.KEY_USER_NAME, "");

        // Pass role down through fragments via the Intent so they can retrieve it
        getIntent().putExtra("role", role);
        FloatingActionButton addBtn = findViewById(R.id.addServiceBtn);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        String normalizedRole = (role != null) ? role.toLowerCase() : "staff";

        // FCM Subscriptions
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d("FCM", "FCM Token (Device ID): " + task.getResult());
            }
        });
        com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) android.util.Log.d("FCM", "Subscribed to: all");
                    else android.util.Log.e("FCM", "Failed all subscription: " + task.getException());
                });

        if ("admin".equals(normalizedRole) || "owner".equals(normalizedRole)) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("admins")
                    .addOnCompleteListener(task -> android.util.Log.d("FCM", "Subscribed to: admins"));
        } else if ("operator".equals(normalizedRole)) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic("operators")
                    .addOnCompleteListener(task -> android.util.Log.d("FCM", "Subscribed to: operators"));
        } else if ("staff".equals(normalizedRole) && userName != null && !userName.isEmpty()) {
            String sanitizedTopic = "staff_" + userName.replaceAll("[^a-zA-Z0-9-_.~%]", "_");
            com.google.firebase.messaging.FirebaseMessaging.getInstance().subscribeToTopic(sanitizedTopic)
                    .addOnCompleteListener(task -> android.util.Log.d("FCM", "Subscribed to: " + sanitizedTopic));
        }

        // Request POST_NOTIFICATIONS for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        switch (normalizedRole) {

            case "owner":
            case "admin":
                addBtn.setVisibility(View.VISIBLE);
                bottomNav.inflateMenu(R.menu.menu_admin);
                break;

            case "operator":
                addBtn.setVisibility(View.VISIBLE);
                bottomNav.inflateMenu(R.menu.menu_operator);
                break;

            case "staff":
            default:
                addBtn.setVisibility(View.GONE);
                bottomNav.inflateMenu(R.menu.menu_staff);
                break;
        }

        addBtn.setOnClickListener(v ->
                startActivity(new Intent(this, AddServiceActivity.class)));

        // DEFAULT PAGE
        bottomNav.setSelectedItemId(R.id.nav_services);
        loadFragment(new ServiceListFragment());

        // NAVIGATION CLICK
        bottomNav.setOnItemSelectedListener(item -> {

            Fragment fragment = null;

            if (item.getItemId() == R.id.nav_services) {
                fragment = new ServiceListFragment();

            } else if (item.getItemId() == R.id.nav_dashboard) {
                fragment = new DashboardFragment();

            } else if (item.getItemId() == R.id.nav_my_services) {
                fragment = new MyServicesFragment();

            } else if (item.getItemId() == R.id.nav_reports) {
                fragment = new ReportsFragment();
                
            } else if (item.getItemId() == R.id.nav_approvals) {
                fragment = new AdminApprovalFragment();

            } else if (item.getItemId() == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                loadFragment(fragment);
                return true;
            }
            return false;
        });
    }

    /** Inflate toolbar menu for admin/owner to access Staff Approval screen. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if ("admin".equals(role) || "owner".equals(role)) {
            menu.add(0, 1001, 0, "Staff Approvals")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.add(0, 1002, 1, "Logout")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        } else {
            menu.add(0, 1002, 0, "Logout")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1001) {
            BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
            bottomNav.setSelectedItemId(R.id.nav_approvals);
            return true;
        } else if (item.getItemId() == 1002) {
            // Logout
            getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                    .edit().clear().apply();
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // Move app to background instead of logging out
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
        long loginTime = prefs.getLong("loginTime", 0);
        if (loginTime > 0 && System.currentTimeMillis() - loginTime > 14L * 24 * 60 * 60 * 1000) {
            // 14 days expired
            android.widget.Toast.makeText(this, "Session expired (14 days), please login again.", android.widget.Toast.LENGTH_LONG).show();
            prefs.edit().clear().apply();
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            android.net.Uri resultUri = null;
            if (data != null && data.getData() != null) {
                resultUri = data.getData();
            } else if (ServiceAdapter.cachedCameraUri != null) {
                resultUri = ServiceAdapter.cachedCameraUri;
            }
            if (resultUri != null) {
                ServiceAdapter.handlePhotoResult(requestCode, resultUri, this);
            }
        }
    }
}