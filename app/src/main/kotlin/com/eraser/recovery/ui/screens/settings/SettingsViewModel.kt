package com.eraser.recovery.ui.screens.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.eraser.recovery.BuildConfig
import com.eraser.recovery.data.local.EraserDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings ViewModel
 * 
 * Manages settings state and preferences.
 * Uses DataStore for persistent storage.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val database: EraserDatabase
) : AndroidViewModel(application) {
    
    private val context: Context = application.applicationContext
    private val sharedPrefs = context.getSharedPreferences("eraser_settings", Context.MODE_PRIVATE)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load settings from SharedPreferences
     */
    private fun loadSettings() {
        viewModelScope.launch {
            val dailyReminders = sharedPrefs.getBoolean(PREF_DAILY_REMINDERS, true)
            val streakWarnings = sharedPrefs.getBoolean(PREF_STREAK_WARNINGS, true)
            val achievementNotifications = sharedPrefs.getBoolean(PREF_ACHIEVEMENT_NOTIFICATIONS, true)
            val autoStartOnBoot = sharedPrefs.getBoolean(PREF_AUTO_START_ON_BOOT, true)
            val batteryOptimizationExempt = isBatteryOptimizationExempt()
            val themeMode = ThemeMode.valueOf(sharedPrefs.getString(PREF_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
            
            _uiState.value = SettingsUiState(
                dailyRemindersEnabled = dailyReminders,
                streakWarningsEnabled = streakWarnings,
                achievementNotificationsEnabled = achievementNotifications,
                autoStartOnBoot = autoStartOnBoot,
                batteryOptimizationExempt = batteryOptimizationExempt,
                themeMode = themeMode,
                appVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            )
        }
    }
    
    /**
     * Toggle daily reminders
     */
    fun toggleDailyReminders(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_DAILY_REMINDERS, enabled).apply()
        _uiState.value = _uiState.value.copy(dailyRemindersEnabled = enabled)
        
        // TODO: Schedule/cancel daily reminder notifications
    }
    
    /**
     * Toggle streak warnings
     */
    fun toggleStreakWarnings(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_STREAK_WARNINGS, enabled).apply()
        _uiState.value = _uiState.value.copy(streakWarningsEnabled = enabled)
        
        // TODO: Schedule/cancel streak warning notifications
    }
    
    /**
     * Toggle achievement notifications
     */
    fun toggleAchievementNotifications(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_ACHIEVEMENT_NOTIFICATIONS, enabled).apply()
        _uiState.value = _uiState.value.copy(achievementNotificationsEnabled = enabled)
    }
    
    /**
     * Toggle auto-start on boot
     */
    fun toggleAutoStartOnBoot(enabled: Boolean) {
        sharedPrefs.edit().putBoolean(PREF_AUTO_START_ON_BOOT, enabled).apply()
        _uiState.value = _uiState.value.copy(autoStartOnBoot = enabled)
    }
    
    /**
     * Toggle battery optimization
     */
    fun toggleBatteryOptimization(enabled: Boolean) {
        if (enabled) {
            // Request battery optimization exemption
            requestBatteryOptimizationExemption()
        }
        // Note: We can't programmatically disable the exemption
        // User must do it manually in system settings
    }
    
    /**
     * Check if battery optimization is exempt
     */
    private fun isBatteryOptimizationExempt(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    /**
     * Request battery optimization exemption
     */
    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to battery optimization settings
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Show theme selection dialog
     */
    fun showThemeDialog() {
        // TODO: Implement theme selection dialog
        // For now, cycle through themes
        val currentTheme = _uiState.value.themeMode
        val nextTheme = when (currentTheme) {
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
        }
        setThemeMode(nextTheme)
    }
    
    /**
     * Set theme mode
     */
    private fun setThemeMode(themeMode: ThemeMode) {
        sharedPrefs.edit().putString(PREF_THEME_MODE, themeMode.name).apply()
        _uiState.value = _uiState.value.copy(themeMode = themeMode)
        
        // TODO: Apply theme change to app
    }
    
    /**
     * Clear blocked history
     */
    fun clearBlockedHistory() {
        viewModelScope.launch {
            database.blockedAttemptDao().deleteAll()
        }
    }
    
    /**
     * Reset all progress
     */
    fun resetProgress() {
        viewModelScope.launch {
            // Clear all database tables
            database.clearAllTables()
            
            // Reset SharedPreferences
            sharedPrefs.edit().clear().apply()
            
            // Reload settings
            loadSettings()
        }
    }
    
    /**
     * Open privacy policy
     */
    fun openPrivacyPolicy() {
        // TODO: Open privacy policy screen or URL
    }
    
    /**
     * Open developer info
     */
    fun openDeveloperInfo() {
        // TODO: Open developer info screen
    }
    
    companion object {
        private const val PREF_DAILY_REMINDERS = "daily_reminders"
        private const val PREF_STREAK_WARNINGS = "streak_warnings"
        private const val PREF_ACHIEVEMENT_NOTIFICATIONS = "achievement_notifications"
        private const val PREF_AUTO_START_ON_BOOT = "auto_start_on_boot"
        private const val PREF_THEME_MODE = "theme_mode"
    }
}

/**
 * Settings UI State
 */
data class SettingsUiState(
    val dailyRemindersEnabled: Boolean = true,
    val streakWarningsEnabled: Boolean = true,
    val achievementNotificationsEnabled: Boolean = true,
    val autoStartOnBoot: Boolean = true,
    val batteryOptimizationExempt: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "v1.0.0"
)

/**
 * Theme Mode
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System Default")
}

