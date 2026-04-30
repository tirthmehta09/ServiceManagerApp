package com.example.servicemanagerapp;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.ArrayList;
import java.util.List;

public class Service {

    private String serviceId;
    private String customerName;
    private String phone;
    private String machineType;
    private String issue;
    private String location;
    private String status;
    private String createdBy;
    private Object assignedStaff;
    private Object acceptedStaff;
    private String createdAt;
    private Object createdTimestamp; // Firestore Timestamp or Date
    private String videoUrl;
    private String completionVideo;
    private String closedBy;
    private double serviceCost;

    public Service() {}

    public Service(String serviceId, String customerName, String phone,
                   String machineType, String issue, String location,
                   String status, String createdBy, List<String> assignedStaff,
                   List<String> acceptedStaff,
                   String createdAt, String videoUrl,
                   String completionVideo) {

        this.serviceId = serviceId;
        this.customerName = customerName;
        this.phone = phone;
        this.machineType = machineType;
        this.issue = issue;
        this.location = location;
        this.status = status;
        this.createdBy = createdBy;
        this.assignedStaff = assignedStaff;
        this.acceptedStaff = acceptedStaff;
        this.createdAt = createdAt;
        this.videoUrl = videoUrl;
        this.completionVideo = completionVideo;
    }

    // -------- GETTERS --------

    public String getServiceId() { return serviceId; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getMachineType() { return machineType; }
    public String getIssue() { return issue; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }

    @Exclude
    public List<String> getAssignedStaff() {
        if (assignedStaff instanceof List) {
            return (List<String>) assignedStaff;
        } else if (assignedStaff instanceof String) {
            List<String> list = new ArrayList<>();
            list.add((String) assignedStaff);
            return list;
        }
        return new ArrayList<>();
    }

    @Exclude
    public List<String> getAcceptedStaff() {
        if (acceptedStaff instanceof List) {
            return (List<String>) acceptedStaff;
        } else if (acceptedStaff instanceof String) {
            List<String> list = new ArrayList<>();
            list.add((String) acceptedStaff);
            return list;
        }
        return new ArrayList<>();
    }

    @PropertyName("assignedStaff")
    public Object getAssignedStaffRaw() {
        return assignedStaff;
    }

    @PropertyName("acceptedStaff")
    public Object getAcceptedStaffRaw() {
        return acceptedStaff;
    }

    public String getCreatedAt() { return createdAt; }
    public Object getCreatedTimestamp() { return createdTimestamp; }
    public String getVideoUrl() { return videoUrl; }
    public String getCompletionVideo() { return completionVideo; }
    public String getClosedBy() { return closedBy; }
    public double getServiceCost() { return serviceCost; }

    // -------- SETTERS --------

    public void setServiceId(String serviceId) { this.serviceId = serviceId; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setMachineType(String machineType) { this.machineType = machineType; }
    public void setIssue(String issue) { this.issue = issue; }
    public void setLocation(String location) { this.location = location; }
    public void setStatus(String status) { this.status = status; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    @Exclude
    public void setAssignedStaff(List<String> assignedStaff) { this.assignedStaff = assignedStaff; }

    @Exclude
    public void setAcceptedStaff(List<String> acceptedStaff) { this.acceptedStaff = acceptedStaff; }

    @PropertyName("assignedStaff")
    public void setAssignedStaffRaw(Object assignedStaff) { this.assignedStaff = assignedStaff; }

    @PropertyName("acceptedStaff")
    public void setAcceptedStaffRaw(Object acceptedStaff) { this.acceptedStaff = acceptedStaff; }

    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public void setCreatedTimestamp(Object createdTimestamp) { this.createdTimestamp = createdTimestamp; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public void setCompletionVideo(String completionVideo) { this.completionVideo = completionVideo; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
    public void setServiceCost(double serviceCost) { this.serviceCost = serviceCost; }

    /**
     * Helper: returns a comma-joined display string of assigned staff names.
     */
    public String getAssignedStaffDisplay() {
        List<String> staffList = getAssignedStaff();
        if (staffList == null || staffList.isEmpty()) return "Not Assigned";
        StringBuilder sb = new StringBuilder();
        for (String name : staffList) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(name);
        }
        return sb.toString();
    }

    /**
     * Helper: checks if a staff member (by name) is already assigned.
     */
    public boolean isStaffAssigned(String staffName) {
        List<String> staffList = getAssignedStaff();
        return staffList != null && staffList.contains(staffName);
    }

    /**
     * Helper: checks if a staff member (by name) has accepted.
     */
    public boolean isStaffAccepted(String staffName) {
        List<String> staffList = getAcceptedStaff();
        return staffList != null && staffList.contains(staffName);
    }
}