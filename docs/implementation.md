# Implementation Plan

This document outlines the implementation steps for ChronicHeal, based on the `README.md` and `docs/design.md`.

## Phase 1: Core Data Foundation and Basic UI (Completed)
Established the core data structure and a basic user interface.

## Phase 2: Timeline and Entry Management (Completed)
Goal: Allow users to view their history and add new entries.

### Accomplishments:
- **Unified Entry System**: Implemented all specialized screens (Pain, Drugs, Symptoms, Disease, Meals, Sleep, Medical Appointment, Activity, External Factors, Journal, Period, Beverage, Stool, Mood).
- **Entry Edition**: Added support for editing existing entries. Users can tap any entry in the Timeline or Day View to modify its details.
- **Intensity Tracking**: All "Occurrence" categories now include an intensity/impact level.
- **Calendar View**: Added a monthly overview with data markers for active days.
- **Calendar UX**: Highlighted today's date and added a "Go to Today" button for easier navigation.
- **Day View**: Contextual list of events for a specific day with adding support.
- **Navigation**: Smart contextual navigation (Save -> Return to Day View).
- **Grouped Timeline**: Date delimiters (Year, Month, Day) for better readability.
- **Sticky Headers**: Pinned chronological context (Year, Month, Full Date) at the top of the timeline during scrolling.
- **Visual Coding**: Categorized entries into "What occurs to you" (Red) cognizant and "What you can manage" (Green) using vertical color stripes on timeline cards.
- **Adaptive Indicators**: Occurrence dots in the calendar now grow in size based on the sum of intensities for that day, creating a visual health heatmap.

## Phase 3: Analytics and Export (Completed)
Goal: Visualize data trends and ensure data portability.

### Accomplishments:
- **Data Portability**: JSON Export/Import via Storage Access Framework.
- **Analytics**: Grouped Charts (Pain Line Chart & Symptom Bar Chart) with period selection (Week, Month, Year).
- **Medical Reporting**: PDF Export with embedded visual graphs and chronological logs.

## Phase 4: Advanced Features (Completed)
Goal: Interactive tools and notifications.

### 1. Symptom Body Scan (Completed)
- **Interactive Silhouette**: Tap on body regions to pre-fill location in logs.
- **SVG Integration**: Replaced the hardcoded humanoid drawing with a professional SVG-based silhouette.
- **Precise Hit Detection**: Implemented path-based hit detection using `android.graphics.Region`.
- **Auto-scaling**: Silhouette automatically scales to fit the screen while maintaining aspect ratio.
- **Interactive Intensity Selection**: Implemented "hold-to-increase" intensity logic.
- **Body Scan Reminders**: Added dedicated management for Body Scan prompts.

### 2. Reminders (Completed)
- **Exact Scheduling**: Used `AlarmManager` for high-precision health reminders.
- **Integrated Creation**: Users can now set reminders directly from relevant entry screens.
- **Notification Quick Actions**: Added "10 min" and "1 hour" snooze actions.

## Phase 5: Polish and Privacy (Completed)
- **Enhanced Entry Model**: Added `durationMinutes` and `isFinished` properties.
- **Swipe-to-Action**: Implemented `SwipeToDismissBox` for deletion and completion.
- **UI/UX Refinements**:
    - **Factorized Add Screens**: Introduced `AddEntryScaffold` to share common logic.
    - **Custom Theme**: Implemented a Soft Blue (`#a7c7d4`) and complementary Peach/Orange palette.
    - **Intelligent Autocomplete**: Integrated `ExposedDropdownMenuBox` in entry creation screens.
- **Biometric Lock Implementation**: Mandatory authentication on app startup when enabled.
- **Search and Filters**: Added a dynamic search bar and type-based filtering chips.

## Phase 6: Onboarding and Personalization (Completed)
- **Startup Welcome Wizard**: Multi-step onboarding flow using `HorizontalPager`.
- **Quick Add Shortcuts**: Integrated a "QUICK ADD" bar at the bottom of the `TimelineScreen`.

## Phase 7: Specialized Entry Types (Completed)
- **Period Tracking**: Added `EntryType.PERIOD` with a dedicated flow intensity slider.
- **Beverage Tracking**: Added `EntryType.BEVERAGE` to track drinks and quantities.
- **Stool Tracking**: Added `EntryType.STOOL` with optional aspect logging.
- **Mood Tracking**: Added `EntryType.MOOD` with a 1-10 slider and dynamic emoji feedback.

## Phase 8: Technical Foundation and Modernization (Completed)
- **Edge-to-Edge Layout**: Enabled content drawing behind system bars with safe drawing padding.
- **Navigation Enhancements**: Smooth horizontal slide transitions between screens.
- **Release Build Optimization**: Hardened ProGuard rules for Hilt, Serialization, and Coroutines.

