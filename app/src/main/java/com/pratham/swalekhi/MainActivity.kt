package com.pratham.swalekhi

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.pratham.swalekhi.ui.lock.LockScreen
import com.pratham.swalekhi.ui.navigation.SwalekhiNavHost
import com.pratham.swalekhi.ui.onboarding.OnboardingScreen
import com.pratham.swalekhi.ui.theme.SwalekhiTheme
import com.pratham.swalekhi.util.BiometricAuth
import com.pratham.swalekhi.work.NotificationHelper
import com.pratham.swalekhi.work.WorkScheduler

// BiometricPrompt requires a FragmentActivity (it hosts a headless Fragment internally to
// survive configuration changes) — Compose's setContent works the same on it as on ComponentActivity.
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SwalekhiApp

        NotificationHelper.ensureChannels(this)
        WorkScheduler.scheduleRecurringCheck(this)
        if (app.settingsRepository.dailyReminderEnabled.value) {
            WorkScheduler.scheduleDailyReminder(
                this,
                app.settingsRepository.reminderHour.value,
                app.settingsRepository.reminderMinute.value
            )
        }

        setContent {
            SwalekhiTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var showOnboarding by remember { mutableStateOf(!app.settingsRepository.hasSeenOnboarding) }
                    val lockRequired = app.settingsRepository.appLockEnabled.value && BiometricAuth.canAuthenticate(this)
                    var isUnlocked by remember { mutableStateOf(!lockRequired) }

                    when {
                        showOnboarding -> OnboardingScreen(onDone = {
                            app.settingsRepository.markOnboardingSeen()
                            showOnboarding = false
                        })
                        !isUnlocked -> {
                            LaunchedEffect(Unit) {
                                BiometricAuth.authenticate(
                                    activity = this@MainActivity,
                                    onSuccess = { isUnlocked = true },
                                    onFailure = { /* stay locked; user can retry via the button */ }
                                )
                            }
                            LockScreen(onUnlockClick = {
                                BiometricAuth.authenticate(
                                    activity = this@MainActivity,
                                    onSuccess = { isUnlocked = true },
                                    onFailure = { }
                                )
                            })
                        }
                        else -> SwalekhiNavHost(app = app)
                    }
                }
            }
        }
    }
}
