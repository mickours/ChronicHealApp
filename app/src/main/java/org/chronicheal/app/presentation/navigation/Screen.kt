package org.chronicheal.app.presentation.navigation

sealed class Screen(val route: String) {
    object WelcomeWizard : Screen("welcome_wizard")
    object Timeline : Screen("timeline")
    object Calendar : Screen("calendar")
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object BodyScan : Screen("body_scan")
    object BodyScanReminders : Screen("body_scan_reminders")
    object Reminders : Screen("reminders")
    object AddReminder : Screen("add_reminder?type={type}&id={id}") {
        fun createRoute(type: String? = null, id: Long? = null): String {
            val typePart = if (type != null) "type=$type" else null
            val idPart = if (id != null) "id=$id" else null
            val query = listOfNotNull(typePart, idPart).joinToString("&")
            return if (query.isNotEmpty()) "add_reminder?$query" else "add_reminder"
        }
    }
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
    object AddPain : Screen("add_pain?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_pain", date, location, id)
    }
    object AddDrug : Screen("add_drug?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_drug", date, location, id)
    }
    object AddSymptom : Screen("add_symptom?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_symptom", date, location, id)
    }
    object AddActivity : Screen("add_activity?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_activity", date, location, id)
    }
    object AddMeal : Screen("add_meal?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_meal", date, location, id)
    }
    object AddDisease : Screen("add_disease?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_disease", date, location, id)
    }
    object AddSleep : Screen("add_sleep?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_sleep", date, location, id)
    }
    object AddMedicalAppointment : Screen("add_medical_appointment?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_medical_appointment", date, location, id)
    }
    object AddExternalFactor : Screen("add_external_factor?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_external_factor", date, location, id)
    }
    object AddJournal : Screen("add_journal?date={date}&location={location}&id={id}") {
        fun createRoute(date: String? = null, location: String? = null, id: Long? = null) = createQueryRoute("add_journal", date, location, id)
    }

    protected fun createQueryRoute(base: String, date: String?, location: String?, id: Long? = null): String {
        val datePart = if (date != null) "date=$date" else null
        val locPart = if (location != null) "location=$location" else null
        val idPart = if (id != null) "id=$id" else null
        val query = listOfNotNull(datePart, locPart, idPart).joinToString("&")
        return if (query.isNotEmpty()) "$base?$query" else base
    }
}
