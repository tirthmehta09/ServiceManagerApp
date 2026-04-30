package com.example.servicemanagerapp;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servicemanagerapp.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    Context context;
    ArrayList<User> userList;
    ArrayList<String> userIds;

    FirestoreRepository repo = new FirestoreRepository();

    public UserAdapter(Context context, ArrayList<User> userList, ArrayList<String> userIds) {
        this.context = context;
        this.userList = userList;
        this.userIds  = userIds;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {

        User user = userList.get(position);
        String uid = userIds.get(position);

        holder.nameTxt.setText(user.getName() != null ? user.getName() : "—");
        holder.emailTxt.setText(user.getEmail() != null ? user.getEmail() : "—");
        holder.phoneTxt.setText(user.getPhone() != null ? user.getPhone() : "—");

        String empId = user.getEmployeeId() != null ? user.getEmployeeId() : "Not Assigned";
        holder.empIdTxt.setText("Employee ID : " + empId);

        String status = user.getStatus() != null ? user.getStatus() : "pending";
        holder.statusTxt.setText("Status : " + status);

        if ("approved".equals(status)) {
            holder.statusTxt.setTextColor(Color.parseColor("#2E7D32"));
            holder.approveBtn.setEnabled(false);
        } else if ("rejected".equals(status)) {
            holder.statusTxt.setTextColor(Color.RED);
            holder.approveBtn.setEnabled(true);
        } else {
            holder.statusTxt.setTextColor(Color.GRAY);
            holder.approveBtn.setEnabled(true);
        }

        // Role spinner
        String[] roles = {"Staff", "Admin", "Operator", "Owner"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                context, R.layout.spinner_item, roles);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        holder.roleSpinner.setAdapter(spinnerAdapter);

        if (user.getRole() != null) {
            String roleCap = capitalize(user.getRole());
            int pos = spinnerAdapter.getPosition(roleCap);
            if (pos >= 0) holder.roleSpinner.setSelection(pos);
        }

        // Save role
        holder.saveRoleBtn.setOnClickListener(v -> {
            String selectedRole = holder.roleSpinner.getSelectedItem().toString().toLowerCase();
            repo.updateUserRole(uid, selectedRole,
                    unused -> Toast.makeText(context, "Role updated to " + selectedRole, Toast.LENGTH_SHORT).show(),
                    e -> Toast.makeText(context, "Failed to update role", Toast.LENGTH_SHORT).show());
        });

        // Approve
        holder.approveBtn.setOnClickListener(v -> approveUser(uid, user));

        // Reject / Deactivate
        if ("approved".equals(status)) {
            holder.rejectBtn.setText("Deactivate");
        } else {
            holder.rejectBtn.setText("Reject");
        }

        holder.rejectBtn.setOnClickListener(v -> {
            String action = "approved".equals(status) ? "Deactivate" : "Reject";
            String newStatus = "approved".equals(status) ? "inactive" : "rejected";
            
            new androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(action + " User")
                    .setMessage("Are you sure you want to " + action.toLowerCase() + " " + (user.getName() != null ? user.getName() : "this user") + "?")
                    .setPositiveButton("Yes", (d, w) ->
                            repo.updateUserStatus(uid, newStatus,
                                    unused -> Toast.makeText(context, "User " + action + "d", Toast.LENGTH_SHORT).show(),
                                    e -> Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // Delete
        holder.deleteBtn.setOnClickListener(v ->
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("Delete User")
                        .setMessage("WARNING: This permanently deletes " + (user.getName() != null ? user.getName() : "this user") + " from the database. This cannot be undone.")
                        .setPositiveButton("Delete", (d, w) ->
                                repo.getDb().collection("users").document(uid).delete()
                                        .addOnSuccessListener(unused -> Toast.makeText(context, "User Deleted", Toast.LENGTH_SHORT).show())
                                        .addOnFailureListener(e -> Toast.makeText(context, "Delete Failed", Toast.LENGTH_SHORT).show()))
                        .setNegativeButton("Cancel", null)
                        .show()
        );
    }

    private void approveUser(String uid, User user) {

        if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
            // Already has employee ID – just re-approve
            repo.updateUserStatus(uid, "approved",
                    unused -> Toast.makeText(context, "User Approved", Toast.LENGTH_SHORT).show(),
                    e -> Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show());
            return;
        }

        // Generate new Employee ID
        repo.getDb().collection("metadata")
                .document("employeeCounter")
                .get()
                .addOnSuccessListener(doc -> {

                    long lastId = 0;
                    if (doc.exists()) {
                        Long val = doc.getLong("lastId");
                        if (val != null) lastId = val;
                    }
                    long newId = lastId + 1;
                    String empId = "EMP" + String.format("%03d", newId);

                    Map<String, Object> update = new HashMap<>();
                    update.put("status", "approved");
                    update.put("employeeId", empId);

                    repo.getDb().collection("users").document(uid).update(update);
                    repo.getDb().collection("metadata").document("employeeCounter")
                            .set(java.util.Collections.singletonMap("lastId", newId));

                    Toast.makeText(context, "Approved: " + empId, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Failed to generate ID", Toast.LENGTH_SHORT).show());
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    @Override
    public int getItemCount() { return userList.size(); }

    static class UserViewHolder extends RecyclerView.ViewHolder {

        TextView nameTxt, emailTxt, phoneTxt, statusTxt, empIdTxt;
        Spinner roleSpinner;
        Button approveBtn, rejectBtn, saveRoleBtn, deleteBtn;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTxt    = itemView.findViewById(R.id.nameTxt);
            emailTxt   = itemView.findViewById(R.id.emailTxt);
            phoneTxt   = itemView.findViewById(R.id.phoneTxt);
            statusTxt  = itemView.findViewById(R.id.statusTxt);
            empIdTxt   = itemView.findViewById(R.id.empIdTxt);
            roleSpinner = itemView.findViewById(R.id.roleSpinner);
            approveBtn = itemView.findViewById(R.id.approveBtn);
            rejectBtn  = itemView.findViewById(R.id.rejectBtn);
            saveRoleBtn = itemView.findViewById(R.id.saveRoleBtn);
            deleteBtn   = itemView.findViewById(R.id.deleteBtn);
        }
    }
}