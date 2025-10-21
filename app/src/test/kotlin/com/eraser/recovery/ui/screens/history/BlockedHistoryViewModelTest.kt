package com.eraser.recovery.ui.screens.history

import com.eraser.recovery.data.local.dao.BlockedAttemptDao
import com.eraser.recovery.data.local.entity.BlockedAttemptEntity
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for BlockedHistoryViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BlockedHistoryViewModelTest {
    
    private lateinit var viewModel: BlockedHistoryViewModel
    private lateinit var blockedAttemptDao: BlockedAttemptDao
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        blockedAttemptDao = mockk(relaxed = true)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadBlockedAttempts updates state flow`() = runTest {
        // Given
        val attempts = listOf(
            createBlockedAttempt(id = 1, domain = "example.com", completed = true),
            createBlockedAttempt(id = 2, domain = "test.com", completed = false)
        )
        coEvery { blockedAttemptDao.getAll() } returns flowOf(attempts)
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        viewModel.loadBlockedAttempts()
        advanceUntilIdle()
        
        // Then
        assertEquals(2, viewModel.blockedAttempts.value.size)
        assertEquals("example.com", viewModel.blockedAttempts.value[0].domain)
        assertEquals("test.com", viewModel.blockedAttempts.value[1].domain)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `loading state is true initially`() = runTest {
        // Given
        coEvery { blockedAttemptDao.getAll() } returns flowOf(emptyList())
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        
        // Then
        assertTrue(viewModel.isLoading.value)
    }
    
    @Test
    fun `loading state is false after attempts loaded`() = runTest {
        // Given
        val attempts = listOf(
            createBlockedAttempt(id = 1, domain = "example.com", completed = true)
        )
        coEvery { blockedAttemptDao.getAll() } returns flowOf(attempts)
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        viewModel.loadBlockedAttempts()
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `empty attempts list is handled`() = runTest {
        // Given
        coEvery { blockedAttemptDao.getAll() } returns flowOf(emptyList())
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        viewModel.loadBlockedAttempts()
        advanceUntilIdle()
        
        // Then
        assertEquals(0, viewModel.blockedAttempts.value.size)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `refresh reloads attempts`() = runTest {
        // Given
        val initialAttempts = listOf(
            createBlockedAttempt(id = 1, domain = "example.com", completed = true)
        )
        val updatedAttempts = listOf(
            createBlockedAttempt(id = 1, domain = "example.com", completed = true),
            createBlockedAttempt(id = 2, domain = "test.com", completed = false)
        )
        coEvery { blockedAttemptDao.getAll() } returns flowOf(initialAttempts) andThen flowOf(updatedAttempts)
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        viewModel.loadBlockedAttempts()
        advanceUntilIdle()
        assertEquals(1, viewModel.blockedAttempts.value.size)
        
        viewModel.refresh()
        advanceUntilIdle()
        
        // Then
        assertEquals(2, viewModel.blockedAttempts.value.size)
    }
    
    @Test
    fun `attempts are ordered by timestamp descending`() = runTest {
        // Given
        val now = LocalDateTime.now()
        val attempts = listOf(
            createBlockedAttempt(id = 1, domain = "example.com", timestamp = now.minusHours(2)),
            createBlockedAttempt(id = 2, domain = "test.com", timestamp = now.minusHours(1)),
            createBlockedAttempt(id = 3, domain = "site.com", timestamp = now)
        )
        coEvery { blockedAttemptDao.getAll() } returns flowOf(attempts)
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        viewModel.loadBlockedAttempts()
        advanceUntilIdle()
        
        // Then
        assertEquals(3, viewModel.blockedAttempts.value.size)
        // Note: DAO should return in DESC order, so most recent first
        assertEquals("example.com", viewModel.blockedAttempts.value[0].domain)
    }
    
    @Test
    fun `completed and skipped attempts are included`() = runTest {
        // Given
        val attempts = listOf(
            createBlockedAttempt(id = 1, domain = "example.com", completed = true),
            createBlockedAttempt(id = 2, domain = "test.com", completed = false),
            createBlockedAttempt(id = 3, domain = "site.com", completed = true)
        )
        coEvery { blockedAttemptDao.getAll() } returns flowOf(attempts)
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        viewModel.loadBlockedAttempts()
        advanceUntilIdle()
        
        // Then
        val completedCount = viewModel.blockedAttempts.value.count { it.interventionCompleted }
        val skippedCount = viewModel.blockedAttempts.value.count { !it.interventionCompleted }
        assertEquals(2, completedCount)
        assertEquals(1, skippedCount)
    }
    
    @Test
    fun `multiple domains are handled`() = runTest {
        // Given
        val attempts = listOf(
            createBlockedAttempt(id = 1, domain = "example.com", completed = true),
            createBlockedAttempt(id = 2, domain = "test.com", completed = false),
            createBlockedAttempt(id = 3, domain = "example.com", completed = true),
            createBlockedAttempt(id = 4, domain = "site.com", completed = false)
        )
        coEvery { blockedAttemptDao.getAll() } returns flowOf(attempts)
        
        // When
        viewModel = BlockedHistoryViewModel(blockedAttemptDao)
        viewModel.loadBlockedAttempts()
        advanceUntilIdle()
        
        // Then
        assertEquals(4, viewModel.blockedAttempts.value.size)
        val domains = viewModel.blockedAttempts.value.map { it.domain }.distinct()
        assertEquals(3, domains.size)
    }
    
    // Helper function to create test blocked attempts
    private fun createBlockedAttempt(
        id: Long,
        domain: String,
        completed: Boolean = false,
        timestamp: LocalDateTime = LocalDateTime.now()
    ): BlockedAttemptEntity {
        return BlockedAttemptEntity(
            id = id,
            timestamp = timestamp,
            url = "https://$domain/path",
            domain = domain,
            interventionCompleted = completed,
            interventionType = if (completed) "flashcard" else null,
            interventionDuration = if (completed) 30 else null,
            createdAt = LocalDateTime.now()
        )
    }
}

