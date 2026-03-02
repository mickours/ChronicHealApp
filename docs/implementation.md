# Implementation Plan

This document outlines the implementation steps for ChronicHeal, based on the `README.md` and `docs/design.md`.

## Phase 1: Core Data Foundation and Basic UI (Completed)
Established the core data structure and a basic user interface.

## Phase 2: Timeline and Entry Management (Completed)
Goal: Allow users to view their history and add new entries.

### Accomplishments:
- **Unified Entry System**: Implemented all 11 specialized screens (Pain, Drugs, Symptoms, Disease, Meals, Sleep, Medical Appointment, Activity, External Factors, Journal, Period).
- **Entry Edition**: Added support for editing existing entries. Users can tap any entry in the Timeline or Day View to modify its details.
- **Intensity Tracking**: All "Occurrence" categories (Pain, Intensity, Symptom, Disease, External Factors, Period) now include an intensity/impact level (1-10 or 1-5 for Periods).
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
    - **Asset Management**: Moved `body-shape.svg` to the `assets` folder for raw parsing.
    - **Custom Parser**: Developed `SvgBodyParser` using `XmlPullParser` and `PathParser` to convert SVG elements into `android.graphics.Path` objects while preserving element IDs.
    - **Precise Hit Detection**: Implemented path-based hit detection using `android.graphics.Region`, allowing users to interact with complex anatomical shapes accurately.
    - **Auto-scaling**: Silhouette automatically scales to fit the screen while maintaining aspect ratio.
- **Visual Polish**: Added a vertical gradient fill and refined the stroke for a modern look. Fixed nested transformation bugs in the parser to ensure anatomical accuracy.
- **In-place Creation**: Added a "Save" button to the Body Scan bottom sheet to allow direct creation of pain logs without leaving the interactive silhouette.
    - **Smart State Management**: The Body Scan now detects if a pain log already exists for a specific region on the current day, allowing users to update it instead of creating duplicates.
    - **Feedback Integration**: Added a `SnackbarHost` to provide immediate confirmation when a log is saved or updated.
- **Interactive Intensity Selection**: Implemented "hold-to-increase" intensity logic. Users can press and hold a body region to see the intensity grow dynamically before logging.
    - **Dynamic Visual Feedback**: Added a `VerticalIntensityGauge` overlay that appears during the hold gesture to show the current intensity level in real-time.
    - **Integrated Deletion**: Added a delete button in the Body Scan drawer when editing an existing entry for quicker data management.
- **Body Scan Reminders**: 
    - Added a dedicated "Body Scan Reminders" screen to manage recurring prompts specifically for logging body pain.
    - Integrated a "Reminders" option in the Body Scan Screen top bar menu for easy access.
    - Enhanced `AddReminderScreen` to support both creation and **edition** of existing reminders.
    - Automated category pre-filling (e.g., pre-selecting "Pain" when coming from Body Scan).
    - **Quick Actions**: Notifications now feature "Start Body Scan" and "Skip Today" actions for seamless user flow.

### 2. Reminders (Completed)
- **Exact Scheduling**: Used `AlarmManager` for high-precision health reminders.
- **Integrated Creation**: Users can now set reminders directly from Medication, Meal, Sleep, Appointment, Activity, and Journal entry screens.
- **Visual Feedback**: Entries with associated reminders now display a bell icon (`NotificationsActive`) in the Timeline.
- **Persistence**: Reminders survive device reboots via `BootReceiver`.
- **Centralized Hub**: Reminders are now listed and manageable directly from the Calendar view.
- **Permissions**: Integrated mandatory runtime permission requests for notifications on Android 13+.
- **Notification Quick Actions**:
    - Added "10 min" and "1 hour" snooze actions to health reminders.
    - Implemented a `snooze` method in `ReminderScheduler` using `AlarmManager` for one-time delayed triggering.
    - Updated `ReminderReceiver` to handle snooze actions by canceling the current notification and scheduling a new one-off alarm.

## Phase 5: Polish and Privacy (Completed)
- **Enhanced Entry Model**: Added `durationMinutes` and `isFinished` properties to `HealthEntry`.
- **Default Durations**: Each `EntryType` now has a default duration (e.g., Sleep: 8h, Meal: 30min, Activity: 30min).
- **Swipe-to-Action**: Implemented `SwipeToDismissBox` in Timeline and Day View.
    - **Swipe Left**: Delete entry with undo snackbar.
    - **Swipe Right**: Mark entry as "finished" (visual change to grey and checkmark icon).
