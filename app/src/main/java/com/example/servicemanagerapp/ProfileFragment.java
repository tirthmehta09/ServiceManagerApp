package com.example.servicemanagerapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    TextView profileNameTxt, profileRoleTxt, profileEmailTxt;
    ImageView profileImg;
    Button logoutBtn;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileNameTxt = view.findViewById(R.id.profileNameTxt);
        profileRoleTxt = view.findViewById(R.id.profileRoleTxt);
        profileEmailTxt = view.findViewById(R.id.profileEmailTxt);
        profileImg = view.findViewById(R.id.profileImg);
        logoutBtn = view.findViewById(R.id.logoutBtn);

        profileImg.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, 3001);
        });

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            profileEmailTxt.setText(user.getEmail());
        } else {
            profileEmailTxt.setText("");
        }

        if (getActivity() != null) {
            SharedPreferences prefs = getActivity()
                    .getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
            
            String role = prefs.getString(LoginActivity.KEY_ROLE, "Unknown");
            String name = prefs.getString(LoginActivity.KEY_USER_NAME, "User");
            
            // Capitalize role
            if (role != null && role.length() > 0) {
                role = role.substring(0, 1).toUpperCase() + role.substring(1);
            }
            
            profileNameTxt.setText(name);
            profileRoleTxt.setText("Role: " + role);
        }

        logoutBtn.setOnClickListener(v -> performLogout());

        loadProfilePhoto();

        return view;
    }

    private void performLogout() {
        if (getActivity() == null) return;
        
        getActivity().getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE)
                .edit().clear().apply();
        FirebaseAuth.getInstance().signOut();
        
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void loadProfilePhoto() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(user.getUid())
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && getActivity() != null) {
                    String photoUrl = doc.getString("profilePhoto");
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        Glide.with(this)
                            .load(photoUrl)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .circleCrop()
                            .into(profileImg);
                    }
                }
            });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3001 && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            uploadProfilePhoto(data.getData());
        }
    }

    private void uploadProfilePhoto(android.net.Uri uri) {
        if (getActivity() == null) return;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        Toast.makeText(getActivity(), "Uploading Photo...", Toast.LENGTH_SHORT).show();

        com.google.firebase.storage.StorageReference ref = com.google.firebase.storage.FirebaseStorage.getInstance().getReference()
                .child("profile_pics/" + user.getUid() + ".jpg");

        ref.putFile(uri)
           .addOnSuccessListener(snap -> {
               ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                   com.google.firebase.firestore.FirebaseFirestore.getInstance()
                       .collection("users").document(user.getUid())
                       .update("profilePhoto", downloadUri.toString());
                       
                   if (getActivity() != null) {
                       Toast.makeText(getActivity(), "Profile Photo Updated!", Toast.LENGTH_SHORT).show();
                   }
                   loadProfilePhoto();
               });
           })
           .addOnFailureListener(e -> {
               if (getActivity() != null) {
                   Toast.makeText(getActivity(), "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
               }
           });
    }
}
