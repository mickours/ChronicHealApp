package org.chronicheal.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
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
import org.chronicheal.app.data.notification.NotificationHelper
import org.chronicheal.app.presentation.MainViewModel
import org.chronicheal.app.presentation.SecurityViewModel
import org.chronicheal.app.presentation.navigation.NavGraph
import org.chronicheal.app.presentation.navigation.Screen
import org.chronicheal.app.ui.theme.ChronicHealTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val securityViewModel: SecurityViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            ChronicHealTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isBiometricLockEnabled by securityViewModel.isBiometricLockEnabled.collectAsState()
                    val isWelcomeWizardCompleted by mainViewModel.isWelcomeWizardCompleted.collectAsState()
                    var isAuthenticated by remember { mutableStateOf(false) }

                    if (isBiometricLockEnabled && !isAuthenticated) {
                        Box(modifier = Modifier.fillMaxSize())
                        showBiometricPrompt {
                            isAuthenticated = true
                        }
                    } else if (isWelcomeWizardCompleted != null) {
                        val navController = rememberNavController()
                        
                        val startDestination = if (isWelcomeWizardCompleted == true) {
                            Screen.Timeline.route
                        } else {
                            Screen.WelcomeWizard.route
                        }

                        // Handle navigation from notification actions
                        val entryType = intent.getStringExtra(NotificationHelper.EXTRA_ENTRY_TYPE)
                        LaunchedEffect(entryType) {
                            if (entryType == "PAIN") {
                                navController.navigate(Screen.BodyScan.route) {
                                    popUpTo(Screen.Timeline.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                intent.removeExtra(NotificationHelper.EXTRA_ENTRY_TYPE)
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
    }
}
