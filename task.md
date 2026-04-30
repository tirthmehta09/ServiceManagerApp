# Service Manager App – Development Continuation

## Phase 1: Architecture Refactor
- [/] Create `repository/FirestoreRepository.java`
- [/] Create `repository/AuthRepository.java`
- [/] Update [Service.java](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/Service.java) – change `assignedStaff` from String to `List<String>`
- [/] Update [Service.java](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/Service.java) – add setters for status fields
- [/] Update [User.java](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/User.java) – add `earnings` field

## Phase 2: Role Persistence
- [ ] Update [LoginActivity](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/LoginActivity.java#12-129) – save role to SharedPreferences
- [ ] Update [MainDashboardActivity](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/MainDashboardActivity.java#14-116) – read role from SharedPreferences as fallback
- [ ] Update [ServiceListFragment](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ServiceListFragment.java#16-111) – read role from SharedPreferences
- [ ] Update [MyServicesFragment](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/MyServicesFragment.java#15-103) – read role from SharedPreferences

## Phase 3: Fix Service List & Filters
- [ ] Update [ServiceListFragment](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ServiceListFragment.java#16-111) – implement real filter logic (Requested/Ongoing/Completed)
- [ ] Update [ServiceAdapter](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ServiceAdapter.java#25-410) – fix `assignedStaff` display (List<String> instead of split)
- [ ] Update [ServiceAdapter](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ServiceAdapter.java#25-410) – "Start Service" button: staff adds *themselves* (not admin-selects)
- [ ] Update [ServiceAdapter](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ServiceAdapter.java#25-410) – "Not Going" button for staff to remove themselves
- [ ] Update [ServiceAdapter](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ServiceAdapter.java#25-410) – photo upload (completion + payment) with Firebase Storage

## Phase 4: My Services / Staff Dashboard
- [ ] Update [MyServicesFragment](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/MyServicesFragment.java#15-103) – show stat cards (Total Earnings, Ongoing, Completed)
- [ ] Implement earnings: each Closed service adds earnings per assigned staff

## Phase 5: Reports – Real Firestore Data
- [ ] Update [ReportsFragment](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ReportsFragment.java#13-104) – load real status counts from Firestore for Donut chart
- [ ] Update [ReportsFragment](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ReportsFragment.java#13-104) – load real per-staff service counts for Bar chart
- [ ] Update [ReportsFragment](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/ReportsFragment.java#13-104) – load weekly service trend from Firestore for Line chart

## Phase 6: Admin in MainDashboard
- [ ] Fix [LoginActivity](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/LoginActivity.java#12-129) – admin@service.com should also go through Firebase Auth flow (or keep bypass but pass role correctly into MainDashboardActivity)
- [ ] Ensure [AdminApprovalActivity](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/java/com/example/servicemanagerapp/AdminApprovalActivity.java#16-81) is reachable from the dashboard for admin/owner roles

## Phase 7: UI Improvements
- [ ] Update [service_item.xml](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/res/layout/service_item.xml) – rounded cards, white bg, soft shadows, status badge with rounded corners
- [ ] Update [fragment_dashboard.xml](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/res/layout/fragment_dashboard.xml) – stat cards with icons
- [ ] Update [fragment_my_services.xml](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/res/layout/fragment_my_services.xml) – stat header + earnings card
- [ ] Update [fragment_reports.xml](file:///c:/Users/Admin/AndroidStudioProjects/ServiceManagerApp%20-%20Copy/app/src/main/res/layout/fragment_reports.xml) – section headers for each chart
- [ ] Update `values/colors.xml` – add all status badge colors
- [ ] Update `values/themes.xml` – Material3 consistent theme

## Phase 8: Manifest & Permissions
- [ ] Add `READ_MEDIA_IMAGES` permission to Manifest
- [ ] Add `CALL_PHONE` permission for dial intent

## Phase 9: Verification
- [ ] Build project and check for compilation errors
