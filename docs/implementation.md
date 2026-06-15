# Implementation Plan

This document outlines the implementation steps for ChronicHeal, based on the `README.md` and `docs/design.md`.

... (previous content) ...

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

## Phase 48: Refined Missing Entry Detection (Completed)

Goal: Reduce unnecessary notifications from the missing entry detection feature by making it more
accurate and less intrusive.

**Accomplishments:**

- **User Control**: Added a "Missing Entry Notifications" toggle in Settings (disabled by default)
  to
  give users choice over these alerts.
- **Notification Throttling**: Implemented a "once per day per activity" rule to prevent repeated
  alerts for the same missing log.
- **Usage Awareness**: Added a proximity check that skips notifications if the user has logged
  anything in the last 30 minutes, assuming active app usage.
- **Stricter Consistency Rules**:
    - Increased minimum frequency threshold to 8 occurrences in 14 days.
    - Added a regularity check using standard deviation (must be < 60 minutes) to ensure alerts only
      trigger for truly routine activities.
- **Reminder Integration**: Enhanced the worker to skip notifications for activities that already
  have an active, manual reminder set.
- **Localized Strings**: Provided full English and French translations for the new setting and its
  description.

## Phase 49: Advanced Reminder Details (Completed)

Goal: Allow users to specify details for the entry created when a reminder is triggered.

**Accomplishments:**

- **UI Enhancement**: Redesigned `AddReminderScreen` to include an "Advanced Options" section.
- **Template Logic**: Integrated fields to prefill entry details (name, location, intensity, unit,
  value, note) based on the reminder's `EntryType`.
- **Autocomplete**: Provided suggestions for names, locations, and units using data from existing
  entries via `RemindersViewModel`.
- **Persistence**: Updated `RemindersViewModel` to save and update the linked `templateEntryId`
  in the `Reminder` model when saving details.
- **Consistency**: Used existing UI components like `AutoCompleteTextField` and `Slider` to match
  the rest of the app's logging experience.

## Phase 50: Refactor (In Progress)

Goal: Simplify the ChronicHeal codebase, reduce boilerplate, improve state management, and ensure a
cleaner separation of concerns.

### Phase 50.1: Architecture & State Management (Simplification)

- **Consolidate ViewModel Logic**: Created dedicated UseCases (`GetSuggestionsUseCase`,
  `SaveReminderUseCase`, `DeleteReminderUseCase`, `ToggleReminderUseCase`) to handle shared logic.
  Created a shared `AddEntryViewModel` to manage state during entry creation. (Completed)
- **State Class Pattern**: Standardize `UiState` across all screens using a single `StateFlow`
  pattern for complex screens to improve maintainability. (Completed for `AddEntryViewModel`)
- **Dependency Injection Cleanup**: Audit Hilt modules to ensure concise bindings and correct
  scoping (`@ViewModelScoped`, `@Singleton`).

### Phase 50.2: Feature Modularization & Component Reuse

- **UI Component Library**: Moved repetitive components (e.g., `AutoCompleteTextField`,
  `IntensityField`, `TimePickerDialog`) from `AddEntryComponents.kt` into a dedicated
  `presentation/components` package. (Completed)
- **Entry Handling Abstraction**: Created `AddEntryScaffold` in `AddEntryLayout.kt` to handle common
  scaffold/logic (date/time pickers, save buttons) for all entry types. Updated all
  `Add...Screen.kt` files to use this new layout and the new `AddEntryViewModel`. (Completed)

### Phase 50.4: Voice Logging & AI Refactor (Completed)

Goal: Integrate `LlmManager` into the dependency injection system and enhance its capabilities for
voice logging.

**Accomplishments:**

- **DI Integration**: Refactored `LlmManager` from a concrete class to an interface with
  flavor-specific implementations (`LiteLlmManager`, `FullLlmManager`).
- **Hilt Setup**: Configured `AiModule` to provide the appropriate `LlmManager` instance based on
  the build flavor.
- **Enhanced AI capabilities**:
    - Added `processLog` to `LlmManager` to extract multiple health entries from natural language
      text.
    - Updated `FullLlmManager` with a specialized prompt for multi-entry extraction.
    - Fixed `analyzeMeal` prompt to correctly match the `AiMealAnalysis` data structure.
- **UI Refactor**: Updated `VoiceLoggingScreen` to use the injected `AddEntryViewModel` and its new
  `processLog` capability, removing manual instantiation of the AI manager.
- **ViewModel Update**: Added `processLog` to `AddEntryViewModel` to bridge the UI and the AI
  layer.

## Phase 52: Robust Data Import/Export (Completed)

Goal: Fix crashes and improve reliability when importing data from JSON files.

**Accomplishments:**

- **Main Thread Safety**: Moved database version checks and JSON serialization/deserialization to
  `Dispatchers.IO` in both `ExportDataUseCase` and `ImportDataUseCase` to prevent ANRs and crashes.
- **Robust Parsing**: Enhanced `ImportDataUseCase` with `ignoreUnknownKeys`, `coerceInputValues`,
  and `isLenient` settings in `kotlinx.serialization` to better handle diverse JSON sources and
  older
  backups.
- **Better Error Handling**: Implemented multi-stage parsing (BackupData vs List<HealthEntry>) with
  explicit error messages for empty files, invalid formats, or missing data.
- **UI Performance**: Updated `SettingsScreen` to read backup files on a background thread using
  coroutines, ensuring the UI remains responsive during the process.
- **Improved Feedback**: Standardized the display of error messages from the import process to the
  user via the existing snackbar system.
