package com.example.servicemanagerapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.*;

import com.example.servicemanagerapp.repository.FirestoreRepository;
import com.google.firebase.firestore.*;

import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class ServiceListFragment extends Fragment {

    RecyclerView recyclerView;
    ArrayList<Service> allServices = new ArrayList<>();
    ArrayList<Service> filteredList = new ArrayList<>();
    ServiceAdapter adapter;

    FirestoreRepository repo;
    ImageButton reloadBtn;
    EditText searchBar;

    Button btnAll, btnRequested, btnOngoing, btnCompleted;

    String role = "staff";
    String activeFilter = "all"; // all | requested | ongoing | completed

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_service_list, container, false);

        recyclerView = view.findViewById(R.id.serviceRecycler);
        reloadBtn    = view.findViewById(R.id.reloadBtn);
        searchBar    = view.findViewById(R.id.searchBar);

        btnAll       = view.findViewById(R.id.btnAll);
        btnRequested = view.findViewById(R.id.btnRequested);
        btnOngoing   = view.findViewById(R.id.btnOngoing);
        btnCompleted = view.findViewById(R.id.btnCompleted);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Read role from SharedPreferences
        if (getActivity() != null) {
            role = getActivity().getIntent().getStringExtra("role");
            if (role == null) {
                SharedPreferences prefs = getActivity()
                        .getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
                role = prefs.getString(LoginActivity.KEY_ROLE, "staff");
            }
        }

        repo = new FirestoreRepository();

        adapter = new ServiceAdapter(getContext(), filteredList, role, serviceId -> {
            // Not strictly needed here as activity handles results
        });
        recyclerView.setAdapter(adapter);

        loadServices();

        // Filter buttons
        btnAll.setOnClickListener(v -> applyFilter("all"));
        btnRequested.setOnClickListener(v -> applyFilter("requested"));
        btnOngoing.setOnClickListener(v -> applyFilter("ongoing"));
        btnCompleted.setOnClickListener(v -> applyFilter("completed"));

        // Refresh button
        reloadBtn.setOnClickListener(v -> loadServices());

        // Search
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilterWithSearch(activeFilter, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadServices() {
        repo.listenAllServices((value, error) -> {
            if (error != null || value == null) return;

            allServices.clear();
            for (DocumentSnapshot doc : value.getDocuments()) {
                Service s = doc.toObject(Service.class);
                if (s != null) allServices.add(s);
            }

            java.util.Collections.sort(allServices, (s1, s2) -> {
                if ("staff".equalsIgnoreCase(role)) {
                    String myName = "";
                    if (getActivity() != null) {
                        SharedPreferences prefs = getActivity().getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE);
                        myName = prefs.getString(LoginActivity.KEY_USER_NAME, "");
                    }
                    boolean s1Mine = (s1.getAssignedStaff() != null && s1.getAssignedStaff().contains(myName)) ||
                                     (s1.getAcceptedStaff() != null && s1.getAcceptedStaff().contains(myName));
                    boolean s2Mine = (s2.getAssignedStaff() != null && s2.getAssignedStaff().contains(myName)) ||
                                     (s2.getAcceptedStaff() != null && s2.getAcceptedStaff().contains(myName));
                    if (s1Mine && !s2Mine) return -1;
                    if (!s1Mine && s2Mine) return 1;
                }
                if (s1.getServiceId() != null && s2.getServiceId() != null) {
                    return s2.getServiceId().compareTo(s1.getServiceId());
                }
                return 0;
            });

            applyFilter(activeFilter);
        });
    }

    private void applyFilter(String filter) {
        activeFilter = filter;
        String query = searchBar != null ? searchBar.getText().toString() : "";
        applyFilterWithSearch(filter, query);

        // Update button styles
        resetFilterButtons();
        switch (filter) {
            case "requested": highlightButton(btnRequested); break;
            case "ongoing":   highlightButton(btnOngoing); break;
            case "completed": highlightButton(btnCompleted); break;
            default:          highlightButton(btnAll); break;
        }
    }

    private void applyFilterWithSearch(String filter, String query) {
        filteredList.clear();

        for (Service s : allServices) {
            if (!matchesFilter(s, filter)) continue;
            if (!query.isEmpty() && !matchesSearch(s, query)) continue;
            filteredList.add(s);
        }

        adapter.notifyDataSetChanged();
    }

    private boolean matchesFilter(Service s, String filter) {
        if (s.getStatus() == null) return false;
        switch (filter) {
            case "requested":
                return "Requested".equalsIgnoreCase(s.getStatus());
            case "ongoing":
                switch (s.getStatus()) {
                    case "Going":
                    case "Inspection Done":
                    case "Repair In Progress":
                    case "Repair Completed":
                    case "Delivered":
                        return true;
                    default:
                        return false;
                }
            case "completed":
                return "Closed".equalsIgnoreCase(s.getStatus());
            default:
                return true; // "all"
        }
    }

    private boolean matchesSearch(Service s, String query) {
        String q = query.toLowerCase();
        return (s.getCustomerName() != null && s.getCustomerName().toLowerCase().contains(q))
                || (s.getServiceId() != null && s.getServiceId().toLowerCase().contains(q))
                || (s.getMachineType() != null && s.getMachineType().toLowerCase().contains(q))
                || (s.getLocation() != null && s.getLocation().toLowerCase().contains(q));
    }

    private void resetFilterButtons() {
        int defaultBg = android.graphics.Color.parseColor("#F0F0F0");
        int defaultTxt = android.graphics.Color.parseColor("#333333");
        for (Button btn : new Button[]{btnAll, btnRequested, btnOngoing, btnCompleted}) {
            btn.setBackgroundColor(defaultBg);
            btn.setTextColor(defaultTxt);
        }
    }

    private void highlightButton(Button btn) {
        btn.setBackgroundColor(android.graphics.Color.parseColor("#1565C0"));
        btn.setTextColor(android.graphics.Color.WHITE);
    }
}