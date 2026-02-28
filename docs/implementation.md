# Implementation Plan

This document outlines the implementation steps for ChronicHeal, based on the `README.md` and `docs/design.md`.

## Phase 1: Core Data Foundation and Basic UI (Completed)
Established the core data structure and a basic user interface.

### Accomplishments:
- **Dependency Setup**: Integrated Jetpack Compose, Hilt, Room, and KSP.
- **Domain Layer**: Defined `HealthEntry` and `EntryType`.
- **Data Layer**: 
    - Implemented Room database (`AppDatabase`, `EntryDao`).
    - Implemented `EntryRepository` and its implementation `EntryRepositoryImpl`.
    - Set up Hilt modules for Dependency Injection.
- **Presentation Layer**: 
    - Switched `MainActivity` to Compose.
    - Set up Material 3 Theme.
    - Cleaned up legacy View-based files.

## Phase 2: Timeline and Entry Management (In Progress)
Goal: Allow users to view their history and add new entries.

### Accomplishments:
- **Domain Layer (Use Cases)**: Implemented `GetEntriesUseCase` and `AddEntryUseCase`.
- **Presentation Layer (Navigation)**: 
    - Integrated Compose Navigation.
    - Defined routes for Timeline, Type Selection, and specialized input screens.
- **UI Components**:
    - `TimelineScreen`: Displays a list of health entries from the database.
    - `EntryTypeSelectionScreen`: Grid-based selection for different tracking types.
    - `AddPainScreen`: Specialized input with intensity slider and location field.
    - `AddDrugScreen`: Specialized input for medication and dosage.

### Remaining for Phase 2:
- **More Specialized Input Screens**: Add screens for Symptoms, Meals, and Activities.
- **Entry Edition/Deletion**: Allow users to tap an entry in the timeline to edit or delete it.
- **Calendar View**: Add a monthly calendar view to navigate the history.

## Phase 3: Analytics and Export
- Basic graphing of pain intensity over time.
- JSON export/import functionality.

## Phase 4: Advanced Features
- Body scan UI.
- PDF Report generation.
- Reminders via WorkManager.
