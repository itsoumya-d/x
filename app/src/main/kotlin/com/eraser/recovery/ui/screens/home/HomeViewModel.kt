package com.eraser.recovery.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eraser.recovery.data.local.dao.BlockedAttemptDao
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.domain.journey.JourneyService
import com.eraser.recovery.domain.vpn.VpnManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Home Screen ViewModel
 * 
 * Manages home screen state including journey progress, VPN status, and statistics.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Real-time status updates improve user confidence
 * - Large day counter is most motivating element
 * - Streak indicator with fire icon increases engagement
 * - Quick access to protection toggle reduces friction
 * - Statistics cards provide progress feedback
 * 
 * @param context Application context
 * @param userDao User data access
 * @param blockedAttemptDao Blocked attempt data access
 * @param journeyService Journey service
 * @param vpnManager VPN manager
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao,
    private val blockedAttemptDao: BlockedAttemptDao,
    private val journeyService: JourneyService,
    private val vpnManager: VpnManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "HomeViewModel"
    }
    
    // State flows
    private val _daysSinceStart = MutableStateFlow(0)
    val daysSinceStart: StateFlow<Int> = _daysSinceStart.asStateFlow()
    
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()
    
    private val _isProtectionActive = MutableStateFlow(false)
    val isProtectionActive: StateFlow<Boolean> = _isProtectionActive.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _blockedToday = MutableStateFlow(0)
    val blockedToday: StateFlow<Int> = _blockedToday.asStateFlow()
    
    private val _blockedThisWeek = MutableStateFlow(0)
    val blockedThisWeek: StateFlow<Int> = _blockedThisWeek.asStateFlow()
    
    private val _blockedTotal = MutableStateFlow(0)
    val blockedTotal: StateFlow<Int> = _blockedTotal.asStateFlow()
    
    init {
        loadUserData()
        loadStatistics()
        observeJourneyState()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DATA LOADING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Load user data (days since start, streak)
     */
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                userDao.getUser().collect { user ->
                    if (user != null) {
                        // Calculate days since start
                        val daysSince = java.time.temporal.ChronoUnit.DAYS.between(
                            user.startDate,
                            LocalDate.now()
                        ).toInt()
                        
                        _daysSinceStart.value = daysSince
                        _currentStreak.value = user.currentStreak
                        
                        Log.d(TAG, "User data loaded: days=$daysSince, streak=${user.currentStreak}")
                    } else {
                        _daysSinceStart.value = 0
                        _currentStreak.value = 0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user data", e)
            }
        }
    }
    
    /**
     * Load statistics (blocked attempts)
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                // Blocked today
                val today = LocalDate.now()
                val todayStr = today.toString() // Format: YYYY-MM-DD
                val todayCount = blockedAttemptDao.getCountByDate(todayStr)
                _blockedToday.value = todayCount

                // Blocked this week
                val weekStart = today.minusDays(7)
                val weekStartStr = weekStart.toString()
                val weekCount = blockedAttemptDao.getCountBetweenDates(weekStartStr, todayStr)
                _blockedThisWeek.value = weekCount

                // Blocked total
                val totalCount = blockedAttemptDao.getTotalCount()
                _blockedTotal.value = totalCount

                Log.d(TAG, "Statistics loaded: today=$todayCount, week=$weekCount, total=$totalCount")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading statistics", e)
            }
        }
    }
    
    /**
     * Observe journey state from JourneyService
     */
    private fun observeJourneyState() {
        viewModelScope.launch {
            journeyService.isJourneyActive.collect { isActive ->
                _isProtectionActive.value = isActive
                Log.d(TAG, "Journey state changed: $isActive")
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PROTECTION CONTROL
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Prepare VPN permission intent
     * 
     * @return VPN permission intent or null if already granted
     */
    fun prepareVpnPermission(): Intent? {
        return VpnService.prepare(context)
    }
    
    /**
     * Start protection (VPN + Journey)
     */
    fun startProtection() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Start journey (which will start VPN)
                val result = journeyService.startJourney()
                
                if (result.isSuccess) {
                    Log.i(TAG, "Protection started successfully")
                    loadUserData()
                    loadStatistics()
                } else {
                    Log.e(TAG, "Failed to start protection: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error starting protection", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Stop protection (VPN + Journey)
     */
    fun stopProtection() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Stop journey (which will stop VPN)
                val result = journeyService.stopJourney()
                
                if (result.isSuccess) {
                    Log.i(TAG, "Protection stopped successfully")
                    loadUserData()
                } else {
                    Log.e(TAG, "Failed to stop protection: ${result.exceptionOrNull()?.message}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping protection", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // REFRESH
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Refresh all data
     */
    fun refresh() {
        loadUserData()
        loadStatistics()
    }
    
    /**
     * Get motivational message based on days
     */
    fun getMotivationalMessage(days: Int): String {
        return when {
            days == 0 -> "Start your journey today"
            days == 1 -> "Great start! Keep going"
            days < 7 -> "Building momentum"
            days < 14 -> "You're doing amazing!"
            days < 30 -> "Incredible progress!"
            days < 90 -> "You're unstoppable!"
            days < 180 -> "Legendary streak!"
            days < 365 -> "Almost a year!"
            else -> "You're a champion!"
        }
    }
}

