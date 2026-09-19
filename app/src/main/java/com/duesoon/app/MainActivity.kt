package com.duesoon.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.duesoon.app.data.repository.UserPreferencesState
import com.duesoon.app.navigation.AppNavigation
import com.duesoon.app.ui.theme.DueSoonTheme
import com.duesoon.app.ui.theme.LocalHapticFeedbackEnabled
import com.duesoon.app.ui.theme.LocalVisualDensity

class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_QUICK_ADD = "com.duesoon.app.action.QUICK_ADD"
    }

    private val _quickAddTrigger = kotlinx.coroutines.flow.MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        checkQuickAddIntent(intent)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }

        val appContainer = (application as DueSoonApplication).container
        val userPreferencesRepository = appContainer.userPreferencesRepository

        setContent {
            val userPreferences by userPreferencesRepository.userPreferencesFlow.collectAsState(initial = UserPreferencesState())
            val triggerQuickAdd by _quickAddTrigger.collectAsState()
            
            val isDarkTheme = when (userPreferences.theme) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            CompositionLocalProvider(
                LocalVisualDensity provides userPreferences.visualDensity,
                LocalHapticFeedbackEnabled provides userPreferences.hapticsEnabled
            ) {
                DueSoonTheme(accentPalette = userPreferences.accentPalette, darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            quickAddTrigger = triggerQuickAdd,
                            onQuickAddHandled = { _quickAddTrigger.value = false }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        checkQuickAddIntent(intent)
    }

    private fun checkQuickAddIntent(intent: android.content.Intent?) {
        if (intent?.action == ACTION_QUICK_ADD || intent?.getBooleanExtra("quick_add", false) == true) {
            _quickAddTrigger.value = true
        }
    }
}
