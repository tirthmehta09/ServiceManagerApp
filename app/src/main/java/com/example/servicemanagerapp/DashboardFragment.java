package com.example.servicemanagerapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DashboardFragment extends Fragment {

    TextView totalTxt,pendingTxt,ongoingTxt,completedTxt;

    FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_dashboard,
                container,
                false
        );

        totalTxt = view.findViewById(R.id.totalTxt);
        pendingTxt = view.findViewById(R.id.pendingTxt);
        ongoingTxt = view.findViewById(R.id.ongoingTxt);
        completedTxt = view.findViewById(R.id.completedTxt);

        db = FirebaseFirestore.getInstance();

        loadCounts();

        return view;
    }

    private void loadCounts(){

        db.collection("services")
                .addSnapshotListener((value,error)->{

                    if(value==null) return;

                    int total=0;
                    int requested=0;
                    int progress=0;
                    int completed=0;

                    for(DocumentSnapshot doc:value.getDocuments()){

                        total++;

                        String status = doc.getString("status");

                        if("Requested".equals(status)) requested++;

                        if("Going".equals(status) ||
                                "Repair In Progress".equals(status))
                            progress++;

                        if("Closed".equals(status)) completed++;
                    }

                    totalTxt.setText(String.valueOf(total));
                    pendingTxt.setText(String.valueOf(requested));
                    ongoingTxt.setText(String.valueOf(progress));
                    completedTxt.setText(String.valueOf(completed));
                });
    }
}