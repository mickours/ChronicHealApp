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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.chronicheal.app.data.notification.NotificationHelper
import org.chronicheal.app.data.worker.BackupManager
import org.chronicheal.app.data.worker.MissingEntryWorker
import org.chronicheal.app.domain.model.EntryType
import org.chronicheal.app.domain.repository.SettingsRepository
import org.chronicheal.app.presentation.MainViewModel
import org.chronicheal.app.presentation.SecurityViewModel
import org.chronicheal.app.presentation.navigation.NavGraph
import org.chronicheal.app.presentation.navigation.Screen
import org.chronicheal.app.ui.theme.ChronicHealTheme
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val securityViewModel: SecurityViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    data class PendingAction(
        val type: String?,
        val reminderId: Long?,
        val templateEntryId: Long?,
        val isLogNow: Boolean,
        val action: String? = null
    )
    private val pendingEntryAction = MutableStateFlow<PendingAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        handleIntent(intent)

        // Ensure daily backup is scheduled if enabled
        lifecycleScope.launch {
            if (settingsRepository.isAutoBackupEnabled.first()) {
                backupManager.scheduleDailyBackup()
            }
            scheduleMissingEntryDetection()
        }

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
                            entryAction?.let { action ->
                                var route: String? = null

                                when (action.action) {
                                    ACTION_VOICE_LOGGING -> {
                                        route = Screen.VoiceLogging.route
                                    }

                                    ACTION_CHECKUP -> {
                                        route = Screen.AddCompleteEntry.createRoute()
                                    }

                                    else -> {
                                        val entryType = action.type?.let {
                                            try {
                                                EntryType.valueOf(it)
                                            } catch (_: Exception) {
                                                null
                                            }
                                        }

                                        route = when (entryType) {
                                            EntryType.PAIN -> Screen.AddPain.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.DRUG -> Screen.AddDrug.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.SYMPTOM -> Screen.AddSymptom.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.DISEASE -> Screen.AddDisease.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.MEAL -> Screen.AddMeal.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.SLEEP -> Screen.AddSleep.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.MEDICAL_APPOINTMENT -> Screen.AddMedicalAppointment.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.ACTIVITY -> Screen.AddActivity.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.EXTERNAL_FACTOR -> Screen.AddExternalFactor.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.JOURNAL -> Screen.AddJournal.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.PERIOD -> Screen.AddPeriod.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.BEVERAGE -> Screen.AddBeverage.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.STOOL -> Screen.AddStool.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.MOOD -> Screen.AddMood.createRoute(
                                                templateId = action.templateEntryId,
                                                reminderId = action.reminderId
                                            )

                                            EntryType.VOICE_LOGGING -> Screen.VoiceLogging.route
                                            null -> {
                                                if (action.reminderId != null) {
                                                    Screen.AddCompleteEntry.createRoute()
                                                } else {
                                                    null
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                route?.let {
                                    navController.navigate(it) {
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
        val action = intent?.action
        val type = intent?.getStringExtra(NotificationHelper.EXTRA_ENTRY_TYPE)
        val reminderId = intent?.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)?.takeIf { it != -1L }
        val templateId = intent?.getLongExtra(NotificationHelper.EXTRA_TEMPLATE_ENTRY_ID, -1L)
            ?.takeIf { it != -1L }
        val isLogNow = intent?.getBooleanExtra(NotificationHelper.EXTRA_IS_LOG_NOW, false) ?: false

        if (action == ACTION_VOICE_LOGGING || action == ACTION_CHECKUP || type != null || reminderId != null || templateId != null) {
            // Cancel notification when handling it
            reminderId?.let { notificationHelper.cancelNotification(it.toInt()) }

            pendingEntryAction.value = PendingAction(type, reminderId, templateId, isLogNow, action)
            intent.removeExtra(NotificationHelper.EXTRA_ENTRY_TYPE)
            intent.removeExtra(NotificationHelper.EXTRA_REMINDER_ID)
            intent.removeExtra(NotificationHelper.EXTRA_TEMPLATE_ENTRY_ID)
            intent.removeExtra(NotificationHelper.EXTRA_IS_LOG_NOW)
        }
    }

    private fun scheduleMissingEntryDetection() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<MissingEntryWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "missing_entry_detection",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
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

    companion object {
        const val ACTION_VOICE_LOGGING = "org.chronicheal.app.ACTION_VOICE_LOGGING"
        const val ACTION_CHECKUP = "org.chronicheal.app.ACTION_CHECKUP"
    }
}
