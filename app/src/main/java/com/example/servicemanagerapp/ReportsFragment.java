package com.example.servicemanagerapp;

import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.servicemanagerapp.repository.FirestoreRepository;
import com.github.mikephil.charting.charts.*;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.*;

public class ReportsFragment extends Fragment {

    PieChart statusChart, machineChart;
    BarChart staffPerformanceChart, acceptanceRateChart;
    LineChart workloadChart;
    TextView avgTimeTxt;
    android.widget.Button downloadReportBtn;

    FirestoreRepository repo;
    String latestReportPayload = "Loading data...";

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        statusChart = view.findViewById(R.id.statusChart);
        machineChart = view.findViewById(R.id.machineChart);
        staffPerformanceChart = view.findViewById(R.id.staffPerformanceChart);
        acceptanceRateChart = view.findViewById(R.id.acceptanceRateChart);
        workloadChart = view.findViewById(R.id.workloadChart);
        avgTimeTxt = view.findViewById(R.id.avgTimeTxt);
        downloadReportBtn = view.findViewById(R.id.downloadReportBtn);

        repo = new FirestoreRepository();

        downloadReportBtn.setOnClickListener(v -> {
            android.content.Intent sendIntent = new android.content.Intent();
            sendIntent.setAction(android.content.Intent.ACTION_SEND);
            sendIntent.putExtra(android.content.Intent.EXTRA_TEXT, latestReportPayload);
            sendIntent.setType("text/plain");
            startActivity(android.content.Intent.createChooser(sendIntent, "Download/Share Report"));
        });

        loadRealCharts();

