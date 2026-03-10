package org.chronicheal.app.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.model.HealthEntry
import org.chronicheal.app.presentation.AddActivityScreen
import org.chronicheal.app.presentation.AddBeverageScreen
import org.chronicheal.app.presentation.AddCompleteEntryScreen
import org.chronicheal.app.presentation.AddDiseaseScreen
import org.chronicheal.app.presentation.AddDrugScreen
import org.chronicheal.app.presentation.AddExternalFactorScreen
import org.chronicheal.app.presentation.AddJournalScreen
import org.chronicheal.app.presentation.AddMealScreen
import org.chronicheal.app.presentation.AddMedicalAppointmentScreen
import org.chronicheal.app.presentation.AddMoodScreen
import org.chronicheal.app.presentation.AddPainScreen
import org.chronicheal.app.presentation.AddPeriodScreen
import org.chronicheal.app.presentation.AddReminderScreen
import org.chronicheal.app.presentation.AddSleepScreen
import org.chronicheal.app.presentation.AddStoolScreen
import org.chronicheal.app.presentation.AddSymptomScreen
import org.chronicheal.app.presentation.AnalyticsScreen
import org.chronicheal.app.presentation.BodyScanRemindersScreen
import org.chronicheal.app.presentation.BodyScanScreen
import org.chronicheal.app.presentation.CalendarScreen
import org.chronicheal.app.presentation.DayViewScreen
import org.chronicheal.app.presentation.EntryTypeSelectionScreen
import org.chronicheal.app.presentation.RemindersScreen
import org.chronicheal.app.presentation.SettingsScreen
import org.chronicheal.app.presentation.TimelineScreen
import org.chronicheal.app.presentation.TimelineViewModel
import org.chronicheal.app.presentation.VoiceLoggingScreen
import org.chronicheal.app.presentation.WelcomeWizardScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Timeline.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            )
        }
    ) {
        val onEntryClick: (HealthEntry, String?) -> Unit = { entry, date ->
            val route = when (entry.type) {
                EntryType.PAIN -> Screen.AddPain.createRoute(id = entry.id, date = date)
                EntryType.DRUG -> Screen.AddDrug.createRoute(id = entry.id, date = date)
                EntryType.SYMPTOM -> Screen.AddSymptom.createRoute(id = entry.id, date = date)
                EntryType.DISEASE -> Screen.AddDisease.createRoute(id = entry.id, date = date)
                EntryType.MEAL -> Screen.AddMeal.createRoute(id = entry.id, date = date)
                EntryType.SLEEP -> Screen.AddSleep.createRoute(id = entry.id, date = date)
                EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute(id = entry.id, date = date)
                EntryType.ACTIVITY -> Screen.AddActivity.createRoute(id = entry.id, date = date)
                EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute(id = entry.id, date = date)
                EntryType.JOURNAL -> Screen.AddJournal.createRoute(id = entry.id, date = date)
                EntryType.PERIOD -> Screen.AddPeriod.createRoute(id = entry.id, date = date)
                EntryType.BEVERAGE -> Screen.AddBeverage.createRoute(id = entry.id, date = date)
                EntryType.STOOL -> Screen.AddStool.createRoute(id = entry.id, date = date)
                EntryType.MOOD -> Screen.AddMood.createRoute(id = entry.id, date = date)
                EntryType.VOICE_LOGGING -> Screen.VoiceLogging.route
            }
            navController.navigate(route)
        }

        val onEntryTypeClick: (EntryType) -> Unit = { type ->
            val route = when (type) {
                EntryType.PAIN -> Screen.BodyScan.createRoute()
                EntryType.DRUG -> Screen.AddDrug.createRoute()
                EntryType.SYMPTOM -> Screen.AddSymptom.createRoute()
                EntryType.DISEASE -> Screen.AddDisease.createRoute()
                EntryType.MEAL -> Screen.AddMeal.createRoute()
                EntryType.SLEEP -> Screen.AddSleep.createRoute()
                EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute()
                EntryType.ACTIVITY -> Screen.AddActivity.createRoute()
                EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute()
                EntryType.JOURNAL -> Screen.AddJournal.createRoute()
                EntryType.PERIOD -> Screen.AddPeriod.createRoute()
                EntryType.BEVERAGE -> Screen.AddBeverage.createRoute()
                EntryType.STOOL -> Screen.AddStool.createRoute()
                EntryType.MOOD -> Screen.AddMood.createRoute()
                EntryType.VOICE_LOGGING -> Screen.VoiceLogging.route
            }
            navController.navigate(route)
        }

        composable(route = Screen.WelcomeWizard.route) {
            WelcomeWizardScreen(
                onWizardCompleted = {
                    navController.navigate(Screen.Timeline.route) {
                        popUpTo(Screen.WelcomeWizard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Timeline.route) { backStackEntry ->
            val viewModel: TimelineViewModel = hiltViewModel(backStackEntry)
            TimelineScreen(
                navController = navController,
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
                    navController.navigate(Screen.BodyScan.createRoute())
                },
                onVoiceLoggingClick = {
                    navController.navigate(Screen.VoiceLogging.route)
                },
                onEntryTypeClick = onEntryTypeClick,
                onEntryClick = { entry -> onEntryClick(entry, null) },
                viewModel = viewModel
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
        composable(
            route = Screen.BodyScan.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            BodyScanScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onRemindersClick = { navController.navigate(Screen.BodyScanReminders.route) }
            )
        }
        composable(route = Screen.BodyScanReminders.route) {
            BodyScanRemindersScreen(
                onBackClick = { navController.popBackStack() },
                onAddReminderClick = { 
                    navController.navigate(Screen.AddReminder.createRoute(type = EntryType.PAIN.name)) 
                },
                onReminderClick = { reminderId ->
                    navController.navigate(Screen.AddReminder.createRoute(id = reminderId))
                }
            )
        }
        composable(
            route = Screen.DayView.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val viewModel: TimelineViewModel = hiltViewModel(backStackEntry)
            DayViewScreen(
                navController = navController,
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onAddEntryClick = { clickedDate ->
                    navController.navigate(Screen.EntryTypeSelection.createRoute(clickedDate.toString()))
                },
                onEntryClick = { entry -> onEntryClick(entry, date) },
                viewModel = viewModel
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
                onAddReminderClick = { navController.navigate(Screen.AddReminder.createRoute()) },
                onReminderClick = { reminderId ->
                    navController.navigate(Screen.AddReminder.createRoute(id = reminderId))
                }
            )
        }
        composable(
            route = Screen.AddReminder.route,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val typeString = backStackEntry.arguments?.getString("type")
            val initialType = typeString?.let { try { EntryType.valueOf(it) } catch(e: Exception) { null } }
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddReminderScreen(
                id = id,
                initialType = initialType,
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
                        EntryType.PAIN -> Screen.BodyScan.createRoute(date)
                        EntryType.DRUG -> Screen.AddDrug.createRoute(date, location)
                        EntryType.SYMPTOM -> Screen.AddSymptom.createRoute(date, location)
                        EntryType.DISEASE -> Screen.AddDisease.createRoute(date, location)
                        EntryType.MEAL -> Screen.AddMeal.createRoute(date, location)
                        EntryType.SLEEP -> Screen.AddSleep.createRoute(date, location)
                        EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute(date, location)
                        EntryType.ACTIVITY -> Screen.AddActivity.createRoute(date, location)
                        EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute(date, location)
                        EntryType.JOURNAL -> Screen.AddJournal.createRoute(date, location)
                        EntryType.PERIOD -> Screen.AddPeriod.createRoute(date, location)
                        EntryType.BEVERAGE -> Screen.AddBeverage.createRoute(date, location)
                        EntryType.STOOL -> Screen.AddStool.createRoute(date, location)
                        EntryType.MOOD -> Screen.AddMood.createRoute(date, location)
                        EntryType.VOICE_LOGGING -> Screen.VoiceLogging.route
                    }
                    navController.navigate(route)
                },
                onCompleteEntryClick = {
                    navController.navigate(Screen.AddCompleteEntry.createRoute(date))
                },
                onVoiceLoggingClick = {
                    navController.navigate(Screen.VoiceLogging.route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        fun onSaveSuccess(date: String?, isUpdate: Boolean) {
            val message = if (isUpdate) "Entry updated" else "Entry saved"
            val targetRoute = if (date != null) Screen.DayView.createRoute(date) else Screen.Timeline.route
            navController.getBackStackEntry(targetRoute).savedStateHandle.set("message", message)
            navController.popBackStack(targetRoute, inclusive = false)
        }

        val onCancel: (Long?) -> Unit = { id ->
            if (id != null) {
                navController.previousBackStackEntry?.savedStateHandle?.set("message", "Edition canceled")
            }
            navController.popBackStack()
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
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
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
            )
        }
        composable(
            route = Screen.AddPeriod.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddPeriodScreen(
                dateString = date,
                id = id,
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
            )
        }
        composable(
            route = Screen.AddBeverage.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddBeverageScreen(
                dateString = date,
                id = id,
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
            )
        }
        composable(
            route = Screen.AddStool.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddStoolScreen(
                dateString = date,
                id = id,
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
            )
        }
        composable(
            route = Screen.AddMood.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("location") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("id") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            val id = backStackEntry.arguments?.getLong("id").takeIf { it != -1L }
            AddMoodScreen(
                dateString = date,
                id = id,
                onBackClick = { onCancel(id) },
                onSaveSuccess = { onSaveSuccess(date, id != null) }
            )
        }
        composable(
            route = Screen.AddCompleteEntry.route,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddCompleteEntryScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date, false) }
            )
        }
        composable(route = Screen.VoiceLogging.route) {
            VoiceLoggingScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { 
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                    navController.getBackStackEntry(Screen.Timeline.route).savedStateHandle.set("message", "Entry saved via voice")
                }
            )
        }
    }
}
