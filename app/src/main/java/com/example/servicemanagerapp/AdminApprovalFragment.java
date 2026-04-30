package com.example.servicemanagerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;

import java.util.ArrayList;
import java.util.Collections;

public class AdminApprovalFragment extends Fragment {

    RecyclerView recyclerPendingUsers;
    RecyclerView recyclerApprovedUsers;
    android.widget.TextView pendingHeaderTxt;
    android.widget.TextView approvedHeaderTxt;
    ImageButton reloadBtn;

    UserAdapter adapterPending;
    UserAdapter adapterApproved;

    ArrayList<User> pendingList = new ArrayList<>();
    ArrayList<String> pendingIds = new ArrayList<>();
    ArrayList<User> approvedList = new ArrayList<>();
    ArrayList<String> approvedIds = new ArrayList<>();

    // Full lists for filtering
    ArrayList<User> allPending = new ArrayList<>();
    ArrayList<String> allPendingIds = new ArrayList<>();
    ArrayList<User> allApproved = new ArrayList<>();
    ArrayList<String> allApprovedIds = new ArrayList<>();

    EditText adminSearchBar;
    FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_admin_approval, container, false);

        recyclerPendingUsers = view.findViewById(R.id.recyclerPendingUsers);
        recyclerApprovedUsers = view.findViewById(R.id.recyclerApprovedUsers);
        pendingHeaderTxt = view.findViewById(R.id.pendingHeaderTxt);
        approvedHeaderTxt = view.findViewById(R.id.approvedHeaderTxt);
        reloadBtn = view.findViewById(R.id.reloadBtn);

        recyclerPendingUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerApprovedUsers.setLayoutManager(new LinearLayoutManager(getContext()));

        adapterPending = new UserAdapter(getContext(), pendingList, pendingIds);
        adapterApproved = new UserAdapter(getContext(), approvedList, approvedIds);

        recyclerPendingUsers.setAdapter(adapterPending);
        recyclerApprovedUsers.setAdapter(adapterApproved);

        adminSearchBar = view.findViewById(R.id.userSearchBar);
        adminSearchBar.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        db = FirebaseFirestore.getInstance();

        reloadBtn.setOnClickListener(v -> loadUsers());

        loadUsers();

        return view;
    }

    private void performSearch(String query) {
        String q = query.toLowerCase().trim();
        
        pendingList.clear();
        pendingIds.clear();
        approvedList.clear();
        approvedIds.clear();

        // Filter Pending
        for (int i = 0; i < allPending.size(); i++) {
            User u = allPending.get(i);
            if (u.getName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q)) {
                pendingList.add(u);
                pendingIds.add(allPendingIds.get(i));
            }
        }

        // Filter Approved
        for (int i = 0; i < allApproved.size(); i++) {
            User u = allApproved.get(i);
            if (u.getName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q)) {
                approvedList.add(u);
                approvedIds.add(allApprovedIds.get(i));
            }
        }

        adapterPending.notifyDataSetChanged();
        adapterApproved.notifyDataSetChanged();

        pendingHeaderTxt.setText("Pending Approvals (" + pendingList.size() + ")");
        approvedHeaderTxt.setText("Active Team (" + approvedList.size() + ")");
    }

    private void loadUsers() {
        db.collection("users")
                .get()
                .addOnSuccessListener(snap -> {
                    ArrayList<DocumentSnapshot> docs = (ArrayList<DocumentSnapshot>) snap.getDocuments();
                    
                    allPending.clear();
                    allPendingIds.clear();
                    allApproved.clear();
                    allApprovedIds.clear();

                    for (DocumentSnapshot doc : docs) {
                        User user = doc.toObject(User.class);
                        if(user == null) continue;

                        String status = user.getStatus() != null ? user.getStatus() : "";
                        
                        if ("pending".equalsIgnoreCase(status)) {
                            allPending.add(user);
                            allPendingIds.add(doc.getId());
                        } else {
                            allApproved.add(user);
                            allApprovedIds.add(doc.getId());
                        }
                    }

                    // Trigger search/filter with current query
                    if (adminSearchBar != null) {
                        performSearch(adminSearchBar.getText().toString());
                    } else {
                        performSearch("");
                    }
                });
    }
}