        return view;
    }

    private void loadRealCharts() {
        repo.listenServicesUnordered((value, error) -> {
            if (error != null || value == null) {
                if (getContext() != null) Toast.makeText(getContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Status Overview
            int requested = 0, ongoing = 0, completed = 0, cancelled = 0;
            // 2. Machine Distribution
            Map<String, Integer> machineCount = new HashMap<>();
            // 3. Staff Performance (Completed)
            Map<String, Integer> staffCompletedCount = new HashMap<>();
            // 4. Acceptance Rate (Assigned vs Accepted)
            Map<String, int[]> staffRates = new HashMap<>(); // [0] = assigned, [1] = accepted
            // 5. Avg Time Calculation
            long totalTimeMs = 0;
            int closedCountForAvg = 0;
            // 6. Peak Workload (services created per day)
            Map<String, Integer> dateCount = new TreeMap<>();

            SimpleDateFormat dayFormat = new SimpleDateFormat("MMM dd", Locale.US);

            for (DocumentSnapshot doc : value.getDocuments()) {
                Service s = doc.toObject(Service.class);
                if (s == null) continue;

                // STATUS
                String st = s.getStatus() != null ? s.getStatus() : "";
                switch (st) {
                    case "Requested": requested++; break;
                    case "Closed":    completed++; break;
                    case "Cancelled": cancelled++; break;
                    default:          ongoing++;   break;
                }

                // MACHINE
                String machineName = s.getMachineType() != null && !s.getMachineType().trim().isEmpty() ? s.getMachineType() : "Unknown";
                machineCount.put(machineName, machineCount.getOrDefault(machineName, 0) + 1);

                // PERFORMANCE (Closed)
                if ("Closed".equals(st)) {
                    List<String> accepted = s.getAcceptedStaff();
                    if (accepted != null) {
                        for (String staff : accepted) {
                            staffCompletedCount.put(staff, staffCompletedCount.getOrDefault(staff, 0) + 1);
                        }
                    }
                    // Avg calculation
                    Object createdObj = s.getCreatedTimestamp();
                    Long createdTs = null;
                    if (createdObj instanceof Long) {
                        createdTs = (Long) createdObj;
                    } else if (createdObj instanceof com.google.firebase.Timestamp) {
                        createdTs = ((com.google.firebase.Timestamp) createdObj).toDate().getTime();
                    } else if (createdObj instanceof java.util.Date) {
                        createdTs = ((java.util.Date) createdObj).getTime();
                    }
                    
                    Long closedTs = null;
                    Date closedDate = doc.getDate("closedTimestamp");
                    if (closedDate != null) closedTs = closedDate.getTime();

                    if (closedTs != null && createdTs != null && closedTs > createdTs) {
                        totalTimeMs += (closedTs - createdTs);
                        closedCountForAvg++;
                    }
                }

                // ACCEPTANCE RATE
                List<String> assigned = s.getAssignedStaff();
                List<String> accepted = s.getAcceptedStaff();
                if (assigned != null) {
                    for (String staff : assigned) {
                        if (!staffRates.containsKey(staff)) staffRates.put(staff, new int[]{0, 0});
                        staffRates.get(staff)[0]++;
                        if (accepted != null && accepted.contains(staff)) {
                            staffRates.get(staff)[1]++;
                        }
                    }
                }

                // WORKLOAD (Robust handling of Timestamp)
                Date createdDate = null;
                Object tsObj = s.getCreatedTimestamp();
                
                if (tsObj instanceof com.google.firebase.Timestamp) {
                    createdDate = ((com.google.firebase.Timestamp) tsObj).toDate();
                } else if (tsObj instanceof Long) {
                    createdDate = new Date((Long) tsObj);
                } else if (s.getCreatedAt() != null) {
                    // Fallback to parsing the string if timestamp is missing
                    try {
                        createdDate = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).parse(s.getCreatedAt());
                    } catch (Exception ignored) {}
                }

                if (createdDate != null) {
                    String dateStr = dayFormat.format(createdDate);
                    dateCount.put(dateStr, dateCount.getOrDefault(dateStr, 0) + 1);
                }

            }

            // Update Avg Time Text
            if (closedCountForAvg > 0) {
                long avgMs = totalTimeMs / closedCountForAvg;
                long hours = avgMs / (1000 * 60 * 60);
                long minutes = (avgMs % (1000 * 60 * 60)) / (1000 * 60);
                avgTimeTxt.setText(hours + "h " + minutes + "m per service");
            } else {
                avgTimeTxt.setText("No data");
            }

            // Payload builder
            StringBuilder sb = new StringBuilder();
            sb.append("ADVANCED EXECUTIVE REPORT\n=========================\n\n");
            sb.append(String.format("Avg Completion: %s\n\n", avgTimeTxt.getText().toString()));
            sb.append("STATUS: Req=").append(requested).append(", On=").append(ongoing)
              .append(", Done=").append(completed).append(", Cancel=").append(cancelled).append("\n\n");
            
            latestReportPayload = sb.toString();

            // Build Charts
            buildStatusChart(requested, ongoing, completed, cancelled);
            buildMachineChart(machineCount);
            buildStaffPerformanceChart(staffCompletedCount);
            buildAcceptanceRateChart(staffRates);
            buildWorkloadChart(dateCount);
        });
    }

    private void buildStatusChart(int req, int ongoing, int done, int cancelled) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        if (req > 0)       entries.add(new PieEntry(req, "Requested"));
        if (ongoing > 0)   entries.add(new PieEntry(ongoing, "Ongoing"));
        if (done > 0)      entries.add(new PieEntry(done, "Completed"));
        if (cancelled > 0) entries.add(new PieEntry(cancelled, "Cancelled"));

        if (entries.isEmpty()) entries.add(new PieEntry(1, "No Data"));
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#9E9E9E"), Color.parseColor("#1976D2"),
                Color.parseColor("#43A047"), Color.parseColor("#D32F2F"));
        setupPieChart(statusChart, dataSet);
    }

    private void buildMachineChart(Map<String, Integer> map) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            entries.add(new PieEntry(e.getValue(), e.getKey()));
        }
        if (entries.isEmpty()) entries.add(new PieEntry(1, "No Data"));
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(Color.parseColor("#00897B"), Color.parseColor("#5E35B1"),
                Color.parseColor("#F4511E"), Color.parseColor("#FFB300"), Color.parseColor("#E53935"));
        setupPieChart(machineChart, dataSet);
    }

    private void buildStaffPerformanceChart(Map<String, Integer> map) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            labels.add(e.getKey());
            entries.add(new BarEntry(i++, e.getValue()));
        }
        if (entries.isEmpty()) { entries.add(new BarEntry(0, 0)); labels.add("N/A"); }
        BarDataSet dataSet = new BarDataSet(entries, "Completed Services");
        dataSet.setColor(Color.parseColor("#43A047"));
        setupBarChart(staffPerformanceChart, dataSet, labels);
    }

    private void buildAcceptanceRateChart(Map<String, int[]> map) {
        // We will show Accepted as Bar, and Max value is Assigned
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, int[]> e : map.entrySet()) {
            labels.add(e.getKey());
            float assigned = e.getValue()[0];
            float accepted = e.getValue()[1];
            float percent = assigned == 0 ? 0 : (accepted / assigned) * 100f;
            entries.add(new BarEntry(i++, percent));
        }
        if (entries.isEmpty()) { entries.add(new BarEntry(0, 0)); labels.add("N/A"); }
        BarDataSet dataSet = new BarDataSet(entries, "Acceptance %");
        dataSet.setColor(Color.parseColor("#1E88E5"));
        setupBarChart(acceptanceRateChart, dataSet, labels);
        acceptanceRateChart.getAxisLeft().setAxisMaximum(100f);
        acceptanceRateChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void buildWorkloadChart(Map<String, Integer> map) {
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            labels.add(e.getKey());
            entries.add(new Entry(i++, e.getValue()));
        }
        if (entries.isEmpty()) { entries.add(new Entry(0, 0)); labels.add("N/A"); }
        
        LineDataSet dataSet = new LineDataSet(entries, "Services per Day");
        dataSet.setColor(Color.parseColor("#D81B60"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleColor(Color.parseColor("#D81B60"));
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(Color.DKGRAY);
        
        LineData data = new LineData(dataSet);
        workloadChart.setData(data);
        workloadChart.getDescription().setEnabled(false);
        workloadChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        workloadChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        workloadChart.getXAxis().setGranularity(1f);
        workloadChart.getAxisRight().setEnabled(false);
        workloadChart.getAxisLeft().setGranularity(1f);
        workloadChart.animateX(800);
        workloadChart.invalidate();
    }

    // -- CHART SETUP UTILS --

    private void setupPieChart(PieChart chart, PieDataSet dataSet) {
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(12f);
        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.setHoleRadius(50f);
        chart.setTransparentCircleRadius(55f);
        chart.setHoleColor(Color.WHITE);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(true);
        chart.setEntryLabelColor(Color.DKGRAY);
        chart.setEntryLabelTextSize(10f);
        chart.animateY(800);
        chart.invalidate();
    }

    private void setupBarChart(BarChart chart, BarDataSet dataSet, List<String> labels) {
        dataSet.setValueTextColor(Color.DKGRAY);
        dataSet.setValueTextSize(11f);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.5f);
        chart.setData(data);
        chart.getDescription().setEnabled(false);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setGranularity(1f);
        chart.getLegend().setEnabled(false);
        chart.animateY(800);
        chart.invalidate();
    }
}