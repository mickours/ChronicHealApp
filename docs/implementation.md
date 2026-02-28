# Implementation Plan - Phase 1: Core Data Foundation and Basic UI

This document outlines the first steps for implementing ChronicHeal, based on the `README.md` and `docs/design.md`.

## Goal
Establish the core data structure and a basic user interface to display a timeline of health-related entries.

## 1. Dependency Setup
We have already updated `build.gradle.kts` and `libs.versions.toml` to include:
- **Jetpack Compose**: For the modern UI layer.
- **Hilt**: For dependency injection.
- **Room**: For local data persistence.
- **Kotlin Symbol Processing (KSP)**: For efficient annotation processing.

## 2. Domain Layer: Core Entities
We will define a unified `BaseEntry` and specialized entry types as suggested in the design doc.

### Initial Entities:
- `HealthEntry`: The base entity representing any tracked event.
- `EntryType`: An enum defining types (PAIN, DRUG, SYMPTOM, etc.).

## 3. Data Layer: Room Database
- `EntryDao`: To handle CRUD operations for health entries.
- `AppDatabase`: The main database class.
- `EntryRepository`: An interface and implementation to abstract data access.

## 4. Presentation Layer: Basic Timeline
- `MainViewModel`: To manage UI state and interact with the repository.
- `TimelineScreen`: A Jetpack Compose screen to display a list of entries.

## Next Steps
1. Create the `HealthEntry` entity and `EntryType` enum.
2. Set up the Hilt `AppModule` for dependency injection.
3. Implement the Room database and DAO.
4. Replace the default `MainActivity` (View-based) with a Compose-based version.
