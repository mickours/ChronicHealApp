# Implementation Plan

This document outlines the implementation steps for ChronicHeal, based on the `README.md` and `docs/design.md`.

## Phase 45: Intensity Heatmap Analytics (Completed)

Goal: Add a visual calendar heatmap to the Analytics screen to track intensity trends.

**Accomplishments:**

- **UI Abstraction**: Unified the `MetricDropdown` to be reusable for multiple selection contexts (
  Correlation vs. Heatmap).
- **Heatmap Data Aggregation**: Added `heatmapData` to `AnalyticsUiState` and implemented
  `getDailyValues` in `AnalyticsViewModel` to compute the sum of intensities or occurrences per day
  for a selected metric.
- **Dynamic Selection**: Users can now select any series available in the correlation selector to
  display its intensity heatmap.
- **Adaptive Calendar View**:
    - **Weekly/Monthly**: Displays a grid-based calendar where each day's color intensity reflects
      its total logged values.
    - **Yearly/All-time**: Switches to a compact GitHub-style contribution grid for long-term trend
      visualization.
- **Auto-initialization**: The heatmap automatically selects "Pain" as the default metric on first
  load.
- **Technical Polish**: Updated `AnalyticsViewModel` with a more robust `combine` flow to handle 5+
  state streams and avoid "Cannot infer type" compiler errors.
