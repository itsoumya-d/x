package com.eraser.recovery.ui.screens.safepage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eraser.recovery.data.local.dao.BlockedAttemptDao
import com.eraser.recovery.data.local.dao.InterventionSessionDao
import com.eraser.recovery.data.local.entity.BlockedAttemptEntity
import com.eraser.recovery.domain.flashcard.FlashcardService
import com.eraser.recovery.data.local.entity.FlashcardEntity
import com.eraser.recovery.data.local.entity.InterventionSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Safe Page ViewModel
 * 
 * Manages safe page state, flashcard display, and intervention tracking.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Show blocked domain prominently for transparency
 * - Display random flashcard with 3D flip animation
 * - Track intervention completion for analytics
 * - Provide skip option but encourage completion
 * - Show statistics to motivate user
 */
@HiltViewModel
class SafePageViewModel @Inject constructor(
    private val flashcardService: FlashcardService,
    private val blockedAttemptDao: BlockedAttemptDao,
    private val interventionSessionDao: InterventionSessionDao
) : ViewModel() {
    
    companion object {
        private const val TAG = "SafePageViewModel"
    }
    
    // Current flashcard
    private val _flashcard = MutableStateFlow<FlashcardEntity?>(null)
    val flashcard: StateFlow<FlashcardEntity?> = _flashcard.asStateFlow()
    
    // Current intervention session
    private val _session = MutableStateFlow<InterventionSessionEntity?>(null)
    val session: StateFlow<InterventionSessionEntity?> = _session.asStateFlow()
    
    // Blocked attempt ID
    private val _blockedAttemptId = MutableStateFlow<Long?>(null)
    val blockedAttemptId: StateFlow<Long?> = _blockedAttemptId.asStateFlow()
    
    // Statistics
    private val _completedCount = MutableStateFlow(0)
    val completedCount: StateFlow<Int> = _completedCount.asStateFlow()
    
    private val _skippedCount = MutableStateFlow(0)
    val skippedCount: StateFlow<Int> = _skippedCount.asStateFlow()
    
    private val _completionRate = MutableStateFlow(0)
    val completionRate: StateFlow<Int> = _completionRate.asStateFlow()
    
    // Session start time
    private var sessionStartTime: LocalDateTime? = null
    
    // Task completed flag
    private val _taskCompleted = MutableStateFlow(false)
    val taskCompleted: StateFlow<Boolean> = _taskCompleted.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Initialize safe page with blocked domain
     */
    fun initialize(blockedDomain: String, blockedUrl: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                sessionStartTime = LocalDateTime.now()
                
                // Load random flashcard
                loadRandomFlashcard(blockedDomain, blockedUrl)
                
                // Log blocked attempt
                logBlockedAttempt(blockedDomain, blockedUrl)
                
                // Load statistics
                loadStatistics()
                
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing safe page", e)
                _isLoading.value = false
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // FLASHCARD LOADING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Load random flashcard and start intervention session
     */
    private suspend fun loadRandomFlashcard(blockedDomain: String, blockedUrl: String?) {
        try {
            // Get random flashcard
            val flashcard = flashcardService.getRandomFlashcard()
            
            if (flashcard != null) {
                _flashcard.value = flashcard
                
                // Start intervention session
                val session = flashcardService.startSession(
                    blockedDomain = blockedDomain,
                    blockedUrl = blockedUrl,
                    flashcardId = flashcard.flashcardId
                )
                
                _session.value = session
                
                Log.d(TAG, "Loaded flashcard: ${flashcard.flashcardId}, session: ${session.id}")
            } else {
                Log.w(TAG, "No flashcard available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading flashcard", e)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BLOCKED ATTEMPT LOGGING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Log blocked attempt to database
     */
    private suspend fun logBlockedAttempt(blockedDomain: String, blockedUrl: String?) {
        try {
            val attempt = BlockedAttemptEntity(
                domain = blockedDomain,
                url = blockedUrl ?: "https://$blockedDomain",
                timestamp = java.time.LocalDateTime.now(),
                interventionCompleted = false
            )
            
            val id = blockedAttemptDao.insert(attempt)
            _blockedAttemptId.value = id
            
            Log.d(TAG, "Logged blocked attempt: $blockedDomain (ID: $id)")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging blocked attempt", e)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS LOADING
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Load intervention statistics
     */
    private suspend fun loadStatistics() {
        try {
            val completed = interventionSessionDao.getCompletedCount()
            val skipped = interventionSessionDao.getSkippedCount()
            val total = completed + skipped
            
            _completedCount.value = completed
            _skippedCount.value = skipped
            _completionRate.value = if (total > 0) {
                ((completed.toFloat() / total) * 100).toInt()
            } else {
                0
            }
            
            Log.d(TAG, "Statistics: completed=$completed, skipped=$skipped, rate=${_completionRate.value}%")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading statistics", e)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TASK COMPLETION
    // ═══════════════════════════════════════════════════════════════════════════════
    
    /**
     * Handle task completion
     */
    fun completeTask() {
        viewModelScope.launch {
            try {
                val session = _session.value
                val sessionStart = sessionStartTime
                
                if (session == null || sessionStart == null) {
                    Log.w(TAG, "Cannot complete task: session or start time is null")
                    _taskCompleted.value = true
                    return@launch
                }
                
                // Calculate duration
                val durationSeconds = java.time.Duration.between(sessionStart, LocalDateTime.now()).seconds.toInt()
                
                // Complete task in flashcard service
                flashcardService.completeTask(
                    sessionId = session.id,
                    durationSeconds = durationSeconds
                )
                
                // Update blocked attempt
                val attemptId = _blockedAttemptId.value
                if (attemptId != null) {
                    val attempt = blockedAttemptDao.getById(attemptId)
                    if (attempt != null) {
                        blockedAttemptDao.update(
                            attempt.copy(interventionCompleted = true)
                        )
                    }
                }
                
                _taskCompleted.value = true
                
                Log.d(TAG, "Task completed: session=${session.id}, duration=${durationSeconds}s")
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task", e)
            }
        }
    }
    
    /**
     * Handle task skip
     */
    fun skipTask() {
        viewModelScope.launch {
            try {
                val session = _session.value
                
                if (session == null) {
                    Log.w(TAG, "Cannot skip task: session is null")
                    _taskCompleted.value = true
                    return@launch
                }
                
                // Skip task in flashcard service
                flashcardService.skipTask(session.id)
                
                // Update blocked attempt (not completed)
                val attemptId = _blockedAttemptId.value
                if (attemptId != null) {
                    val attempt = blockedAttemptDao.getById(attemptId)
                    if (attempt != null) {
                        blockedAttemptDao.update(
                            attempt.copy(interventionCompleted = false)
                        )
                    }
                }
                
                _taskCompleted.value = true
                
                Log.d(TAG, "Task skipped: session=${session.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error skipping task", e)
            }
        }
    }
    
    /**
     * Handle early exit (user closes screen without completing or skipping)
     */
    fun exitEarly() {
        viewModelScope.launch {
            try {
                val session = _session.value
                
                if (session != null && !_taskCompleted.value) {
                    flashcardService.exitSession(session.id)
                    Log.d(TAG, "Session exited early: ${session.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exiting session", e)
            }
        }
    }
}

