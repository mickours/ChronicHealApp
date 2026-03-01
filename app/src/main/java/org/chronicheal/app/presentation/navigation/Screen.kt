package org.chronicheal.app.presentation.navigation

sealed class Screen(val route: String) {
    object Timeline : Screen("timeline")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object DayView : Screen("day_view/{date}") {
        fun createRoute(date: String) = "day_view/$date"
    }
    object EntryTypeSelection : Screen("entry_type_selection?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "entry_type_selection?date=$date" else "entry_type_selection"
    }
    object AddPain : Screen("add_pain?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_pain?date=$date" else "add_pain"
    }
    object AddDrug : Screen("add_drug?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_drug?date=$date" else "add_drug"
    }
    object AddSymptom : Screen("add_symptom?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_symptom?date=$date" else "add_symptom"
    }
    object AddActivity : Screen("add_activity?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_activity?date=$date" else "add_activity"
    }
    object AddMeal : Screen("add_meal?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_meal?date=$date" else "add_meal"
    }
    object AddDisease : Screen("add_disease?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_disease?date=$date" else "add_disease"
    }
    object AddSleep : Screen("add_sleep?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_sleep?date=$date" else "add_sleep"
    }
    object AddMedicalAppointment : Screen("add_medical_appointment?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_medical_appointment?date=$date" else "add_medical_appointment"
    }
    object AddExternalFactor : Screen("add_external_factor?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_external_factor?date=$date" else "add_external_factor"
    }
    object AddJournal : Screen("add_journal?date={date}") {
        fun createRoute(date: String? = null) = if (date != null) "add_journal?date=$date" else "add_journal"
    }
}
