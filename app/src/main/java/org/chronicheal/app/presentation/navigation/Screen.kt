package org.chronicheal.app.presentation.navigation

sealed class Screen(val route: String) {
    object Timeline : Screen("timeline")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object DayView : Screen("day_view/{date}") {
        fun createRoute(date: String) = "day_view/$date"
    }
    object EntryTypeSelection : Screen("entry_type_selection")
    object AddPain : Screen("add_pain")
    object AddDrug : Screen("add_drug")
    object AddSymptom : Screen("add_symptom")
    object AddActivity : Screen("add_activity")
    object AddMeal : Screen("add_meal")
    object AddDisease : Screen("add_disease")
    object AddSleep : Screen("add_sleep")
    object AddMedicalAppointment : Screen("add_medical_appointment")
    object AddExternalFactor : Screen("add_external_factor")
    object AddJournal : Screen("add_journal")
}
