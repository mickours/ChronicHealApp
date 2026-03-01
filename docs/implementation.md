# Implementation Plan

This document outlines the implementation steps for ChronicHeal, based on the `README.md` and `docs/design.md`.

## Phase 1: Core Data Foundation and Basic UI (Completed)
Established the core data structure and a basic user interface.

## Phase 2: Timeline and Entry Management (Completed)
Goal: Allow users to view their history and add new entries.

### Accomplishments:
- **Unified Entry System**: Implemented all 10 specialized screens (Pain, Drugs, Symptoms, Disease, Meals, Sleep, Medical Appointment, Activity, External Factors, Journal).
- **Entry Edition**: Added support for editing existing entries. Users can tap any entry in the Timeline or Day View to modify its details.
- **Intensity Tracking**: All "Occurrence" categories (Pain, Symptom, Disease, External Factors) now include an intensity/impact level (1-10).
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

### 2. Reminders (Completed)
- **Exact Scheduling**: Used `AlarmManager` for high-precision health reminders.
- **Integrated Creation**: Users can now set reminders directly from Medication, Meal, Sleep, Appointment, Activity, and Journal entry screens.
- **Visual Feedback**: Entries with associated reminders now display a bell icon (`NotificationsActive`) in the Timeline.
- **Persistence**: Reminders survive device reboots via `BootReceiver`.
- **Centralized Hub**: Reminders are now listed and manageable directly from the Calendar view.

## Phase 5: Polish and Privacy (Current)
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
- Biometric Lock.
- Search and Filters.
