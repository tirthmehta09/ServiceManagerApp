package com.example.servicemanagerapp;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText name, email, password, phone;
    Spinner locationSpinner;
    Button registerBtn;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        phone = findViewById(R.id.phone);
        locationSpinner = findViewById(R.id.locationSpinner);
        registerBtn = findViewById(R.id.registerBtn);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Location Spinner
        ArrayAdapter<CharSequence> locationAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.locations,
                R.layout.spinner_item
        );
        locationAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        locationSpinner.setAdapter(locationAdapter);

        registerBtn.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String userName = name.getText().toString().trim();
        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();
        String userPhone = phone.getText().toString().trim();
        String location = locationSpinner.getSelectedItem().toString();

        // -------- VALIDATIONS --------

        if (userName.isEmpty()) {
            name.setError("Enter full name");
            name.requestFocus();
            return;
        }

        if (userEmail.isEmpty()) {
            email.setError("Enter email");
            email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            email.setError("Enter valid email");
            email.requestFocus();
            return;
        }

        if (userPassword.isEmpty()) {
            password.setError("Enter password");
            password.requestFocus();
            return;
        }

        if (userPassword.length() < 6) {
            password.setError("Password must be at least 6 characters");
            password.requestFocus();
            return;
        }

        if (!userPassword.matches("^(?=.*[0-9])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{6,}$")) {
            password.setError("Password needs 1 uppercase, 1 number, 1 special char");
            password.requestFocus();
            return;
        }

        if (userPhone.isEmpty()) {
            phone.setError("Enter phone number");
            phone.requestFocus();
            return;
        }

        if (userPhone.length() < 10) {
            phone.setError("Enter valid phone number");
            phone.requestFocus();
            return;
        }

        // Prevent multiple clicks
        registerBtn.setEnabled(false);
        registerBtn.setText("Registering...");

        // -------- FIREBASE AUTH --------

        auth.createUserWithEmailAndPassword(userEmail, userPassword)

                .addOnSuccessListener(authResult -> {

                    String uid = auth.getCurrentUser().getUid();

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", userName);
                    user.put("email", userEmail);
                    user.put("phone", userPhone);
                    user.put("location", location);
                    user.put("status", "pending");
                    // DEFAULT ROLE
                    user.put("role", "STAFF");
                    user.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());

                    // -------- FIRESTORE SAVE --------

                    db.collection("users")
                            .document(uid)
                            .set(user)

                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this,
                                        "Registration submitted.\nAwait admin approval.",
                                        Toast.LENGTH_LONG).show();

                                finish();
                            })

                            .addOnFailureListener(e -> {

                                registerBtn.setEnabled(true);
                                registerBtn.setText("Register");

                                Toast.makeText(this,
                                        "Firestore Error: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });

                })

                .addOnFailureListener(e -> {

                    registerBtn.setEnabled(true);
                    registerBtn.setText("Register");

                    Toast.makeText(this,
                            "Registration Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}