package com.eraser.recovery.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eraser.recovery.domain.statistics.ChartDataPoint
import com.eraser.recovery.domain.statistics.StatisticsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reports ViewModel
 * 
 * Manages reports screen state and statistics data.
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val statisticsService: StatisticsService
) : ViewModel() {
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()
    
    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()
    
    private val _totalDaysClean = MutableStateFlow(0)
    val totalDaysClean: StateFlow<Int> = _totalDaysClean.asStateFlow()
    
    private val _successRate = MutableStateFlow(0.0)
    val successRate: StateFlow<Double> = _successRate.asStateFlow()
    
    private val _blockedAttempts = MutableStateFlow(0)
    val blockedAttempts: StateFlow<Int> = _blockedAttempts.asStateFlow()
    
    private val _completedInterventions = MutableStateFlow(0)
    val completedInterventions: StateFlow<Int> = _completedInterventions.asStateFlow()
    
    private val _chartData = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartData: StateFlow<List<ChartDataPoint>> = _chartData.asStateFlow()
    
    /**
     * Load all statistics
     */
    fun loadStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Get dashboard statistics
                val dashboardStats = statisticsService.getDashboardStatistics()
                
                _currentStreak.value = dashboardStats["currentStreak"] as? Int ?: 0
                _longestStreak.value = dashboardStats["longestStreak"] as? Int ?: 0
                _totalDaysClean.value = dashboardStats["totalCleanDays"] as? Int ?: 0
                _successRate.value = dashboardStats["interventionSuccessRate"] as? Double ?: 0.0
                _blockedAttempts.value = dashboardStats["totalBlockedAttempts"] as? Int ?: 0
                _completedInterventions.value = dashboardStats["completedInterventions"] as? Int ?: 0
                
                // Get chart data (last 7 days)
                val streakChartData = statisticsService.getStreakProgressChartData(days = 7)
                _chartData.value = streakChartData
                
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh statistics
     */
    fun refresh() {
        loadStatistics()
    }
}