## Phase 10: State Persistence and Configuration Robustness (Completed)
- **Configuration Change Resilience**: Updated all entry screens to use `rememberSaveable` for UI state.
- **Component Refactoring**: Moved `PainEntryForm` to `AddEntryComponents.kt` for centralized reuse.

## Phase 11: Expanded Logging Capabilities (Completed)
- **New Entry Types**:
    - **Beverage**: Users can log beverage names (autocompleted) and specific quantities (e.g., 250ml).
    - **Stool**: Simple logging for bowel movements with an optional "aspect" field (autocompleted).
    - **Mood**: High-fidelity mood tracking with a 1-10 slider that updates a central smiley and text label (e.g., "Very Bad" to "Amazing").
- **Analytics Integration**: Updated correlation analytics to handle new types (e.g., Beverage quantity sums, Mood intensity averages).

## Phase 12: Advanced Analytics (Completed)
- **Correlation Insights**: Implemented Pearson correlation coefficient calculation in `AnalyticsViewModel` to quantify the relationship between different health metrics (e.g., Pain vs. Sleep).
- **Correlation UI**: Added visual feedback in `AnalyticsScreen` with text-based insights based on the correlation score (Strong, Moderate, No correlation).
- **UX Refinement**: Added an explicit "Save" button in the TopAppBar of entry screens via `AddEntryScaffold` to improve discoverability and prevent accidental loss of data on back navigation.
- **Stability**: Fixed build errors related to missing `onSaveSuccess` parameter in various add/edit screens.

## Phase 13: Notification and UX Refinements (Completed)
- **Quick Log from Notifications**: Added a "Quick Log" button to health reminders. When pressed, it automatically creates a new entry of the same type and values as the original entry that created the reminder, but with the current timestamp.
- **DAO & Repository Enhancement**: Added `getEntryByReminderId` to `EntryDao` and `EntryRepository` to support the Quick Log feature.
- **Swipe-to-Dismiss Adjustment**: Increased the positional threshold for the swipe-to-delete action on the timeline cards to 60% of the screen width to prevent accidental deletions.

## Phase 14: Smart Autocomplete and Enhanced Calendar UX (Completed)
- **Frequency-Based Autocomplete**:
    - Updated `TimelineViewModel` to sort all entry suggestions by usage frequency.
    - Enhanced `AutoCompleteTextField` with a horizontal row of frequency-sorted clickable `SuggestionChip`s directly below the input field.
    - Labels use the theme's `PrimaryContainerLight` (light button) color.
    - Top 5 suggestions are displayed even when the field is empty and filter in real-time as the user types.
- **Calendar Redesign**:
    - Removed the secondary reminder list from the `CalendarScreen` to provide a full-screen grid.
    - Implemented a collapsible, scrollable section for daily entry details below the grid.
    - Daily entries are now sorted by intensity level.
    - Drug entries in the daily list now display dosage (value/unit).

## Phase 15: Interactive Reminder Logging (Completed)
- **Renamed "Quick Log" to "Log Now"**: Improved clarity for the notification action.
- **Direct Entry Editing from Notifications**:
    - Instead of background insertion, tapping "**Log Now**" now opens the app directly to the entry's creation screen.
    - The screen is pre-filled using the previous entry associated with that reminder as a template.
    - ID and Timestamp are reset to ensure it's saved as a new record, while preserving dosage, name, and notes.
- **Smart Navigation Routing**: Updated `MainActivity` and `ReminderReceiver` to pass and handle `EXTRA_REMINDER_ID` for contextual screen initialization.

## Phase 16: Swipe-to-Delete Removal (Completed)
- **Removal of Swipe-to-Action**: Removed `SwipeToDismissBox` from the `TimelineScreen` as it led to unwanted deletions. Entries can still be modified by clicking on them.

## Phase 17: Calendar Search and Filtering (Completed)
- **Integrated Search & Filters**: Replicated the search bar and type-filtering from the Timeline onto the Calendar screen.
- **Dynamic Calendar Heatmap**: The calendar grid dots now update in real-time based on the search query and selected filters.

## Phase 18: Enhanced Analytics and Reporting (Completed)
- **Clickable Legends**: Legend items in evolution charts are now clickable to toggle visibility.
- **Stacked Timeline Charts**: Replaced pie charts with cumulative stacked timeline charts.
- **Detailed PDF Reports**: Added high-fidelity stacked charts and distribution histograms to PDF exports.

## Phase 19: PDF Report Optimization (Completed)
- **Improved Layout**: Implemented date-grouped logs and efficient vertical flow.

## Phase 20: Notification UX and Branding (Completed)
- **Launcher Icons**: Implemented high-fidelity launcher and notification icons based on the app logo.

## Phase 21: Weekly Insights (Completed)
- **Actionable Summary**: Added a "Last 7 Days Insights" card to the Timeline.

