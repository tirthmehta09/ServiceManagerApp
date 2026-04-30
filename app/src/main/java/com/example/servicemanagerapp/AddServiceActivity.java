package com.example.servicemanagerapp;

import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.*;

public class AddServiceActivity extends AppCompatActivity {

    EditText customerName, phone, machineType, issue, location, serviceCost;
    AutoCompleteTextView staffSpinner;
    Button videoBtn, createServiceBtn;
    List<String> staffList = new ArrayList<>();
    Uri videoUri;

    FirebaseFirestore db;
    FirebaseAuth auth;
    FirebaseStorage storage;

    ActivityResultLauncher<String> videoPicker;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_service);

        customerName = findViewById(R.id.customerName);
        phone = findViewById(R.id.phone);
        machineType = findViewById(R.id.machineType);
        issue = findViewById(R.id.issue);
        location = findViewById(R.id.location);
        serviceCost = findViewById(R.id.serviceCost);
        staffSpinner = findViewById(R.id.staffSpinner);

        videoBtn = findViewById(R.id.videoBtn);
        createServiceBtn = findViewById(R.id.createServiceBtn);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // Load Staff into Spinner
        staffList.add("Unassigned");
        db.collection("users").whereIn("role", Arrays.asList("staff", "STAFF")).get()
                .addOnSuccessListener(snap -> {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snap.getDocuments()) {
                        String sName = doc.getString("name");
                        if (sName != null) staffList.add(sName);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, staffList);
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    staffSpinner.setAdapter(adapter);
                });

        // VIDEO PICKER
        videoPicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                            retriever.setDataSource(this, uri);
                            String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                            long timeInMillisec = Long.parseLong(time);
                            retriever.close();

                            if (timeInMillisec > 20000) {
                                Toast.makeText(this, "Video must be less than 20 seconds", Toast.LENGTH_LONG).show();
                                videoUri = null;
                            } else {
                                videoUri = uri;
                                Toast.makeText(this, "Video Selected", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Could not verify video length", Toast.LENGTH_SHORT).show();
                            videoUri = null;
                        }
                    }
                }
        );

        videoBtn.setOnClickListener(v -> videoPicker.launch("video/*"));

        createServiceBtn.setOnClickListener(v -> {

            createServiceBtn.setEnabled(false);
            createServiceBtn.setText("Creating Service...");

            createService();
        });
    }

    private void createService(){

        String name = customerName.getText().toString().trim();
        String phoneTxt = phone.getText().toString().trim();
        String machine = machineType.getText().toString().trim();
        String issueTxt = issue.getText().toString().trim();
        String loc = location.getText().toString().trim();
        String costStr = serviceCost.getText().toString().trim();
        String staff = staffSpinner.getText().toString().trim();

        if(name.isEmpty() || phoneTxt.isEmpty() || machine.isEmpty()
                || issueTxt.isEmpty() || loc.isEmpty() || costStr.isEmpty() 
                || staff.isEmpty() || "Unassigned".equals(staff) || videoUri == null){

            Toast.makeText(this,"All fields and initial video are compulsory",Toast.LENGTH_SHORT).show();

            createServiceBtn.setEnabled(true);
            createServiceBtn.setText("Add Service");

            return;
        }

        double cost = 0;
        if (!costStr.isEmpty()) {
            try { cost = Double.parseDouble(costStr); } catch (Exception e) {}
        }

        generateServiceIdAndSave(name, phoneTxt, machine, issueTxt, loc, cost);
    }

    private void generateServiceIdAndSave(String name,String phoneTxt,String machine,
                                          String issueTxt,String loc, double cost){

        db.collection("metadata")
                .document("serviceCounter")
                .get()
                .addOnSuccessListener(doc -> {

                    long counter = 1;

                    if(doc.exists()){
                        Long value = doc.getLong("count");
                        if(value != null) counter = value + 1;
                    }

                    String serviceId = String.format("SRV%03d", counter);

                    Map<String,Object> update = new HashMap<>();
                    update.put("count",counter);

                    db.collection("metadata")
                            .document("serviceCounter")
                            .set(update);

                    String date = new SimpleDateFormat(
                            "dd-MM-yyyy HH:mm",
                            Locale.getDefault()
                    ).format(new Date());

                    if(videoUri != null){
                        uploadVideo(serviceId,name,phoneTxt,machine,issueTxt,loc,date,cost);
                    }else{
                        saveService(serviceId,name,phoneTxt,machine,issueTxt,loc,date,"",cost);
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,"Failed to generate Service ID",
                            Toast.LENGTH_SHORT).show();

                    createServiceBtn.setEnabled(true);
                    createServiceBtn.setText("Add Service");
                });
    }

    private void uploadVideo(String serviceId,String name,String phoneTxt,String machine,
                             String issueTxt,String loc,String date, double cost){

        StorageReference ref = storage.getReference()
                .child("service_videos/" + serviceId + ".mp4");

        ref.putFile(videoUri)

                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                saveService(serviceId,name,phoneTxt,machine,issueTxt,loc,date,uri.toString(), cost)
                        ))

                .addOnFailureListener(e -> {

                    Toast.makeText(this,"Video upload failed",
                            Toast.LENGTH_SHORT).show();

                    createServiceBtn.setEnabled(true);
                    createServiceBtn.setText("Add Service");
                });
    }

    private void saveService(String serviceId,String name,String phoneTxt,String machine,
                             String issueTxt,String loc,String date,String videoUrl, double cost){

        Map<String,Object> service = new HashMap<>();

        service.put("serviceId",serviceId);
        service.put("customerName",name);
        service.put("phone",phoneTxt);
        service.put("machineType",machine);
        service.put("issue",issueTxt);
        service.put("location",loc);
        service.put("status","Requested");
        service.put("createdBy",auth.getCurrentUser().getEmail());
        service.put("serviceCost", cost);
        
        List<String> assignedStaffList = new ArrayList<>();
        if (staffSpinner != null && staffSpinner.getText() != null) {
            String selectedStaff = staffSpinner.getText().toString();
            if (!"Unassigned".equals(selectedStaff) && !selectedStaff.isEmpty()) {
                assignedStaffList.add(selectedStaff);
            }
        }
        service.put("assignedStaff", assignedStaffList);
        
        service.put("createdAt",date);
        service.put("videoUrl",videoUrl);

        // ⭐ IMPORTANT FOR SORTING
        service.put("createdTimestamp", new Date());

        db.collection("services")
                .document(serviceId)
                .set(service)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this,"Service Created",
                            Toast.LENGTH_LONG).show();

                    // FCM Trigger: New service created -> notify EVERYONE
                    FCMHelper.sendNotification("all", "New Service Request", "A new request " + serviceId + " has been created.", serviceId);

                    createServiceBtn.setEnabled(true);
                    createServiceBtn.setText("Add Service");

                    finish();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(this,"Failed to create service",
                            Toast.LENGTH_SHORT).show();

                    createServiceBtn.setEnabled(true);
                    createServiceBtn.setText("Add Service");
                });
    }
}