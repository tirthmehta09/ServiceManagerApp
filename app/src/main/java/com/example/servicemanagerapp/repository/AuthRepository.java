package com.example.servicemanagerapp.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * AuthRepository – wraps FirebaseAuth operations.
 */
public class AuthRepository {

    private final FirebaseAuth auth;

    public AuthRepository() {
        this.auth = FirebaseAuth.getInstance();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public void signOut() {
        auth.signOut();
    }

    public void signIn(String email, String password,
                       com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.auth.AuthResult> onSuccess,
                       com.google.android.gms.tasks.OnFailureListener onFailure) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void register(String email, String password,
                         com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.auth.AuthResult> onSuccess,
                         com.google.android.gms.tasks.OnFailureListener onFailure) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}