## Phase 22: Voice Logging and UI Polish (Completed)
- **Multi-Entry Voice Logging**: Support for multiple entries in one voice command.
- **Permission Transparency**: One-time rationale dialog for microphone access.
- **Timeline UX**: Reduced Quick Add section height.
- **Empty State Illustrations**: Added the "Body-dont-know" SVG illustration to the empty states of the Reminders and Body Scan Reminders screens for better visual feedback.

## Phase 23: Unified Assessment and Voice Prioritization (Completed)
- **Voice Entry Prioritization**: Moved Voice Logging to the top of entry selection.
- **Unified Check-in**: Merged Body Scan and Full Check-in into a single scrollable assessment.

## Phase 24: Intelligent Defaults and Localization (Completed)
- **Intelligent Sleep Defaults**: Duration defaults based on time of day.
- **Scale Standardization**: Sleep Quality standardized to 1-10.
- **Body Scan Localization**:
    - **Technical IDs**: Pain entries now store the technical ID from the SVG (e.g., `shoulder_left`) instead of the localized name.
    - **On-the-fly Translation**: The UI uses `formatId(context, id)` to translate these IDs into the current system language (English/French) for display.

## Phase 25: Enhanced Timeline Detail Visibility (Completed)
- **Increased Information Density**: Updated the `EntryItem` component to show all filled-in fields directly in the timeline.
- **Contextual Data Display**: 
    - Meal entries now list their ingredients and quantities.
    - Medication and beverage entries display their specific dosages/volumes.
    - Duration is shown for any entry where it's recorded (e.g., activities, sleep).
    - Location and Name are both shown if they exist and aren't redundant with the title.

## Phase 32: Automated Rolling Backups (Completed)
- **Automated Backup System**: Implemented a daily automated backup using `WorkManager`.
- **Rolling Retention Logic**:
    - Keeps all backups from the **last 7 days**.
    - Keeps the **first backup of each month** for all previous months.
    - Automatically purges older redundant files to manage storage usage.
- **Internal Storage**: Backups are stored in the app's internal files directory (`/backups`) for maximum privacy and security.
- **User Control**: Added a toggle in the Settings screen to enable or disable automated backups.

## Phase 33: Checkup and Integrated Reminders (Completed)
- **Renamed to Checkup**: Rebranded "Complete Check-in" as "**Checkup**" throughout the app for better clarity.
- **Timeline Integration**: Replaced the Body Scan header shortcut with a dedicated Checkup button.
- **Integrated Reminders**: Users can now enable and schedule their daily Checkup reminder directly within the Checkup logging screen.
- **Welcome Wizard Update**: The onboarding flow now proposes a daily Checkup reminder instead of a Body Scan reminder.
- **Cleanup**: Removed redundant dedicated Body Scan reminders to favor the unified Checkup approach.

## Phase 34: User Profile and Health Foundation (Completed)
- **Health Profile Collection**: Added a new step to the Welcome Wizard to collect basic user data: **Age, Sex, Weight, Height, and Chronic Conditions**.
- **Settings Integration**: Added a collapsible "**My Profile**" section in the Settings screen to view and edit this information anytime.
- **Data Persistence**: Integrated profile fields into the `SettingsRepository` using encrypted DataStore preferences.
- **Chronic Conditions Management**: Interactive chip-based UI for adding and removing multiple chronic diseases.

## Phase 35: Enhanced Dietary Tracking (Completed)
- **Beverage Enhancements**: Added checkboxes for Alcoholic and Caffeinated traits in beverage entries.
- **Allergen Tracking**: 
    - Added comprehensive allergen checkboxes (Gluten, Lactose, etc.) in meal entries.
    - Implemented a reorderable allergen list with persistence in `SettingsRepository`.
    - Meal entries now display selected allergens in the timeline with high-visibility (Error color).
- **Localization**: Full translation of allergens into French.
- **Customization**: Added a management section in Settings to deactivate irrelevant allergens. Deactivated allergens are hidden from the entry creation checklist.
- **Data Model Migration**: Updated `HealthEntry` to include `isAlcoholic`, `isCaffeinated`, and `allergens`. Incremented DB version to 7. Removed `fallbackToDestructiveMigration` to protect user data.

## Phase 36: Pain Origin Tracking (Completed)
- **Data Model Update**: Added `origin` field to `HealthEntry`.
- **Database Migration**: Incremented DB version to 8 and added migration to add `origin` column.
- **Contextual Autocomplete**:
    - Implemented `getPainOriginSuggestions(location)` in `TimelineViewModel`.
    - Suggestions for the "Origin" field are now filtered based on the selected "Location" (body part).
- **UI Integration**:
    - Added "Pain Origin" autocomplete field to `AddPainScreen`.
    - Added "Pain Origin" autocomplete field for each pain entry in the `Checkup` (formerly Complete Check-in) screen.
- **Search Support**: Added the `origin` field to the global timeline search.
