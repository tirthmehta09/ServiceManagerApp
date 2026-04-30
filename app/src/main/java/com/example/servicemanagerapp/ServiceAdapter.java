package com.example.servicemanagerapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servicemanagerapp.repository.FirestoreRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    Context context;
    ArrayList<Service> serviceList;
    String role;

    FirestoreRepository repo = new FirestoreRepository();
    FirebaseAuth auth = FirebaseAuth.getInstance();

    public interface OnPhotoRequestedListener {
        void onPhotoRequested(String serviceId);
    }
    private OnPhotoRequestedListener photoListener;
    private final Map<String, Boolean> uploadStateMap = new java.util.HashMap<>();
    
    // Static context for photo selection (Activity results)
    public static String pendingServiceId = null;
    public static String pendingPhotoType = null;

    // Statuses available when staff updates progress
    String[] progressStatuses = {
            "Going",
            "Inspection Done",
            "Repair In Progress",
            "Repair Completed",
            "Delivered"
    };

    public ServiceAdapter(Context context, ArrayList<Service> serviceList, String role, OnPhotoRequestedListener listener) {
        this.context = context;
        this.serviceList = serviceList;
        this.role = role;
        this.repo = new FirestoreRepository();
        this.photoListener = listener;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.service_item, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {

        Service service = serviceList.get(position);

        // --- Reset visibility first (RecyclerView reuse fix) ---
        holder.startServiceBtn.setVisibility(View.GONE);
        holder.notGoingBtn.setVisibility(View.GONE);
        holder.statusSpinner.setVisibility(View.GONE);
        holder.uploadCompletionBtn.setVisibility(View.GONE);
        holder.viewCompletionBtn.setVisibility(View.GONE);
        holder.closeServiceBtn.setVisibility(View.GONE);
        holder.closedByTxt.setVisibility(View.GONE);

        // --- Basic info ---
        holder.serviceIdTxt.setText("Service ID : " + safe(service.getServiceId()));
        holder.customerTxt.setText("Customer : " + safe(service.getCustomerName()));
        holder.phoneTxt.setText("📞 " + safe(service.getPhone()));
        holder.machineTxt.setText("🔧 Machine : " + safe(service.getMachineType()));
        holder.issueTxt.setText("⚠ Issue : " + safe(service.getIssue()));
        holder.dateTxt.setText("🕒 " + safe(service.getCreatedAt()));

        // Staff list
        holder.staffTxt.setText("👷 Assigned : " + service.getAssignedStaffDisplay());

        // Closed tracking
        if (service.getClosedBy() != null && !service.getClosedBy().trim().isEmpty()) {
            holder.closedByTxt.setVisibility(View.VISIBLE);
            holder.closedByTxt.setText("🔒 Closed By : " + service.getClosedBy());
        }

        // Dial Staff directly
        holder.staffTxt.setOnClickListener(v -> {
            String staffDisplay = service.getAssignedStaffDisplay();
            if (staffDisplay == null || staffDisplay.isEmpty() || "None".equals(staffDisplay)) {
                toast("No staff assigned");
                return;
            }
            
            // Take the first staff member if multiple are assigned
            String firstStaff = staffDisplay;
            if (firstStaff.contains(",")) {
                firstStaff = firstStaff.split(",")[0].trim();
            }
            
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("name", firstStaff)
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null && !snap.isEmpty()) {
                            String p = snap.getDocuments().get(0).getString("phone");
                            if (p != null && !p.isEmpty()) {
                                Intent intent = new Intent(Intent.ACTION_DIAL);
                                intent.setData(Uri.parse("tel:" + p));
                                context.startActivity(intent);
                            } else {
                                toast("Staff member has no phone recorded");
                            }
                        } else {
                            toast("Staff not found in registry");
                        }
                    })
                    .addOnFailureListener(e -> toast("Lookup failed: " + e.getMessage()));
        });

        // --- Phone tap → dial ---
        holder.phoneTxt.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:" + service.getPhone()));
            context.startActivity(i);
        });

        // --- Location tap → Maps ---
        holder.locationTxt.setText("📍 " + safe(service.getLocation()));
        holder.locationTxt.setOnClickListener(v -> {
            Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                    + Uri.encode(safe(service.getLocation())));
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        });

        // --- Status badge ---
        String status = service.getStatus() != null ? service.getStatus() : "Requested";
        holder.statusBadge.setText(status);
        holder.statusBadge.setTextColor(Color.WHITE);
        holder.statusBadge.getBackground().setTint(statusColor(status));

        // Card stroke
        // Refined border for visibility (2dp)
        int strokeWidthPx = (int) (2 * context.getResources().getDisplayMetrics().density);
        holder.cardView.setStrokeWidth(strokeWidthPx);
        holder.cardView.setStrokeColor(statusColor(status));

        // --- Video button ---
        holder.videoBtn.setOnClickListener(v -> {
            String url = service.getVideoUrl();
            if (url != null && !url.isEmpty()) {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } else {
                toast("No Video Available");
            }
        });

        // ─── ROLE-BASED BUTTONS ─────────────────────────────────────────

        String myName = getMyName();
        String normalizedRole = (role != null) ? role.toLowerCase() : "staff";

        switch (normalizedRole) {
            case "staff":
                setupStaffButtons(holder, service, myName);
                break;
            case "operator":
                setupOperatorButtons(holder, service);
                break;
            case "admin":
            case "owner":
                setupAdminButtons(holder, service);
                break;
        }
    }

    // ─── OPERATOR BUTTONS ───────────────────────────────────────────────
    private void setupOperatorButtons(ServiceViewHolder holder, Service service) {
        String status = service.getStatus() != null ? service.getStatus() : "";
        if ("Requested".equals(status)) {
            holder.assignStaffBtn.setVisibility(View.VISIBLE);
            holder.assignStaffBtn.setOnClickListener(v -> {
                // Fetch staff list and show multi-select dialog
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereIn("role", java.util.Arrays.asList("staff", "STAFF"))
                    .get()
                    .addOnSuccessListener(snap -> {
                        if (snap != null && !snap.isEmpty()) {
                            List<String> staffNames = new ArrayList<>();
                            for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                                String name = doc.getString("name");
                                if (name != null) staffNames.add(name);
                            }
                            String[] namesArray = staffNames.toArray(new String[0]);
                            boolean[] checkedItems = new boolean[namesArray.length];
                            
                            // pre-select already assigned staff
                            List<String> alreadyAssigned = service.getAssignedStaff();
                            if (alreadyAssigned != null) {
                                for (int i = 0; i < namesArray.length; i++) {
                                    if (alreadyAssigned.contains(namesArray[i])) {
                                        checkedItems[i] = true;
                                    }
                                }
                            }

                            new AlertDialog.Builder(context)
                                .setTitle("Assign Staff")
                                .setMultiChoiceItems(namesArray, checkedItems, (dialog, which, isChecked) -> {
                                    checkedItems[which] = isChecked;
                                })
                                .setPositiveButton("Assign", (dialog, which) -> {
                                    List<String> selectedStaff = new ArrayList<>();
                                    for (int i = 0; i < checkedItems.length; i++) {
                                        if (checkedItems[i]) selectedStaff.add(namesArray[i]);
                                    }
                                    if (!selectedStaff.isEmpty()) {
                                        repo.operatorAssignStaff(service.getServiceId(), selectedStaff,
                                            unused -> {
                                                toast("Staff assigned successfully");
                                                for (String sName : selectedStaff) {
                                                    FCMHelper.sendNotification("staff_" + sName.replaceAll("[^a-zA-Z0-9-_.~%]", "_"), "New Assignment", "You have been assigned to service " + service.getServiceId(), service.getServiceId());
                                                }
                                            },
                                            e -> toast("Assignment failed: " + e.getMessage()));
                                    } else {
                                        toast("No staff selected");
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                        } else {
                            toast("No staff members found");
                        }
                    })
                    .addOnFailureListener(e -> toast("Error fetching staff"));
            });
        }
    }

    // ─── STAFF BUTTONS ───────────────────────────────────────────────────

    private void setupStaffButtons(ServiceViewHolder holder, Service service, String myName) {

        String status = service.getStatus() != null ? service.getStatus() : "";
        boolean isAssigned = service.isStaffAssigned(myName);
        boolean hasAccepted = service.isStaffAccepted(myName);
        boolean noOneAssigned = (service.getAssignedStaff() == null || service.getAssignedStaff().isEmpty());

        if ("Requested".equals(status)) {
            // Not started – show [ACCEPT] and [NOT GOING] only if assigned but not accepted, OR if unassigned
            if ((isAssigned && !hasAccepted) || noOneAssigned) {
                holder.startServiceBtn.setVisibility(View.VISIBLE);
                holder.startServiceBtn.setText("ACCEPT");
                if (isAssigned && !hasAccepted) {
                    holder.notGoingBtn.setVisibility(View.VISIBLE);
                    holder.notGoingBtn.setText("NOT GOING");
                }

                holder.startServiceBtn.setOnClickListener(v -> handleStaffAcceptance(service, myName));
                
                holder.notGoingBtn.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Decline Service")
                            .setMessage("Are you sure you want to decline this assignment?")
                            .setPositiveButton("Yes", (d, w) ->
                                    repo.staffDeclineService(service.getServiceId(), myName,
                                            unused -> {
                                                toast("Assignment declined");
                                                FCMHelper.sendNotification("operators", "Staff Declined", myName + " declined service " + service.getServiceId(), service.getServiceId());
                                            },
                                            e -> toast("Error: " + e.getMessage())))
                            .setNegativeButton("No", null)
                            .show();
                });
            }
        } else if (!"Closed".equals(status)) {
            // Ongoing – show Update Status (Staff Lockout when closed)
            if (hasAccepted) {
                // Has accepted: can update status
                holder.statusSpinner.setVisibility(View.VISIBLE);
                holder.updateStatusBtn.setVisibility(View.VISIBLE);
                holder.uploadCompletionBtn.setVisibility(View.VISIBLE);

                // Spinner for progress statuses
                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                        context, R.layout.spinner_item, progressStatuses);
                spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                holder.statusSpinner.setAdapter(spinnerAdapter);

                int pos = spinnerAdapter.getPosition(status);
                if (pos >= 0) holder.statusSpinner.setSelection(pos);

                holder.updateStatusBtn.setOnClickListener(v -> {
                    String newStatus = holder.statusSpinner.getSelectedItem().toString();
                    repo.updateServiceStatus(service.getServiceId(), newStatus,
                            unused -> {
                                toast("Status updated to: " + newStatus);
                                if ("Delivered".equals(newStatus)) {
                                    FCMHelper.sendNotification("admins", "Service Delivered", "Service " + service.getServiceId() + " was delivered", service.getServiceId());
                                    FCMHelper.sendNotification("operators", "Service Delivered", "Service " + service.getServiceId() + " was delivered", service.getServiceId());
                                }
                            },
                            e -> toast("Update failed"));
                });

                // Photo uploads
                holder.uploadCompletionBtn.setOnClickListener(v ->
                        launchPhotoPicker("completion", service.getServiceId()));

            } else if (isAssigned) {
                // It's in progress but this specific assigned staff hasn't accepted yet
                holder.startServiceBtn.setVisibility(View.VISIBLE);
                holder.startServiceBtn.setText("ACCEPT");
                holder.notGoingBtn.setVisibility(View.VISIBLE);
                holder.notGoingBtn.setText("NOT GOING");

                holder.startServiceBtn.setOnClickListener(v -> handleStaffAcceptance(service, myName));
                
                holder.notGoingBtn.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle("Decline Service")
                            .setMessage("Are you sure you want to decline this assignment?")
                            .setPositiveButton("Yes", (d, w) ->
                                    repo.staffDeclineService(service.getServiceId(), myName,
                                            unused -> {
                                                toast("Assignment declined");
                                                FCMHelper.sendNotification("operators", "Staff Declined", myName + " declined service " + service.getServiceId(), service.getServiceId());
                                            },
                                            e -> toast("Error: " + e.getMessage())))
                            .setNegativeButton("No", null)
                            .show();
                });
            } else if (noOneAssigned) {
                 holder.startServiceBtn.setVisibility(View.VISIBLE);
                 holder.startServiceBtn.setText("ACCEPT");
                 holder.startServiceBtn.setOnClickListener(v -> handleStaffAcceptance(service, myName));
            }
        }
    }

    private void handleStaffAcceptance(Service service, String myName) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .whereIn("role", java.util.Arrays.asList("staff", "STAFF"))
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap != null && !snap.isEmpty()) {
                        List<String> staffNames = new ArrayList<>();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                            String name = doc.getString("name");
                            if (name != null && !name.equals(myName)) staffNames.add(name);
                        }
                        
                        if (staffNames.isEmpty()) {
                            proceedWithAcceptance(service, myName, new ArrayList<>());
                            return;
                        }

                        String[] namesArray = staffNames.toArray(new String[0]);
                        boolean[] checkedItems = new boolean[namesArray.length];

                        new AlertDialog.Builder(context)
                                .setTitle("Invite Co-Workers? (Optional)")
                                .setMultiChoiceItems(namesArray, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                                .setPositiveButton("Accept & Invite Selected", (dialog, which) -> {
                                    List<String> invitedStaff = new ArrayList<>();
                                    for (int i = 0; i < checkedItems.length; i++) {
                                        if (checkedItems[i]) invitedStaff.add(namesArray[i]);
                                    }
                                    proceedWithAcceptance(service, myName, invitedStaff);
                                })
                                .setNegativeButton("Just Me", (dialog, which) -> {
                                    proceedWithAcceptance(service, myName, new ArrayList<>());
                                })
                                .show();
                    } else {
                        proceedWithAcceptance(service, myName, new ArrayList<>());
                    }
                }).addOnFailureListener(e -> proceedWithAcceptance(service, myName, new ArrayList<>()));
    }
    
    private void proceedWithAcceptance(Service service, String myName, List<String> invitedStaff) {
        repo.staffAcceptService(service.getServiceId(), myName,
                unused -> {
                    toast("You accepted this service!");
                    FCMHelper.sendNotification("operators", "Staff Accepted", myName + " accepted service " + service.getServiceId(), service.getServiceId());
                    
                    if (!invitedStaff.isEmpty()) {
                        List<String> newAssigned = service.getAssignedStaff();
                        if (newAssigned == null) newAssigned = new ArrayList<>();
                        for (String inv : invitedStaff) {
                            if (!newAssigned.contains(inv)) newAssigned.add(inv);
                            FCMHelper.sendNotification("staff_" + inv.replaceAll("[^a-zA-Z0-9-_.~%]", "_"), "Peer Invitation", myName + " invited you to service " + service.getServiceId(), service.getServiceId());
                        }
                        repo.operatorAssignStaff(service.getServiceId(), newAssigned, u -> toast("Peers invited!"), e -> {});
                    }
                },
                e -> toast("Error: " + e.getMessage()));
    }

    // ─── ADMIN BUTTONS ────────────────────────────────────────────────────

    private void setupAdminButtons(ServiceViewHolder holder, Service service) {
        String status = service.getStatus() != null ? service.getStatus() : "";
        // Admin can close services that are not already Closed or Cancelled
        if (!"Closed".equals(status) && !"Cancelled".equals(status)) {
            holder.closeServiceBtn.setVisibility(View.VISIBLE);
            holder.closeServiceBtn.setText("✔ Close");
            holder.closeServiceBtn.setOnClickListener(v -> {
                if (service.getCompletionVideo() == null || service.getCompletionVideo().trim().isEmpty()) {
                    toast("Error: Completion video required before closing.");
                    return;
                }
                new AlertDialog.Builder(context)
                        .setTitle("Close Service")
                        .setMessage("Confirm closure of " + service.getServiceId() + "?")
                        .setPositiveButton("Close", (d, w) ->
                                repo.closeService(service.getServiceId(), getMyName(),
                                        unused -> {
                                            toast("Service Closed");
                                            FCMHelper.sendNotification("all", "Service Closed", "Service " + service.getServiceId() + " has been closed.", service.getServiceId());
                                            
                                            // --- EARNINGS DISTRIBUTION ---
                                            List<String> accepted = service.getAcceptedStaff();
                                            if (accepted != null && !accepted.isEmpty()) {
                                                double pool = service.getServiceCost();
                                                if (pool > 0) {
                                                    double perStaff = pool / accepted.size();
                                                    for (String sName : accepted) {
                                                        repo.addEarningsByStaffName(sName, perStaff);
                                                    }
                                                    toast("Earnings distributed: " + perStaff + " each");
                                                }
                                            }
                                        },
                                        e -> toast("Error: " + e.getMessage())))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else if ("Closed".equals(status)) {
            holder.closeServiceBtn.setVisibility(View.VISIBLE);
            holder.closeServiceBtn.setText("↺ Undo Close");
            holder.closeServiceBtn.setTextColor(Color.WHITE);
            holder.closeServiceBtn.setBackgroundColor(Color.parseColor("#757575")); // make it gray/readable
            holder.closeServiceBtn.setOnClickListener(v ->
                    new AlertDialog.Builder(context)
                            .setTitle("Undo Closure")
                            .setMessage("Reopen " + service.getServiceId() + " back to Delivered?")
                            .setPositiveButton("Reopen", (d, w) ->
                                    repo.reopenService(service.getServiceId(),
                                            unused -> toast("Service Reopened"),
                                            e -> toast("Error: " + e.getMessage())))
                            .setNegativeButton("Cancel", null)
                            .show());
        }
        // View Photos
        if (service.getCompletionVideo() != null && !service.getCompletionVideo().trim().isEmpty()) {
            holder.viewCompletionBtn.setVisibility(View.VISIBLE);
            holder.viewCompletionBtn.setOnClickListener(v -> {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(service.getCompletionVideo())));
            });
        }
    }

    // ─── PHOTO UPLOAD ─────────────────────────────────────────────────────

    public static Uri cachedCameraUri = null;
    public static String cachedMimeExt = ".jpg";

    private void launchPhotoPicker(String type, String serviceId) {
        pendingPhotoType = type;
        pendingServiceId = serviceId;

        new AlertDialog.Builder(context)
            .setTitle("Completion Video")
            .setItems(new CharSequence[]{"Record Video", "Choose existing"}, (dialog, which) -> {
                if (which == 0) {
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
                    java.io.File file = new java.io.File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES), "vid_" + System.currentTimeMillis() + ".mp4");
                    cachedMimeExt = ".mp4";
                    cachedCameraUri = androidx.core.content.FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, cachedCameraUri);
                    if (context instanceof Activity) ((Activity) context).startActivityForResult(intent, 2001);
                } else {
                    Intent pickIntent = new Intent(Intent.ACTION_PICK);
                    pickIntent.setType("video/*");
                    cachedMimeExt = ""; // Resolve remotely
                    if (context instanceof Activity) ((Activity) context).startActivityForResult(pickIntent, 2001);
                }
            })
            .show();
    }

    /**
     * Called from the hosting Activity's onActivityResult.
     */
    public static void handlePhotoResult(int requestCode, Uri mediaUri, Context ctx) {
        if (mediaUri == null || pendingServiceId == null) return;

        String type = "completion";
        String serviceId = pendingServiceId;

        String ext = cachedMimeExt;
        if (ext.isEmpty()) {
            String mime = ctx.getContentResolver().getType(mediaUri);
            ext = (mime != null && mime.contains("video")) ? ".mp4" : ".jpg";
        }

        // --- Check Video Duration ---
        if (".mp4".equals(ext)) {
            try {
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                retriever.setDataSource(ctx, mediaUri);
                String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                long timeInMillisec = Long.parseLong(time);
                retriever.close();

                if (timeInMillisec > 20000) {
                    Toast.makeText(ctx, "Video must be less than 20 seconds", Toast.LENGTH_LONG).show();
                    pendingServiceId = null;
                    cachedCameraUri = null;
                    return;
                }
            } catch (Exception e) {
                Toast.makeText(ctx, "Could not verify video length", Toast.LENGTH_SHORT).show();
                pendingServiceId = null;
                cachedCameraUri = null;
                return;
            }
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        String folderName = "completion_videos";
        StorageReference ref = storage.getReference()
                .child(folderName + "/" + serviceId + ext);
        
        Toast.makeText(ctx, "Uploading media...", Toast.LENGTH_SHORT).show();

        ref.putFile(mediaUri)
                .addOnSuccessListener(snap ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            FirestoreRepository r = new FirestoreRepository();
                            r.saveCompletionVideo(serviceId, uri.toString(), v -> Toast.makeText(ctx, "Upload Finish", Toast.LENGTH_SHORT).show(), e -> {});
                            pendingServiceId = null;
                            cachedCameraUri = null;
                        }))
                .addOnFailureListener(e -> {
                    Toast.makeText(ctx, "Upload Blocked: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    pendingServiceId = null;
                    cachedCameraUri = null;
                });
    }

    // ─── STATUS COLOR ─────────────────────────────────────────────────────

    private int statusColor(String status) {
        if (status == null) return Color.GRAY;
        switch (status) {
            case "Requested":        return Color.parseColor("#9E9E9E");
            case "Going":            return Color.parseColor("#1976D2");
            case "Inspection Done":  return Color.parseColor("#0288D1");
            case "Repair In Progress": return Color.parseColor("#F57C00");
            case "Repair Completed": return Color.parseColor("#FB8C00");
            case "Delivered":        return Color.parseColor("#43A047");
            case "Closed":           return Color.parseColor("#2E7D32");
            case "Cancelled":        return Color.parseColor("#D32F2F");
            default:                 return Color.GRAY;
        }
    }

    // ─── HELPERS ──────────────────────────────────────────────────────────

    private String safe(String s) { return s != null ? s : ""; }

    private void toast(String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private String getMyName() {
        if (context instanceof Activity) {
            SharedPreferences prefs = ((Activity) context)
                    .getSharedPreferences(LoginActivity.PREFS_NAME, Context.MODE_PRIVATE);
            String name = prefs.getString(LoginActivity.KEY_USER_NAME, "");
            if (!name.isEmpty()) return name;
        }
        // Fallback: use email prefix
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";
        return email != null && email.contains("@") ? email.split("@")[0] : email;
    }

    @Override
    public int getItemCount() { return serviceList.size(); }

    // ─── VIEW HOLDER ──────────────────────────────────────────────────────

    static class ServiceViewHolder extends RecyclerView.ViewHolder {

        MaterialCardView cardView;

        TextView serviceIdTxt, customerTxt, phoneTxt,
                machineTxt, issueTxt, locationTxt,
                staffTxt, closedByTxt, dateTxt, statusBadge;

        Spinner statusSpinner;

        Button startServiceBtn, notGoingBtn, updateStatusBtn, assignStaffBtn,
                videoBtn, uploadCompletionBtn,
                viewCompletionBtn, closeServiceBtn;

        public ServiceViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.serviceCard);

            serviceIdTxt = itemView.findViewById(R.id.serviceIdTxt);
            customerTxt  = itemView.findViewById(R.id.customerTxt);
            phoneTxt     = itemView.findViewById(R.id.phoneTxt);
            machineTxt   = itemView.findViewById(R.id.machineTxt);
            issueTxt     = itemView.findViewById(R.id.issueTxt);
            locationTxt  = itemView.findViewById(R.id.locationTxt);
            staffTxt     = itemView.findViewById(R.id.staffTxt);
            closedByTxt  = itemView.findViewById(R.id.closedByTxt);
            dateTxt      = itemView.findViewById(R.id.dateTxt);
            statusBadge  = itemView.findViewById(R.id.statusBadge);

            statusSpinner = itemView.findViewById(R.id.statusSpinner);

            startServiceBtn      = itemView.findViewById(R.id.startServiceBtn);
            notGoingBtn          = itemView.findViewById(R.id.notGoingBtn);
            updateStatusBtn      = itemView.findViewById(R.id.updateStatusBtn);
            assignStaffBtn       = itemView.findViewById(R.id.assignStaffBtn);
            videoBtn             = itemView.findViewById(R.id.videoBtn);
            uploadCompletionBtn  = itemView.findViewById(R.id.uploadCompletionBtn);
            viewCompletionBtn    = itemView.findViewById(R.id.viewCompletionBtn);
            closeServiceBtn      = itemView.findViewById(R.id.closeServiceBtn);
        }
    }
}