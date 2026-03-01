package org.chronicheal.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Timeline.route
    ) {
        val onEntryClick: (HealthEntry) -> Unit = { entry ->
            val route = when (entry.type) {
                EntryType.PAIN -> Screen.AddPain.createRoute(id = entry.id)
                EntryType.DRUG -> Screen.AddDrug.createRoute(id = entry.id)
                EntryType.SYMPTOM -> Screen.AddSymptom.createRoute(id = entry.id)
                EntryType.DISEASE -> Screen.AddDisease.createRoute(id = entry.id)
                EntryType.MEAL -> Screen.AddMeal.createRoute(id = entry.id)
                EntryType.SLEEP -> Screen.AddSleep.createRoute(id = entry.id)
                EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute(id = entry.id)
                EntryType.ACTIVITY -> Screen.AddActivity.createRoute(id = entry.id)
                EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute(id = entry.id)
                EntryType.JOURNAL -> Screen.AddJournal.createRoute(id = entry.id)
            }
            navController.navigate(route)
        }

        composable(route = Screen.Timeline.route) {
            TimelineScreen(
                onAddEntryClick = {
                    navController.navigate(Screen.EntryTypeSelection.createRoute())
                },
                onCalendarClick = {
                    navController.navigate(Screen.Calendar.route)
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAnalyticsClick = {
                    navController.navigate(Screen.Analytics.route)
                },
                onBodyScanClick = {
                    navController.navigate(Screen.BodyScan.route)
                },
                onEntryClick = onEntryClick
            )
        }
        composable(route = Screen.Calendar.route) {
            CalendarScreen(
                onBackClick = { navController.popBackStack() },
                onDateClick = { date ->
                    navController.navigate(Screen.DayView.createRoute(date.toString()))
                },
                onManageRemindersClick = {
                    navController.navigate(Screen.Reminders.route)
                }
            )
        }
        composable(route = Screen.BodyScan.route) {
            BodyScanScreen(
                onBackClick = { navController.popBackStack() },
                onRegionClick = { region ->
                    navController.navigate(Screen.EntryTypeSelection.createRoute(location = region))
                }
            )
        }
        composable(
            route = Screen.DayView.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            DayViewScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onAddEntryClick = { clickedDate ->
                    navController.navigate(Screen.EntryTypeSelection.createRoute(clickedDate.toString()))
                },
                onEntryClick = onEntryClick
            )
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Reminders.route) {
            RemindersScreen(
                onBackClick = { navController.popBackStack() },
                onAddReminderClick = { navController.navigate(Screen.AddReminder.route) }
            )
        }
        composable(route = Screen.AddReminder.route) {
            AddReminderScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Analytics.route) {
            AnalyticsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EntryTypeSelection.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val location = backStackEntry.arguments?.getString("location")
            EntryTypeSelectionScreen(
                onTypeSelected = { type ->
                    val route = when (type) {
                        EntryType.PAIN -> Screen.AddPain.createRoute(date, location)
                        EntryType.DRUG -> Screen.AddDrug.createRoute(date, location)
                        EntryType.SYMPTOM -> Screen.AddSymptom.createRoute(date, location)
                        EntryType.DISEASE -> Screen.AddDisease.createRoute(date, location)
                        EntryType.MEAL -> Screen.AddMeal.createRoute(date, location)
                        EntryType.SLEEP -> Screen.AddSleep.createRoute(date, location)
                        EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute(date, location)
                        EntryType.ACTIVITY -> Screen.AddActivity.createRoute(date, location)
                        EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute(date, location)
                        EntryType.JOURNAL -> Screen.AddJournal.createRoute(date, location)
                    }
                    navController.navigate(route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        fun onSaveSuccess(date: String?) {
            if (date != null) {
                navController.popBackStack(Screen.DayView.createRoute(date), inclusive = false)
            } else {
                navController.popBackStack(Screen.Timeline.route, inclusive = false)
            }
        }

        composable(
            route = Screen.AddPain.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val location = backStackEntry.arguments?.getString("location")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddPainScreen(
                dateString = date,
                locationString = location,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddDrug.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddDrugScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddSymptom.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val location = backStackEntry.arguments?.getString("location")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddSymptomScreen(
                dateString = date,
                locationString = location,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddActivity.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddActivityScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddMeal.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddMealScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddSleep.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddSleepScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddDisease.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddDiseaseScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddMedicalAppointment.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddMedicalAppointmentScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddExternalFactor.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddExternalFactorScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddJournal.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddJournalScreen(
                dateString = date,
                id = id,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
    }
}
