# SaiFit

SaiFit is a comprehensive mobile application designed to evaluate and track athlete fitness using AI-powered video analysis. The application supports recording and analyzing various fitness benchmarks, such as sit-ups, vertical jumps, shuttle runs, and endurance stretches, to provide accurate performance metrics.

## Features

- **Athlete and Admin Roles:** Secure authentication and role-based access control for athletes and administrators.
- **Fitness Benchmarking:** Record and submit videos for different fitness exercises.
- **AI-Powered Evaluation:** Automated analysis of recorded videos using computer vision to detect reps, form, and overall performance.
- **Interactive Video Review:** Review recorded attempts, replay videos, and verify AI-generated skeletons directly in the app before submission.
- **Gamification & Progress Tracking:** Track your progress over time with built-in leaderboards and achievement badges.
- **Offline Mode:** Designed to queue submissions when offline and sync when a network connection is available.
- **Modern UI:** Built fully in Jetpack Compose featuring a clean, responsive, and material-design inspired interface.

## Tech Stack

- **Platform:** Android
- **UI Toolkit:** Jetpack Compose
- **Language:** Kotlin
- **Architecture:** MVVM
- **Video Processing:** CameraX, OpenCV/MediaPipe (Backend integration)
- **Local Storage:** Room Database, SharedPreferences DataStore

## Getting Started

1. Clone the repository to your local machine.
2. Open the project in Android Studio.
3. Sync the project with Gradle files.
4. Build and run the app on an Android device or emulator.

## Project Structure

- `app/src/main/java/com/saifit/app/ui/` - Contains all Jetpack Compose UI components, screens, and viewmodels.
- `app/src/main/java/com/saifit/app/data/` - Contains models, local database configurations, and repositories.
- `app/src/main/java/com/saifit/app/network/` - Backend communication logic and API definitions.

## License

This project is proprietary and confidential. Unauthorized copying or distribution of this code is strictly prohibited.
