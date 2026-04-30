package com.example.servicemanagerapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.servicemanagerapp.repository.FirestoreRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class MyServicesFragment extends Fragment {

    // Stats cards
    TextView earningsTxt, ongoingCountTxt, completedCountTxt, greetingTxt;

    RecyclerView ongoingRecycler, completedRecycler;

    ArrayList<Service> ongoingList = new ArrayList<>();
    ArrayList<Service> completedList = new ArrayList<>();

    ServiceAdapter ongoingAdapter;
    ServiceAdapter completedAdapter;

    FirestoreRepository repo;
    FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_services, container, false);

        earningsTxt       = view.findViewById(R.id.earningsTxt);
        ongoingCountTxt   = view.findViewById(R.id.ongoingCountTxt);
        completedCountTxt = view.findViewById(R.id.completedCountTxt);
        greetingTxt       = view.findViewById(R.id.greetingTxt);

        ongoingRecycler  = view.findViewById(R.id.ongoingRecycler);
        completedRecycler = view.findViewById(R.id.completedRecycler);

        ongoingRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        completedRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        ServiceAdapter.OnPhotoRequestedListener photoListener = serviceId -> {
            // This is handled by the Activity's onActivityResult, 
            // but we can add local handling if needed.
        };

        ongoingAdapter   = new ServiceAdapter(getContext(), ongoingList, "staff", photoListener);
        completedAdapter = new ServiceAdapter(getContext(), completedList, "staff", photoListener);

        ongoingRecycler.setAdapter(ongoingAdapter);
        completedRecycler.setAdapter(completedAdapter);

        repo = new FirestoreRepository();
        auth = FirebaseAuth.getInstance();

        listenToUserProfile();
        loadMyServices();

        return view;
    }

    private void listenToUserProfile() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (doc != null && doc.exists()) {
                        Double eVal = doc.getDouble("earnings");
                        earningsTxt.setText("₹" + String.format("%.0f", eVal != null ? eVal : 0));
                    }
                });
    }

    private void loadMyServices() {
        if (auth.getCurrentUser() == null) return;

        // Get staff name from SharedPreferences (primary), fallback to email prefix
        String myName = "";
        if (getActivity() != null) {
            SharedPreferences prefs = getActivity()
                    .getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
            myName = prefs.getString(LoginActivity.KEY_USER_NAME, "");
        }
        if (myName.isEmpty()) {
            String email = auth.getCurrentUser().getEmail();
            myName = (email != null && email.contains("@")) ? email.split("@")[0] : email;
        }

        final String staffName = myName;
        greetingTxt.setText("Hello, " + staffName + "!");

        repo.listenServicesUnordered((value, error) -> {
            if (error != null || value == null) return;

            ongoingList.clear();
            completedList.clear();

            for (DocumentSnapshot doc : value.getDocuments()) {
                Service s = doc.toObject(Service.class);
                if (s == null) continue;

                List<String> assigned = s.getAssignedStaff();
                if (assigned == null || !assigned.contains(staffName)) continue;

                boolean isDone = "Closed".equals(s.getStatus()) || "Delivered".equals(s.getStatus());

                if (isDone) {
                    completedList.add(s);
                } else {
                    ongoingList.add(s);
                }
            }

            java.util.Collections.sort(ongoingList, (s1, s2) -> {
                if (s1.getServiceId() != null && s2.getServiceId() != null) return s2.getServiceId().compareTo(s1.getServiceId());
                return 0;
            });
            java.util.Collections.sort(completedList, (s1, s2) -> {
                if (s1.getServiceId() != null && s2.getServiceId() != null) return s2.getServiceId().compareTo(s1.getServiceId());
                return 0;
            });

            ongoingAdapter.notifyDataSetChanged();
            completedAdapter.notifyDataSetChanged();

            // Update stat counts
            ongoingCountTxt.setText(String.valueOf(ongoingList.size()));
            completedCountTxt.setText(String.valueOf(completedList.size()));
        });
    }
}