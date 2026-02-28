package org.chronicheal.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.presentation.AddActivityScreen
import org.chronicheal.app.presentation.AddDrugScreen
import org.chronicheal.app.presentation.AddMealScreen
import org.chronicheal.app.presentation.AddPainScreen
import org.chronicheal.app.presentation.AddSleepScreen
import org.chronicheal.app.presentation.AddSymptomScreen
import org.chronicheal.app.presentation.EntryTypeSelectionScreen
import org.chronicheal.app.presentation.TimelineScreen

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
                }
            )
        }
        composable(route = Screen.EntryTypeSelection.route) {
            EntryTypeSelectionScreen(
                onTypeSelected = { type ->
                    when (type) {
                        EntryType.PAIN -> navController.navigate(Screen.AddPain.route)
                        EntryType.DRUG -> navController.navigate(Screen.AddDrug.route)
                        EntryType.SYMPTOM -> navController.navigate(Screen.AddSymptom.route)
                        EntryType.ACTIVITY -> navController.navigate(Screen.AddActivity.route)
                        EntryType.MEAL -> navController.navigate(Screen.AddMeal.route)
                        EntryType.SLEEP -> navController.navigate(Screen.AddSleep.route)
                        else -> {
                            // TODO: Implement other specialized screens
                            navController.popBackStack()
                        }
                    }
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
    }
}
