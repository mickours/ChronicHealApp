# ChronicHeal Design Document

## 1. Introduction
ChronicHeal is an open-source, privacy-first chronic disease tracking application. It allows users to monitor symptoms, treatments, and lifestyle factors locally on their device, facilitating better communication with healthcare providers without compromising data privacy.

## 2. Core Principles
- **Privacy First**: All data is stored locally by default. No mandatory cloud accounts.
- **Simplicity**: Easy and specialized entry methods for various health metrics.
- **Actionable Insights**: Visualization of data to identify trends and correlations.
- **Portability**: Simple export options (PDF, JSON) for sharing and backups.

## 3. Architecture Overview
The app follows **Clean Architecture** principles combined with **MVVM** (Model-View-ViewModel) or **MVI** (Model-View-Intent) for the UI layer.

### Layers:
- **UI (Presentation)**: Jetpack Compose screens, ViewModels, and UI state management.
- **Domain**: Business logic, Use Cases, and Entity definitions.
- **Data**: Repository implementations, Room Database (Local Source), and DataStore for preferences.

## 4. Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Asynchronous Programming**: Coroutines & Flow
- **Local Database**: Room (SQLite)
- **Dependency Injection**: Hilt
- **Background Tasks**: WorkManager (for reminders and periodic backups)
- **Navigation**: Compose Navigation
- **Charting**: Vico or custom Compose Canvas-based charts
- **PDF Generation**: Android Print API / PdfDocument

## 5. Data Model
A unified `Event` or `Entry` system to handle diverse data types while maintaining a consistent timeline.

- **Base Entry**: `id`, `timestamp`, `note`, `type` (Enum).
- **Specialized Types**:
    - `PainEntry`: Intensity (Scale 1-10), Location (Body Part), Type (Dull, Sharp, etc.).
    - `DrugEntry`: Name, Dosage, Unit, Frequency.
    - `SymptomEntry`: Symptom name, Severity, Duration.
    - `ActivityEntry`: Type, Duration, Intensity.
    - `MealEntry`: Description, Potential triggers.
    - `MedicalAppointment`: Doctor name, Purpose, Outcome.

## 6. Key Features Design

### 6.1. Unified Entry System
Instead of a generic form, specialized input screens for each type (e.g., a slider for pain, a searchable list for drugs) to minimize friction.

### 6.2. Calendar & Timeline
- **Monthly View**: High-level overview with icons/dots indicating activity.
- **Daily View/Timeline**: Detailed list of events for a specific day.

### 6.3. Symptom Body Scan
An interactive SVG or Canvas-based human silhouette allowing users to tap on specific body regions to log pain or symptoms.

### 6.4. Analytics & Graphs
- Correlation views: "Pain Level vs. Medication" or "Symptoms vs. Sleep".
- Trend lines for chronic progress.

### 6.5. Export Engine
- **JSON**: Raw data export for interoperability and personal backups.
- **PDF**: Formatted report tailored for medical professionals, including summary graphs and chronological logs.

## 7. Security & Privacy
- **Local-only**: The `data` layer has no network access by default.
- **Encryption**: Optional biometric lock for the app.
- **Explicit Consent**: Any data movement (export/sync) must be user-initiated.
