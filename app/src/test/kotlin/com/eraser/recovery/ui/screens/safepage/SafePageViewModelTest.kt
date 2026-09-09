package com.eraser.recovery.ui.screens.safepage

import com.eraser.recovery.data.local.dao.BlockedAttemptDao
import com.eraser.recovery.data.local.dao.InterventionSessionDao
import com.eraser.recovery.data.local.entity.BlockedAttemptEntity
import com.eraser.recovery.data.local.entity.DifficultyLevel
import com.eraser.recovery.data.local.entity.FlashcardEntity
import com.eraser.recovery.data.local.entity.InterventionSessionEntity
import com.eraser.recovery.data.local.entity.MessageCategory
import com.eraser.recovery.data.local.entity.TaskType
import com.eraser.recovery.domain.flashcard.FlashcardService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Safe Page ViewModel Tests
 * 
 * Tests for SafePageViewModel functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafePageViewModelTest {
    
    private lateinit var viewModel: SafePageViewModel
    private lateinit var flashcardService: FlashcardService
    private lateinit var blockedAttemptDao: BlockedAttemptDao
    private lateinit var interventionSessionDao: InterventionSessionDao
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        flashcardService = mockk(relaxed = true)
        blockedAttemptDao = mockk(relaxed = true)
        interventionSessionDao = mockk(relaxed = true)
        
        // Default mocks
        coEvery { flashcardService.getRandomFlashcard() } returns null
        coEvery { flashcardService.startSession(any(), any(), any()) } returns mockk(relaxed = true)
        coEvery { blockedAttemptDao.insert(any()) } returns 1L
        coEvery { blockedAttemptDao.getById(any()) } returns null
        coEvery { interventionSessionDao.getCompletedCount() } returns 0
        coEvery { interventionSessionDao.getSkippedCount() } returns 0
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INITIALIZATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `viewModel is created successfully`() {
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        assertNotNull(viewModel)
    }
    
    @Test
    fun `initial state is correct`() {
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        
        assertEquals(null, viewModel.flashcard.value)
        assertEquals(null, viewModel.session.value)
        assertEquals(null, viewModel.blockedAttemptId.value)
        assertEquals(0, viewModel.completedCount.value)
        assertEquals(0, viewModel.skippedCount.value)
        assertEquals(0, viewModel.completionRate.value)
        assertFalse(viewModel.taskCompleted.value)
        assertTrue(viewModel.isLoading.value)
    }
    
    @Test
    fun `initialize loads flashcard and logs blocked attempt`() = runTest {
        val testFlashcard = FlashcardEntity(
            flashcardId = "test-1",
            frontMessage = "Test Front",
            backTask = "Test Back",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.GOAL_ORIENTED,
            isActive = true
        )
        val testSession = InterventionSessionEntity(
            id = 1L,
            sessionId = "session-1",
            flashcardId = "test-1",
            blockedDomain = "test.com",
            blockedUrl = "https://test.com",
            timestamp = LocalDateTime.now(),
            taskCompleted = false
        )
        
        coEvery { flashcardService.getRandomFlashcard() } returns testFlashcard
        coEvery { flashcardService.startSession(any(), any(), any()) } returns testSession
        coEvery { blockedAttemptDao.insert(any()) } returns 123L
        
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        viewModel.initialize("test.com", "https://test.com")
        advanceUntilIdle()
        
        assertEquals(testFlashcard, viewModel.flashcard.value)
        assertEquals(testSession, viewModel.session.value)
        assertEquals(123L, viewModel.blockedAttemptId.value)
        assertFalse(viewModel.isLoading.value)
        
        coVerify { flashcardService.getRandomFlashcard() }
        coVerify { flashcardService.startSession("test.com", "https://test.com", "test-1") }
        coVerify { blockedAttemptDao.insert(any()) }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `initialize loads statistics correctly`() = runTest {
        coEvery { interventionSessionDao.getCompletedCount() } returns 10
        coEvery { interventionSessionDao.getSkippedCount() } returns 5
        
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        viewModel.initialize("test.com", null)
        advanceUntilIdle()
        
        assertEquals(10, viewModel.completedCount.value)
        assertEquals(5, viewModel.skippedCount.value)
        assertEquals(66, viewModel.completionRate.value) // 10 / 15 = 66%
    }
    
    @Test
    fun `initialize handles zero statistics`() = runTest {
        coEvery { interventionSessionDao.getCompletedCount() } returns 0
        coEvery { interventionSessionDao.getSkippedCount() } returns 0
        
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        viewModel.initialize("test.com", null)
        advanceUntilIdle()
        
        assertEquals(0, viewModel.completedCount.value)
        assertEquals(0, viewModel.skippedCount.value)
        assertEquals(0, viewModel.completionRate.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TASK COMPLETION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `completeTask calls flashcardService and updates blocked attempt`() = runTest {
        val testSession = InterventionSessionEntity(
            id = 1L,
            sessionId = "session-1",
            flashcardId = "test-1",
            blockedDomain = "test.com",
            blockedUrl = null,
            timestamp = LocalDateTime.now(),
            taskCompleted = false
        )
        val testAttempt = BlockedAttemptEntity(
            id = 123L,
            domain = "test.com",
            url = "https://test.com",
            timestamp = LocalDateTime.now(),
            interventionCompleted = false
        )
        
        coEvery { flashcardService.getRandomFlashcard() } returns mockk(relaxed = true)
        coEvery { flashcardService.startSession(any(), any(), any()) } returns testSession
        coEvery { blockedAttemptDao.insert(any()) } returns 123L
        coEvery { blockedAttemptDao.getById(123L) } returns testAttempt
        coEvery { flashcardService.completeTask(any(), any()) } just Runs
        coEvery { blockedAttemptDao.update(any()) } just Runs
        
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        viewModel.initialize("test.com", null)
        advanceUntilIdle()
        
        viewModel.completeTask()
        advanceUntilIdle()
        
        assertTrue(viewModel.taskCompleted.value)
        coVerify { flashcardService.completeTask(1L, any()) }
        coVerify { blockedAttemptDao.update(match { it.interventionCompleted }) }
    }
    
    @Test
    fun `completeTask handles null session`() = runTest {
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        
        viewModel.completeTask()
        advanceUntilIdle()
        
        assertTrue(viewModel.taskCompleted.value)
        coVerify(exactly = 0) { flashcardService.completeTask(any(), any()) }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // TASK SKIP TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `skipTask calls flashcardService and updates blocked attempt`() = runTest {
        val testSession = InterventionSessionEntity(
            id = 1L,
            sessionId = "session-1",
            flashcardId = "test-1",
            blockedDomain = "test.com",
            blockedUrl = null,
            timestamp = LocalDateTime.now(),
            taskCompleted = false
        )
        val testAttempt = BlockedAttemptEntity(
            id = 123L,
            domain = "test.com",
            url = "https://test.com",
            timestamp = LocalDateTime.now(),
            interventionCompleted = false
        )
        
        coEvery { flashcardService.getRandomFlashcard() } returns mockk(relaxed = true)
        coEvery { flashcardService.startSession(any(), any(), any()) } returns testSession
        coEvery { blockedAttemptDao.insert(any()) } returns 123L
        coEvery { blockedAttemptDao.getById(123L) } returns testAttempt
        coEvery { flashcardService.skipTask(any()) } just Runs
        coEvery { blockedAttemptDao.update(any()) } just Runs
        
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        viewModel.initialize("test.com", null)
        advanceUntilIdle()
        
        viewModel.skipTask()
        advanceUntilIdle()
        
        assertTrue(viewModel.taskCompleted.value)
        coVerify { flashcardService.skipTask(1L) }
        coVerify { blockedAttemptDao.update(match { !it.interventionCompleted }) }
    }
    
    @Test
    fun `skipTask handles null session`() = runTest {
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        
        viewModel.skipTask()
        advanceUntilIdle()
        
        assertTrue(viewModel.taskCompleted.value)
        coVerify(exactly = 0) { flashcardService.skipTask(any()) }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EARLY EXIT TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `exitEarly calls flashcardService when task not completed`() = runTest {
        val testSession = InterventionSessionEntity(
            id = 1L,
            sessionId = "session-1",
            flashcardId = "test-1",
            blockedDomain = "test.com",
            blockedUrl = null,
            timestamp = LocalDateTime.now(),
            taskCompleted = false
        )
        
        coEvery { flashcardService.getRandomFlashcard() } returns mockk(relaxed = true)
        coEvery { flashcardService.startSession(any(), any(), any()) } returns testSession
        coEvery { flashcardService.exitSession(any()) } just Runs
        
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        viewModel.initialize("test.com", null)
        advanceUntilIdle()
        
        viewModel.exitEarly()
        advanceUntilIdle()
        
        coVerify { flashcardService.exitSession(1L) }
    }
    
    @Test
    fun `exitEarly does not call flashcardService when task completed`() = runTest {
        val testSession = InterventionSessionEntity(
            id = 1L,
            sessionId = "session-1",
            flashcardId = "test-1",
            blockedDomain = "test.com",
            blockedUrl = null,
            timestamp = LocalDateTime.now(),
            taskCompleted = false
        )
        
        coEvery { flashcardService.getRandomFlashcard() } returns mockk(relaxed = true)
        coEvery { flashcardService.startSession(any(), any(), any()) } returns testSession
        coEvery { flashcardService.completeTask(any(), any()) } just Runs
        coEvery { flashcardService.exitSession(any()) } just Runs
        
        viewModel = SafePageViewModel(flashcardService, blockedAttemptDao, interventionSessionDao)
        viewModel.initialize("test.com", null)
        advanceUntilIdle()
        
        viewModel.completeTask()
        advanceUntilIdle()
        
        viewModel.exitEarly()
        advanceUntilIdle()
        
        coVerify(exactly = 0) { flashcardService.exitSession(any()) }
    }
}

