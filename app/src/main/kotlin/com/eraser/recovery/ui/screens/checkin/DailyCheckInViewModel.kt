package com.eraser.recovery.ui.screens.checkin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eraser.recovery.data.local.dao.DailyLogDao
import com.eraser.recovery.data.local.entity.DailyLogEntity
import com.eraser.recovery.domain.achievement.AchievementService
import com.eraser.recovery.domain.journey.JourneyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Daily Check-In ViewModel
 *
 * Manages state for daily check-in screen.
 * Handles submission and updates to daily logs.
 *
 * Research findings:
 * - Daily check-ins improve accountability
 * - Mood tracking helps identify patterns
 * - Simple binary choice reduces friction
 */
@HiltViewModel
class DailyCheckInViewModel @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val journeyService: JourneyService,
    private val achievementService: AchievementService
) : ViewModel() {

    companion object {
        private const val TAG = "DailyCheckInViewModel"
    }

    // State
    private val _wasClean = MutableStateFlow(true)
    val wasClean: StateFlow<Boolean> = _wasClean.asStateFlow()

    private val _moodRating = MutableStateFlow<Int?>(null)
    val moodRating: StateFlow<Int?> = _moodRating.asStateFlow()

    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _hasCheckedInToday = MutableStateFlow(false)
    val hasCheckedInToday: StateFlow<Boolean> = _hasCheckedInToday.asStateFlow()

    private val _submitSuccess = MutableStateFlow(false)
    val submitSuccess: StateFlow<Boolean> = _submitSuccess.asStateFlow()

    init {
        checkIfAlreadyCheckedIn()
    }

    /**
     * Check if user has already checked in today
     */
    private fun checkIfAlreadyCheckedIn() {
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val todayLog = dailyLogDao.getByDate(today.toEpochDay())

                if (todayLog != null) {
                    _hasCheckedInToday.value = true
                    _wasClean.value = todayLog.wasClean
                    _moodRating.value = todayLog.moodRating
                    _notes.value = todayLog.notes ?: ""
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if already checked in", e)
            }
        }
    }

    /**
     * Set clean/relapse status
     */
    fun setWasClean(wasClean: Boolean) {
        _wasClean.value = wasClean
    }

    /**
     * Set mood rating (1-5)
     */
    fun setMoodRating(rating: Int) {
        _moodRating.value = rating
    }

    /**
     * Set notes
     */
    fun setNotes(notes: String) {
        _notes.value = notes
    }

    /**
     * Submit check-in
     */
    fun submitCheckIn() {
        viewModelScope.launch {
            try {
                _isSubmitting.value = true

                val today = LocalDate.now()
                val dailyLog = DailyLogEntity(
                    date = today,
                    wasClean = _wasClean.value,
                    notes = if (_notes.value.isBlank()) null else _notes.value.trim(),
                    moodRating = _moodRating.value,
                    triggers = null, // Can be added in future enhancement
                    blockedAttempts = null, // Can be calculated from BlockedAttemptDao
                    createdAt = LocalDateTime.now()
                )

                // Save daily log
                dailyLogDao.insert(dailyLog)

                // Update journey (streak, etc.)
                if (_wasClean.value) {
                    journeyService.checkInToday(_moodRating.value ?: 3, _notes.value.ifBlank { null })
                } else {
                    journeyService.recordRelapse(_moodRating.value ?: 3, _notes.value.ifBlank { null })
                }

                // Check for newly unlocked achievements
                achievementService.checkAndUnlockAchievements()

                Log.d(TAG, "Check-in submitted successfully")

                _submitSuccess.value = true

            } catch (e: Exception) {
                Log.e(TAG, "Error submitting check-in", e)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    /**
     * Reset submit success state
     */
    fun resetSubmitSuccess() {
        _submitSuccess.value = false
    }
}

