# Service Manager App

A robust Android application designed for managing service requests, staff assignments, and reporting. Built with a modern tech stack focused on real-time data and role-based access.

## 🚀 Features

- **Role-Based Access**: Specialized dashboards for Admins, Owners, and Staff.
- **Service Lifecycle Management**: Track services from Requested → Ongoing → Completed → Closed.
- **Real-time Synchronization**: Powered by Firebase Firestore for live updates.
- **Reporting & Analytics**: Visual insights through Donut, Bar, and Line charts.
- **Staff Earnings Tracking**: Automatic calculation of earnings for completed services.
- **Media Uploads**: Attach photos for completion and payment verification via Firebase Storage.

## 🛠 Tech Stack

- **Platform**: Android (Java)
- **Backend**: Firebase Auth, Cloud Firestore, Firebase Storage
- **UI Components**: Material Design 3, MPAndroidChart (for reporting)
- **Architecture**: Repository Pattern for clean data handling

## 📂 Project Structure

- `app/`: Main Android application module.
- `functions/`: (Optional) Backend triggers/functions for service logic.
- `repository/`: Data layer abstraction for Firestore and Auth.

## 📝 Current Development Status

The project is currently in active development. Key upcoming features include:
- Persistence of user roles across sessions.
- Enhanced filtering for service lists.
- Integrated photo upload for service verification.

---
*Created as part of a Service Management solution.*

## 👥 Authors

- **Tirth Mehta** - [tirthmehta09](https://github.com/tirthmehta09)
- **Krimy Shah** - [krimyyy](https://github.com/krimyyy)
- **Dhara Patel** - [dharapatel76](https://github.com/dharapatel76)