- **UI/UX Refinements**:
    - **Factorized Add Screens**: Introduced `AddEntryScaffold` to share common logic (cancel/delete/update).
    - **Custom Theme**: Implemented a Soft Blue (`#a7c7d4`) and complementary Peach/Orange palette.
    - **Visual Indicators**: Added emojis to all entry types and improved card shapes to hint at swipability.
    - **Flexible Entry Logging**: Users can now edit both the **start time** and **duration** for every entry type.
    - **Vertical Intensity Gauge**: Refactored the entry cards to move the intensity/quality gauge from a horizontal bar to a compact vertical gauge on the right side of the card. This optimizes space and provides a clearer visual hierarchy.
    - **Intelligent Autocomplete**: Integrated `ExposedDropdownMenuBox` in entry creation screens. Suggestions are dynamically generated from existing logs, reducing data entry friction and ensuring consistent naming for better grouping in analytics.
    - **Advanced Analytics Visualization**: 
        - **Stacked Pain Timeline**: Replaced simple charts with a stacked line chart showing pain intensity by location. This allows users to see cumulative pain impact while distinguishing between different body parts.
        - **Severity-based Symptom Analysis**: Symptom charts now reflect the **sum of severity** rather than just frequency, giving a more accurate picture of symptom burden.
        - **Readability Optimizations**: Implemented rotated X-axis labels and fixed Y-axis increments to handle dense data views across different time ranges (Week, Month, Year).
    - **High-Visibility Calendar Markers**: Increased the size and contrast of health indicators in the calendar view. Dots now feature stronger colors and a subtle border to ensure they remain distinct and legible against all background shades.
- **Biometric Lock Implementation**:
    - **Technical Choice**: Switched `MainActivity` from `ComponentActivity` to `AppCompatActivity` to support the `BiometricPrompt` API.
    - **Enforcement**: Implemented mandatory biometric authentication on app startup when enabled in settings.
    - **Session Persistence**: Used a local `isAuthenticated` state in `MainActivity` to avoid re-triggering the prompt during configuration changes or internal navigation while keeping the app secure on fresh starts.
    - **Availability Safeguards**: Added `BiometricManager` checks in `SecurityViewModel` to disable and reset the lock setting if biometric hardware is missing or credentials are not enrolled. The UI now reflects this state by disabling the toggle and providing contextual feedback.
- **Search and Filters (Completed)**: Added a dynamic search bar and type-based filtering chips to the main timeline for efficient history navigation.

## Phase 6: Onboarding and Personalization (Completed)
Goal: Improve the first-run experience and daily logging efficiency.

### Accomplishments:
- **Startup Welcome Wizard**:
    - Implemented a multi-step onboarding flow using `HorizontalPager`.
    - **Page 1: Introduction**: Explains the app's privacy-first mission and core features.
    - **Page 2: Notifications**: Requests `POST_NOTIFICATIONS` permission (for Android 13+) to ensure reminders work out of the box.
    - **Page 3: Personalization**: Allows users to select their "Favorite Entry Types" from the start.
- **Quick Add Shortcuts**:
    - Integrated a "QUICK ADD" bar at the bottom of the `TimelineScreen`.
    - Dynamically displays the user's favorite entry types as circular chips for one-tap access to specialized logging screens.
- **Persistence**:
    - Introduced `SettingsRepository` using `DataStore` to store onboarding status and favorite preferences.
    - Updated `MainActivity` to conditionally route users to the `WelcomeWizard` or `Timeline` based on their first-run status.

## Phase 7: Specialized Entry Types (Completed)
- **Period Tracking**: Added a specialized entry type for women's periods (`EntryType.PERIOD`).
    - **UI**: Implemented `AddPeriodScreen` with a dedicated flow intensity slider (1-5).
    - **Integration**: Added to navigation graph and entry selection screen.

## Phase 8: Advanced Analytics (In Progress)
- **Correlation Insights**: Visualize correlations between different health metrics (e.g., Pain Intensity vs. Sleep Quality).
- **Custom Dashboard**: Allow users to pin specific charts or summaries to the home screen.
