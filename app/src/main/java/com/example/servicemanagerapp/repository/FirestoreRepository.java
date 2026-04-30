package com.example.servicemanagerapp.repository;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Map;

/**
 * FirestoreRepository – centralises all Firestore write/read operations.
 * UI layers call these methods; no raw Firestore calls should exist in Activities/Fragments.
 */
public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // ─────────────────────────────────────────────
    // SERVICES
    // ─────────────────────────────────────────────

    /** Real-time listener for all services ordered by serviceId. */
    public void listenAllServices(com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        db.collection("services")
                .orderBy("serviceId", Query.Direction.ASCENDING)
                .addSnapshotListener(listener);
    }

    /** Real-time listener for all services (no ordering – used by dashboard/reports). */
    public void listenServicesUnordered(com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        db.collection("services")
                .addSnapshotListener(listener);
    }

    /** Update service status. */
    public void updateServiceStatus(String serviceId, String newStatus,
                                    com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                    com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("services")
                .document(serviceId)
                .update("status", newStatus)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Staff accepts a service (adds their name to acceptedStaff list). */
    public void staffAcceptService(String serviceId, String staffName,
                                 com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                 com.google.android.gms.tasks.OnFailureListener onFailure) {
        Map<String, Object> update = new HashMap<>();
        update.put("acceptedStaff", FieldValue.arrayUnion(staffName));
        update.put("status", "Going");
        db.collection("services")
                .document(serviceId)
                .update(update)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Staff declines a service (removes their name from assignedStaff list). */
    public void staffDeclineService(String serviceId, String staffName,
                                  com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                  com.google.android.gms.tasks.OnFailureListener onFailure) {
        Map<String, Object> update = new HashMap<>();
        update.put("assignedStaff", FieldValue.arrayRemove(staffName));
        // Only revert status if no one else has accepted (handled simply for now)

        db.collection("services")
                .document(serviceId)
                .update(update)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Operator assigns staff directly to a service. */
    public void operatorAssignStaff(String serviceId, java.util.List<String> staffNames,
                                  com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                  com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("services")
                .document(serviceId)
                .update("assignedStaff", staffNames)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Admin closes a service. */
    public void closeService(String serviceId, String adminName,
                             com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                             com.google.android.gms.tasks.OnFailureListener onFailure) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Closed");
        update.put("closedBy", adminName);
        update.put("closedTimestamp", FieldValue.serverTimestamp());

        db.collection("services")
                .document(serviceId)
                .update(update)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Undo Close (Reopen Service). */
    public void reopenService(String serviceId,
                              com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                              com.google.android.gms.tasks.OnFailureListener onFailure) {
        Map<String, Object> update = new HashMap<>();
        update.put("status", "Delivered");
        update.put("closedBy", FieldValue.delete());

        db.collection("services")
                .document(serviceId)
                .update(update)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Save completion video URL. */
    public void saveCompletionVideo(String serviceId, String url,
                                    com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                    com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("services")
                .document(serviceId)
                .update("completionVideo", url)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    // ─────────────────────────────────────────────
    // USERS
    // ─────────────────────────────────────────────

    /** Get user document once by UID. */
    public void getUser(String uid,
                        com.google.android.gms.tasks.OnSuccessListener<com.google.firebase.firestore.DocumentSnapshot> onSuccess,
                        com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Real-time listener for all users (admin approval screen). */
    public void listenAllUsers(com.google.firebase.firestore.EventListener<com.google.firebase.firestore.QuerySnapshot> listener) {
        db.collection("users").addSnapshotListener(listener);
    }

    /** Approve or reject a user registration. */
    public void updateUserStatus(String uid, String newStatus,
                                 com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                                 com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("users")
                .document(uid)
                .update("status", newStatus)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Update user role. */
    public void updateUserRole(String uid, String newRole,
                               com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                               com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("users")
                .document(uid)
                .update("role", newRole)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Increment staff earnings by a given amount. */
    public void addEarnings(String uid, double amount,
                            com.google.android.gms.tasks.OnSuccessListener<Void> onSuccess,
                            com.google.android.gms.tasks.OnFailureListener onFailure) {
        db.collection("users")
                .document(uid)
                .update("earnings", FieldValue.increment(amount))
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    /** Increment staff earnings by name (finds user then updates). */
    public void addEarningsByStaffName(String staffName, double amount) {
        db.collection("users")
                .whereEqualTo("name", staffName)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String uid = snap.getDocuments().get(0).getId();
                        addEarnings(uid, amount, null, null);
                    }
                });
    }

    // ─────────────────────────────────────────────
    // SERVICE COUNTER (for SRV001, SRV002…)
    // ─────────────────────────────────────────────

    public FirebaseFirestore getDb() {
        return db;
    }
}
