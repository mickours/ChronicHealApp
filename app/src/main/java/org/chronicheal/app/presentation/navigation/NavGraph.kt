package org.chronicheal.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
                    navController.navigate(Screen.EntryTypeSelection.route)
                },
                onCalendarClick = {
                    navController.navigate(Screen.Calendar.route)
                }
            )
        }
        composable(route = Screen.Calendar.route) {
            CalendarScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.EntryTypeSelection.route) {
            EntryTypeSelectionScreen(
                onTypeSelected = { type ->
                    val route = when (type) {
                        EntryType.PAIN -> Screen.AddPain.route
                        EntryType.DRUG -> Screen.AddDrug.route
                        EntryType.SYMPTOM -> Screen.AddSymptom.route
                        EntryType.DISEASE -> Screen.AddDisease.route
                        EntryType.MEAL -> Screen.AddMeal.route
                        EntryType.SLEEP -> Screen.AddSleep.route
                        EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.route
                        EntryType.ACTIVITY -> Screen.AddActivity.route
                        EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.route
                        EntryType.JOURNAL -> Screen.AddJournal.route
                    }
                    navController.navigate(route)
                },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(route = Screen.AddPain.route) {
            AddPainScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddDrug.route) {
            AddDrugScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddSymptom.route) {
            AddSymptomScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddActivity.route) {
            AddActivityScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddMeal.route) {
            AddMealScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddSleep.route) {
            AddSleepScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddDisease.route) {
            AddDiseaseScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddMedicalAppointment.route) {
            AddMedicalAppointmentScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddExternalFactor.route) {
            AddExternalFactorScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
        composable(route = Screen.AddJournal.route) {
            AddJournalScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack(Screen.Timeline.route, inclusive = false)
                }
            )
        }
    }
}
