package com.example.servicemanagerapp;

public class User {

    private String name;
    private String email;
    private String phone;
    private String role;
    private String location;
    private String status;
    private String employeeId;
    private double earnings;
    private String profilePhoto;
    private Object createdAt;

    // Required empty constructor for Firestore
    public User() {}

    // Full constructor
    public User(String name, String email, String phone,
                String role, String location,
                String status, String employeeId, double earnings) {

        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.location = location;
        this.status = status;
        this.employeeId = employeeId;
        this.earnings = earnings;
    }

    // -------- GETTERS --------

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getLocation() { return location; }
    public String getStatus() { return status; }
    public String getEmployeeId() { return employeeId; }
    public double getEarnings() { return earnings; }
    public String getProfilePhoto() { return profilePhoto; }
    public Object getCreatedAt() { return createdAt; }

    // -------- SETTERS --------

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setRole(String role) { this.role = role; }
    public void setLocation(String location) { this.location = location; }
    public void setStatus(String status) { this.status = status; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setEarnings(double earnings) { this.earnings = earnings; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }
    public void setCreatedAt(Object createdAt) { this.createdAt = createdAt; }
}