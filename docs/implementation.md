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
- **Visual Coding**: Categorized entries into "What occurs to you" (Red) and "What you can manage" (Green) using vertical color stripes on timeline cards.
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
- Biometric Lock.
- Search and Filters.
- UI/UX refinements.
