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
