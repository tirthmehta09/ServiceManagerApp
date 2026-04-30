package com.example.servicemanagerapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.example.servicemanagerapp.repository.AuthRepository;
import com.example.servicemanagerapp.repository.FirestoreRepository;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn, registerBtn;

    AuthRepository authRepo;
    FirestoreRepository firestoreRepo;

    public static final String PREFS_NAME = "ServiceAppPrefs";
    public static final String KEY_ROLE = "role";
    public static final String KEY_USER_NAME = "userName";
    public static final String KEY_UID = "uid";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);

        authRepo = new AuthRepository();
        firestoreRepo = new FirestoreRepository();

        // Auto-login if already signed in
        if (authRepo.isLoggedIn()) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            long loginTime = prefs.getLong("loginTime", 0);
            
            if (loginTime > 0 && System.currentTimeMillis() - loginTime <= 14L * 24 * 60 * 60 * 1000) {
                String savedRole = prefs.getString(KEY_ROLE, null);
                if (savedRole != null) {
                    goToDashboard(savedRole);
                    return;
                }
            } else {
                // Session expired or not set
                authRepo.signOut();
                prefs.edit().clear().apply();
            }
        }

        loginBtn.setOnClickListener(v -> loginUser());
        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Request POST_NOTIFICATIONS for Android 13+ early on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void loginUser() {

        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        if (userEmail.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        loginBtn.setText("Logging in...");
        loginBtn.setEnabled(false);

        authRepo.signIn(userEmail, userPassword,
                authResult -> {

                    String uid = authRepo.getCurrentUid();

                    firestoreRepo.getUser(uid,
                            doc -> {

                                if (!doc.exists()) {
                                    Toast.makeText(this, "User data missing", Toast.LENGTH_SHORT).show();
                                    resetButton();
                                    return;
                                }

                                String status = doc.getString("status");
                                String role = doc.getString("role");
                                String name = doc.getString("name");

                                if ("approved".equals(status)) {

                                    // Persist role, name, uid to SharedPreferences
                                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                            .edit()
                                            .putString(KEY_ROLE, role)
                                            .putString(KEY_USER_NAME, name != null ? name : "")
                                            .putString(KEY_UID, uid)
                                            .putLong("loginTime", System.currentTimeMillis())
                                            .apply();

                                    goToDashboard(role);

                                } else if ("pending".equals(status)) {

                                    Toast.makeText(this,
                                            "Awaiting admin approval",
                                            Toast.LENGTH_LONG).show();
                                    authRepo.signOut();
                                    resetButton();

                                } else {

                                    Toast.makeText(this,
                                            "Registration rejected",
                                            Toast.LENGTH_LONG).show();
                                    authRepo.signOut();
                                    resetButton();
                                }
                            },
                            e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                resetButton();
                            });
                },
                e -> {
                    Toast.makeText(this,
                            "Login Failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void goToDashboard(String role) {
        Intent intent = new Intent(LoginActivity.this, MainDashboardActivity.class);
        intent.putExtra("role", role);
        startActivity(intent);
        finish();
    }

    private void resetButton() {
        loginBtn.setText("Login");
        loginBtn.setEnabled(true);
    }
}