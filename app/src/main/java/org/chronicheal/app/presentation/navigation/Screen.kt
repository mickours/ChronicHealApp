package org.chronicheal.app.presentation.navigation

sealed class Screen(val route: String) {
    object Timeline : Screen("timeline")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object BodyScan : Screen("body_scan")
    object DayView : Screen("day_view/{date}") {
        fun createRoute(date: String) = "day_view/$date"
    }
    object EntryTypeSelection : Screen("entry_type_selection?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null): String {
            val datePart = if (date != null) "date=$date" else null
            val locPart = if (location != null) "location=$location" else null
            val query = listOfNotNull(datePart, locPart).joinToString("&")
            return if (query.isNotEmpty()) "entry_type_selection?$query" else "entry_type_selection"
        }
    }
    object AddPain : Screen("add_pain?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_pain", date, location)
    }
    object AddDrug : Screen("add_drug?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_drug", date, location)
    }
    object AddSymptom : Screen("add_symptom?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_symptom", date, location)
    }
    object AddActivity : Screen("add_activity?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_activity", date, location)
    }
    object AddMeal : Screen("add_meal?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_meal", date, location)
    }
    object AddDisease : Screen("add_disease?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_disease", date, location)
    }
    object AddSleep : Screen("add_sleep?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_sleep", date, location)
    }
    object AddMedicalAppointment : Screen("add_medical_appointment?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_medical_appointment", date, location)
    }
    object AddExternalFactor : Screen("add_external_factor?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_external_factor", date, location)
    }
    object AddJournal : Screen("add_journal?date={date}&location={location}") {
        fun createRoute(date: String? = null, location: String? = null) = createQueryRoute("add_journal", date, location)
    }

    protected fun createQueryRoute(base: String, date: String?, location: String?): String {
        val datePart = if (date != null) "date=$date" else null
        val locPart = if (location != null) "location=$location" else null
        val query = listOfNotNull(datePart, locPart).joinToString("&")
        return if (query.isNotEmpty()) "$base?$query" else base
    }
}
