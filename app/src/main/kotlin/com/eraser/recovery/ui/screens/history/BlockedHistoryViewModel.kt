package com.eraser.recovery.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eraser.recovery.data.local.dao.BlockedAttemptDao
import com.eraser.recovery.data.local.entity.BlockedAttemptEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Blocked History ViewModel
 * 
 * Manages blocked history screen state and data.
 */
@HiltViewModel
class BlockedHistoryViewModel @Inject constructor(
    private val blockedAttemptDao: BlockedAttemptDao
) : ViewModel() {
    
    private val _blockedAttempts = MutableStateFlow<List<BlockedAttemptEntity>>(emptyList())
    val blockedAttempts: StateFlow<List<BlockedAttemptEntity>> = _blockedAttempts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Load all blocked attempts
     */
    fun loadBlockedAttempts() {
        viewModelScope.launch {
            _isLoading.value = true
            blockedAttemptDao.getAll().collect { attempts ->
                _blockedAttempts.value = attempts
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh blocked attempts
     */
    fun refresh() {
        loadBlockedAttempts()
    }
}

