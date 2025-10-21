package com.eraser.recovery.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.PowerManager
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding ViewModel
 * 
 * Manages onboarding state, permissions, and completion.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Progressive permission requests (ask when needed, not all at once)
 * - Clear explanations before requesting permissions
 * - Skip button for flexibility
 * - Battery optimization exemption for VPN reliability
 * - Onboarding completion tracking
 * 
 * @param context Application context
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "OnboardingViewModel"
        private val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
    
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")
    
    // State flows
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage
    
    private val _batteryOptimizationGranted = MutableStateFlow(false)
    val batteryOptimizationGranted: StateFlow<Boolean> = _batteryOptimizationGranted
    
    private val _vpnPermissionGranted = MutableStateFlow(false)
    val vpnPermissionGranted: StateFlow<Boolean> = _vpnPermissionGranted
    
    init {
        checkBatteryOptimizationStatus()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PAGE NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Update current page
     */
    fun updatePage(page: Int) {
        _currentPage.value = page
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PERMISSIONS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Prepare VPN permission intent
     * 
     * @return VPN permission intent or null if already granted
     */
    fun prepareVpnPermission(): Intent? {
        val intent = VpnService.prepare(context)
        if (intent == null) {
            _vpnPermissionGranted.value = true
        }
        return intent
    }
    
    /**
     * Mark VPN permission as granted
     */
    fun onVpnPermissionGranted() {
        _vpnPermissionGranted.value = true
    }
    
    /**
     * Check battery optimization status
     */
    private fun checkBatteryOptimizationStatus() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        _batteryOptimizationGranted.value = isIgnoringBatteryOptimizations
    }
    
    /**
     * Prepare battery optimization exemption intent
     * 
     * @return Battery optimization settings intent
     */
    fun prepareBatteryOptimizationIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
    
    /**
     * Refresh battery optimization status
     */
    fun refreshBatteryOptimizationStatus() {
        checkBatteryOptimizationStatus()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ONBOARDING COMPLETION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Check if onboarding is complete
     * 
     * @return True if complete, false otherwise
     */
    suspend fun isOnboardingComplete(): Boolean {
        return context.dataStore.data.map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETE] ?: false
        }.first()
    }
    
    /**
     * Mark onboarding as complete
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            context.dataStore.edit { preferences ->
                preferences[KEY_ONBOARDING_COMPLETE] = true
            }
        }
    }
}

