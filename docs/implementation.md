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
- **Integrated Search & Filters**: Replicated the search and type-filtering bar from the Timeline onto the Calendar screen.
- **Dynamic Calendar Heatmap**: The calendar grid dots now update in real-time based on the active search query and selected filters, allowing users to see exactly when specific types of events occurred (e.g., "Show me all high-pain days where I took Ibuprofen").

## Phase 18: Enhanced Analytics and Reporting (Completed)
- **Clickable Legends**: Legend items in Pain and Symptoms evolution charts are now clickable, allowing users to toggle line visibility dynamically.
- **Stacked Timeline Charts**: Replaced pie charts with cumulative stacked timeline charts for both Pain and Symptoms to better visualize overall burden.
- **Detailed PDF Reports**:
    - Added high-fidelity stacked charts to PDF exports, matching the app's UI.
    - Integrated grid lines, axis ticks, and explanatory text for better chart readability.
    - Added a "Period Summary Statistics" table with min/max/avg values.
    - **Detailed Logs**: Pain entries in logs now include the recorded location (e.g., `[Lower Back]`).
- **Export UX**: Added an "Open" quick action to the snackbar after a successful PDF export.
- **Test Automation**: Added unit tests for PDF generation and documented development procedures in the README.

## Phase 19: PDF Report Optimization (Completed)
- **Improved Layout & Density**: Implemented a `PageState` manager to handle efficient vertical flow and pagination.
- **Date-Grouped Logs**: Grouped health entries by day with headers, significantly reducing the vertical space required for long reports.
- **Information Richness**: Included emojis, locations, and summarized notes while maintaining a compact format.
- **Visual Polish**: Adjusted font sizes and chart dimensions for a more professional, medical-grade report appearance.
- **Integrity Fixes**: Ensured the `outputStream` remains open during asynchronous PDF generation, preventing corrupted or empty files.

## Phase 20: Notification UX and Branding (Completed)
- **"Log Now" Action Fixed**: Resolved the issue where the "Log Now" notification action was inactive. It now correctly launches the app and navigates to the pre-filled entry screen for the specific reminder.
- **Updated Visual Identity**:
    - **App Icon**: Manually converted the `logo.svg` paths and nested transformations into a high-fidelity `ic_launcher_foreground.xml` Vector Drawable.
    - **Notification Icon**: Created a monochromatic version of the logo paths (`ic_notification_logo.xml`) optimized for system status bars and notification drawers.
    - **Branding Consistency**: Integrated the new logo into the `NotificationHelper` for a unified look and feel.
- **Automatic Notification Dismissal**: Handled the dismissal of the reminder notification as soon as the user selects "Log Now".
- **Vector Accuracy**: Refined the launcher icon XML to precisely match the source SVG geometry and nested transformations.

## Phase 21: Weekly Insights (Completed)
- **Actionable Summary**: Added a "Last 7 Days Insights" card at the top of the `TimelineScreen` to provide immediate feedback on health trends.
- **Key Metrics**:
    - **Average Pain**: Calculated mean intensity for pain entries over the last week.
    - **Medication Count**: Total number of drug entries in the last 7 days.
    - **Average Sleep**: Mean sleep duration (prioritizing `durationMinutes`, falling back to `intensity * 60`).
- **Trend Analysis**:
    - Compared current week metrics against the previous week (7-14 days ago).
    - Added visual indicators (TrendingUp, TrendingDown, TrendingFlat) with semantic coloring (Green for improvement, Red for decline).
- **UI Integration**: Prepend the insights card to the grouped timeline list, ensuring it's the first thing users see upon opening the app.

## Phase 22: PDF Graph and Analytics Enhancements (Completed)
- **X-Axis Ticks**: Added date labels and ticks to the PDF evolution charts for better temporal context.
- **Distribution Histograms**: Replaced the summary statistics table with a set of histograms showing the distribution of intensity levels (1-10) for each pain location and symptom.
- **Visual Clarity**: Improved chart spacing and layout density in the PDF report.

## Phase 23: Data Integrity Verification (Completed)
- **Test Implementation**: Created `DataTransferTest.kt` to verify the JSON export/import process.
- **Verification Logic**: Simulated a full data lifecycle by exporting complex `HealthEntry` objects to JSON and then importing them back, ensuring 1:1 field parity for timestamps, types, values, units, and notes.
- **Infrastructure**: Added `mockk` and `kotlinx-coroutines-test` dependencies to the project to support robust unit testing of use cases.
