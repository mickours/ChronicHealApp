package org.chronicheal.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import org.chronicheal.app.data.notification.NotificationHelper
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.presentation.MainViewModel
import org.chronicheal.app.presentation.SecurityViewModel
import org.chronicheal.app.presentation.navigation.NavGraph
import org.chronicheal.app.presentation.navigation.Screen
import org.chronicheal.app.ui.theme.ChronicHealTheme
import java.time.LocalDateTime

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val securityViewModel: SecurityViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private val pendingEntryAction = MutableStateFlow<Pair<String, Long?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        setContent {
            ChronicHealTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isBiometricLockEnabled by securityViewModel.isBiometricLockEnabled.collectAsState()
                    val isWelcomeWizardCompleted by mainViewModel.isWelcomeWizardCompleted.collectAsState()
                    var isAuthenticated by remember { mutableStateOf(false) }

                    LaunchedEffect(isBiometricLockEnabled, isAuthenticated) {
                        if (isBiometricLockEnabled && !isAuthenticated) {
                            showBiometricPrompt {
                                isAuthenticated = true
                            }
                        }
                    }

                    if (isBiometricLockEnabled && !isAuthenticated) {
                        // Empty screen while authenticating
                        Box(modifier = Modifier.fillMaxSize())
                    } else if (isWelcomeWizardCompleted != null) {
                        val navController = rememberNavController()
                        
                        val startDestination = if (isWelcomeWizardCompleted == true) {
                            Screen.Timeline.route
                        } else {
                            Screen.WelcomeWizard.route
                        }

                        // Handle navigation from notification actions
                        val entryAction by pendingEntryAction.collectAsState()
                        LaunchedEffect(entryAction) {
                            entryAction?.let { (typeName, reminderId) ->
                                val entryType = try { EntryType.valueOf(typeName) } catch(_: Exception) { null }
                                if (entryType != null) {
                                    val route = when (entryType) {
                                        EntryType.PAIN -> Screen.BodyScan.createRoute(date = LocalDateTime.now().toString())
                                        EntryType.DRUG -> Screen.AddDrug.createRoute(id = reminderId)
                                        EntryType.SYMPTOM -> Screen.AddSymptom.createRoute(id = reminderId)
                                        EntryType.DISEASE -> Screen.AddDisease.createRoute(id = reminderId)
                                        EntryType.MEAL -> Screen.AddMeal.createRoute(id = reminderId)
                                        EntryType.SLEEP -> Screen.AddSleep.createRoute(id = reminderId)
                                        EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute(id = reminderId)
                                        EntryType.ACTIVITY -> Screen.AddActivity.createRoute(id = reminderId)
                                        EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute(id = reminderId)
                                        EntryType.JOURNAL -> Screen.AddJournal.createRoute(id = reminderId)
                                        EntryType.PERIOD -> Screen.AddPeriod.createRoute(id = reminderId)
                                        EntryType.BEVERAGE -> Screen.AddBeverage.createRoute(id = reminderId)
                                        EntryType.STOOL -> Screen.AddStool.createRoute(id = reminderId)
                                        EntryType.MOOD -> Screen.AddMood.createRoute(id = reminderId)
                                        EntryType.VOICE_LOGGING -> Screen.VoiceLogging.route
                                    }
                                    navController.navigate(route) {
                                        popUpTo(Screen.Timeline.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                pendingEntryAction.value = null
                            }
                        }
                        
                        NavGraph(
                            navController = navController,
                            startDestination = startDestination
                        )
                    } else {
                        // Loading state while checking wizard completion
                        Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val type = intent?.getStringExtra(NotificationHelper.EXTRA_ENTRY_TYPE)
        val reminderId = intent?.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)?.takeIf { it != -1L }
        
        if (type != null) {
            pendingEntryAction.value = type to reminderId
            intent.removeExtra(NotificationHelper.EXTRA_ENTRY_TYPE)
            intent.removeExtra(NotificationHelper.EXTRA_REMINDER_ID)
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && 
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for ChronicHeal")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Exit")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}
