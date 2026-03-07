package com.cedd.utangtracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import com.cedd.utangtracker.data.repository.UtangRepository
import com.cedd.utangtracker.navigation.AppNavigation
import com.cedd.utangtracker.presentation.onboarding.OnboardingScreen
import com.cedd.utangtracker.presentation.setup.NameSetupScreen
import com.cedd.utangtracker.presentation.splash.SplashScreen
import com.cedd.utangtracker.worker.OverdueReminderWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.compose.ui.graphics.Color as ComposeColor

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var prefs: PreferencesRepository
    @Inject lateinit var repo: UtangRepository

    private val isAuthenticated = mutableStateOf(false)
    private val isSplashDone    = mutableStateOf(false)

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* proceed either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannels()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch { repo.markOverdueDebts() }
        scheduleOverdueReminder()
        enableEdgeToEdge()
        setContent {
            val isDark            by prefs.isDarkMode.collectAsStateWithLifecycle(false)
            val biometricEnabled  by prefs.isBiometricEnabled.collectAsStateWithLifecycle(false)
            val hasSeenOnboarding by prefs.hasSeenOnboarding.collectAsStateWithLifecycle(true)
            val hasSeenTour       by prefs.hasSeenTour.collectAsStateWithLifecycle(true)
            val lenderName        by prefs.lenderName.collectAsStateWithLifecycle("")
            val authenticated  by isAuthenticated
            val splashDone     by isSplashDone

            UtangTrackerTheme(isDark) {
                if (!splashDone) {
                    SplashScreen(onFinished = { isSplashDone.value = true })
                } else {
                    when {
                        !hasSeenOnboarding -> {
                            OnboardingScreen(
                                onFinish = { lifecycleScope.launch { prefs.setSeenOnboarding() } }
                            )
                        }
                        biometricEnabled && !authenticated -> {
                            LockScreen(onUnlock = { showBiometricPrompt() })
                        }
                        lenderName.isBlank() -> {
                            NameSetupScreen(
                                onSave = { name -> lifecycleScope.launch { prefs.setLenderName(name) } }
                            )
                        }
                        else -> {
                            AppNavigation(
                                showTour = !hasSeenTour,
                                onTourComplete = { lifecycleScope.launch { prefs.setSeenTour() } }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        val bm = BiometricManager.from(this)
        if (bm.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            isAuthenticated.value = true   // device doesn't support biometrics — bypass
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    isAuthenticated.value = true
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { /* stay locked */ }
                override fun onAuthenticationFailed() { /* stay locked */ }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("LoanTrack")
            .setSubtitle("Verify your identity to continue")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel("contract_signing", "Contract Signing", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Notified when a borrower signs remotely" }
            )
            nm.createNotificationChannel(
                NotificationChannel(OverdueReminderWorker.CHANNEL_OVERDUE, "Overdue Reminders", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Daily reminders for overdue debts" }
            )
            nm.createNotificationChannel(
                NotificationChannel(OverdueReminderWorker.CHANNEL_UPCOMING, "Upcoming Due Dates", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "3-day warning before a debt is due" }
            )
        }
    }

    private fun scheduleOverdueReminder() {
        val request = PeriodicWorkRequestBuilder<OverdueReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            OverdueReminderWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

// ── Lock Screen ────────────────────────────────────────────────────────────────

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text("LoanTrack", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text("Verify your identity to access your records.",
                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(40.dp))
            Button(onClick = onUnlock, modifier = Modifier.fillMaxWidth(0.6f)) {
                Icon(Icons.Default.Fingerprint, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Unlock")
            }
        }
    }
}

// ── Theme ──────────────────────────────────────────────────────────────────────

private val LightColors = lightColorScheme(
    primary             = ComposeColor(0xFF4361EE),
    onPrimary           = ComposeColor(0xFFFFFFFF),
    primaryContainer    = ComposeColor(0xFF1E3A8A),
    onPrimaryContainer  = ComposeColor(0xFFFFFFFF),
    secondary           = ComposeColor(0xFF16A34A),
    onSecondary         = ComposeColor(0xFFFFFFFF),
    tertiary            = ComposeColor(0xFFDC2626),
    onTertiary          = ComposeColor(0xFFFFFFFF),
    background          = ComposeColor(0xFFEEF2FF),
    onBackground        = ComposeColor(0xFF1A1C2E),
    surface             = ComposeColor(0xFFFFFFFF),
    onSurface           = ComposeColor(0xFF1A1C2E),
    surfaceVariant      = ComposeColor(0xFFF0F4FF),
    onSurfaceVariant    = ComposeColor(0xFF5A6490),
)

private val DarkColors = darkColorScheme(
    primary             = ComposeColor(0xFF7B8FF7),
    onPrimary           = ComposeColor(0xFFFFFFFF),
    primaryContainer    = ComposeColor(0xFF2B3A8F),
    onPrimaryContainer  = ComposeColor(0xFFD6E4FF),
    secondary           = ComposeColor(0xFF4ADE80),
    onSecondary         = ComposeColor(0xFF052E16),
    tertiary            = ComposeColor(0xFFFF6B6B),
    onTertiary          = ComposeColor(0xFF2D0000),
    background          = ComposeColor(0xFF0A0A18),
    onBackground        = ComposeColor(0xFFE8EAFF),
    surface             = ComposeColor(0xFF141428),
    onSurface           = ComposeColor(0xFFE8EAFF),
    surfaceVariant      = ComposeColor(0xFF1C1C38),
    onSurfaceVariant    = ComposeColor(0xFF9BA4C4),
)

@Composable
fun UtangTrackerTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
