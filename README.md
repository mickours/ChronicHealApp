# 🏥 ChronicHeal

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.22-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-Android-green.svg?logo=android)](https://www.android.com)

**ChronicHeal** is an open-source, privacy-first mobile application designed to help individuals managing chronic conditions track their health journey. Unlike traditional apps, ChronicHeal stores all your data locally, requires no account, and puts you in complete control of your medical history.

---

## ✨ Key Features

### 📝 Comprehensive Tracking
Log everything that matters to your health journey using specialized, easy-to-use input screens:
- **What occurs to you (Occurrences):** Pain levels, Symptoms, Diseases/Conditions, and External Factors (weather, stress).
- **What you can manage (Management):** Medications, Meals, Sleep, Activities, and Medical Appointments.
- **Journal:** A dedicated space for your thoughts and daily reflections.

### 📅 Visual History
- **Calendar View:** Monthly overview with heatmaps indicating activity and symptom intensity.
- **Interactive Timeline:** A detailed, searchable chronological log of all your health entries.
- **Day View:** Focus on a specific day to review or add new entries contextually.

### 📊 Insights & Export
- **Advanced Analytics:** Visualize your progress with charts (Pain Line Charts, Symptom Bar Charts).
- **PDF Reporting:** Generate professional medical reports with embedded graphs to share with healthcare providers.
- **Data Portability:** Full JSON Export/Import functionality for backups and personal data ownership.

### 🔒 Privacy & Security
- **Local-First:** Your data stays on your device. No cloud required.
- **Biometric Lock:** Secure your sensitive health information using your device's fingerprint or face recognition.
- **No Account Needed:** Start tracking immediately without sign-ups or subscriptions.

### 🛠️ Specialized Tools
- **Symptom Body Scan:** Interactive human silhouette to quickly log pain or symptoms by body region.
- **Smart Reminders:** High-precision reminders for medications, meals, appointments, and activities.
- **Autocomplete:** Intelligent suggestions based on your history to reduce data entry friction.
- **Voice commands**: for hands-free entry logging.

---

## 🚀 Getting Started

### Prerequisites
- Android device running Android 7.0 (API 24) or higher.
- (For Developers) Android Studio Iguana or later.

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/mickours/ChronicHeal.git
   ```
2. Open the project in Android Studio.
3. Build and run on your device or emulator.

---

## 🏗️ Architecture

ChronicHeal follows **Clean Architecture** principles combined with the **MVVM/MVI** pattern for a robust and maintainable codebase:
- **Presentation:** Jetpack Compose for a modern, reactive UI.
- **Domain:** Pure Kotlin business logic and Use Cases.
- **Data:** Room for local SQLite persistence and DataStore for user preferences.

**Tech Stack:**
- **UI:** Jetpack Compose, Material 3
- **DI:** Hilt
- **Async:** Coroutines & Flow
- **Database:** Room
- **Navigation:** Compose Navigation
- **Charts:** Vico
- **Background Tasks:** WorkManager

---

## 👨‍💻 Development

### Running Tests
To ensure the reliability of business logic, such as PDF generation and data processing, we use unit tests.

**Via Android Studio:**
1. Open the `Project` tool window.
2. Navigate to `app/src/test/java/org/chronicheal/app`.
3. Right-click the folder or a specific test file (e.g., `ExportPdfUseCaseTest`).
4. Select `Run 'Tests in org.chronicheal.app'`.

**Via Command Line:**
Run the following command in the project root:
```bash
./gradlew test
```

---

## 🗺️ Roadmap

- [ ] Find correlation between symptoms and management events (e.g., Pain vs. Sleep).
- [ ] Integration with wearables for automatic sleep and activity tracking.
- [ ] Custom dashboard panels and graph configurations.
- [ ] iOS application (KMP migration).

---

## 🤝 Contributing

Contributions are welcome! Whether it's a bug report, a feature request, or a pull request, we appreciate your help in making ChronicHeal better.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

Distributed under the GNU General Public License v3.0. See `LICENSE` for more information.

---

*ChronicHeal - Empowering you to understand your health better.*
