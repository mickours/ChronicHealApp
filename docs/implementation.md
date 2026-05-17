# Implementation Plan

This document outlines the implementation steps for ChronicHeal, based on the `README.md` and `docs/design.md`.

... (previous content) ...

## Phase 46: Build Variants & Lite Version (Completed)

Goal: Create a lightweight version of the app that excludes the heavy AI (MediaPipe) dependencies.

**Accomplishments:**

- **Product Flavors**: Introduced `full` and `lite` flavors in `build.gradle.kts`.
- **Dependency Isolation**: Moved the `com.google.mediapipe:tasks-genai` dependency to the
  `fullImplementation` configuration. This significantly reduces the APK size and memory footprint
  of the lite build.
- **Architecture Abstraction**:
    - Created a core `LlmManager` interface in the `main` source set.
    - Implemented `FullLlmManager` in the `full` source set containing the actual MediaPipe logic.
    - Implemented `LiteLlmManager` in the `lite` source set with "no-op" (do nothing)
      implementations.
- **Dependency Injection**: Configured separate Dagger/Hilt `AiModule`s for each flavor to inject
  the appropriate implementation at compile time.
- **UI Adaptation**: Added an `isAiEnabled` flag to the `LlmManager` interface, allowing the UI (
  e.g., `AddMealScreen`) to cleanly hide AI-related buttons when compiled in the lite flavor.

## Phase 47: Enhanced Checkup Reminders with Templates (Completed)

Goal: Improve the medication section in the checkup screen by prefilling dosage and cleaning up drug
names using a template feature.

**Accomplishments:**

- **Schema Update**: Added `templateEntryId` to the `Reminder` model to link a reminder to a
  specific `HealthEntry` serving as a data template.
- **Database Migration**: Implemented a Room migration (version 10 to 11) to add the
  `templateEntryId` column to the `reminders` table.
- **Data Repository**: Updated `EntryDao` and `EntryRepository` to return the ID of newly inserted
  entries.
- **ViewModel Logic**: Enhanced `TimelineViewModel` to automatically link new reminders to the
  entries they were created with.
- **UI Improvements**: Updated `AddCompleteEntryScreen` (Checkup) to fetch and use template entries
  for medications.
    - Drug names are now cleaned of the "Medication: " prefix if no template is found.
    - Dosage (value and unit) is automatically prefilled when the medication is checked.
    - The dosage is displayed under the drug name in the checkup list for better visibility.
