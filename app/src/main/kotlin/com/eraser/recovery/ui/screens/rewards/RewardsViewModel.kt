package com.eraser.recovery.ui.screens.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eraser.recovery.data.local.entity.AchievementEntity
import com.eraser.recovery.domain.achievement.AchievementService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Rewards ViewModel
 * 
 * Manages rewards screen state and achievement data.
 */
@HiltViewModel
class RewardsViewModel @Inject constructor(
    private val achievementService: AchievementService
) : ViewModel() {
    
    private val _achievements = MutableStateFlow<List<AchievementEntity>>(emptyList())
    val achievements: StateFlow<List<AchievementEntity>> = _achievements.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadAchievements()
    }
    
    /**
     * Load all achievements
     */
    private fun loadAchievements() {
        viewModelScope.launch {
            _isLoading.value = true
            achievementService.getAllAchievements().collect { achievementList ->
                _achievements.value = achievementList
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh achievements
     */
    fun refresh() {
        loadAchievements()
    }
}

