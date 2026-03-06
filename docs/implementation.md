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
