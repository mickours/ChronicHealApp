package org.chronicheal.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.presentation.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Timeline.route
    ) {
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
                }
            )
        }
        composable(route = Screen.Calendar.route) {
            CalendarScreen(
                onBackClick = { navController.popBackStack() },
                onDateClick = { date ->
                    navController.navigate(Screen.DayView.createRoute(date.toString()))
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
                }
            )
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EntryTypeSelection.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            EntryTypeSelectionScreen(
                onTypeSelected = { type ->
                    val route = when (type) {
                        EntryType.PAIN -> Screen.AddPain.createRoute(date)
                        EntryType.DRUG -> Screen.AddDrug.createRoute(date)
                        EntryType.SYMPTOM -> Screen.AddSymptom.createRoute(date)
                        EntryType.DISEASE -> Screen.AddDisease.createRoute(date)
                        EntryType.MEAL -> Screen.AddMeal.createRoute(date)
                        EntryType.SLEEP -> Screen.AddSleep.createRoute(date)
                        EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute(date)
                        EntryType.ACTIVITY -> Screen.AddActivity.createRoute(date)
                        EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute(date)
                        EntryType.JOURNAL -> Screen.AddJournal.createRoute(date)
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
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddPainScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddDrug.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddDrugScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddSymptom.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddSymptomScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddActivity.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddActivityScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddMeal.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddMealScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddSleep.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddSleepScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddDisease.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddDiseaseScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddMedicalAppointment.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddMedicalAppointmentScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddExternalFactor.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddExternalFactorScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
        composable(
            route = Screen.AddJournal.route,
            arguments = listOf(navArgument("date") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date")
            AddJournalScreen(
                dateString = date,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { onSaveSuccess(date) }
            )
        }
    }
}
