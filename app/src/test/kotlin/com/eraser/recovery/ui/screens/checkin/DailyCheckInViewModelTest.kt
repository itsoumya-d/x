package com.eraser.recovery.ui.screens.checkin

import com.eraser.recovery.data.local.dao.DailyLogDao
import com.eraser.recovery.data.local.entity.DailyLogEntity
import com.eraser.recovery.domain.achievement.AchievementService
import com.eraser.recovery.domain.journey.JourneyService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for DailyCheckInViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DailyCheckInViewModelTest {
    
    private lateinit var viewModel: DailyCheckInViewModel
    private lateinit var dailyLogDao: DailyLogDao
    private lateinit var journeyService: JourneyService
    private lateinit var achievementService: AchievementService
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dailyLogDao = mockk(relaxed = true)
        journeyService = mockk(relaxed = true)
        achievementService = mockk(relaxed = true)
        
        coEvery { dailyLogDao.getByDate(any()) } returns null
        coEvery { dailyLogDao.insert(any()) } returns 1L
        coEvery { journeyService.checkInToday(any(), any()) } returns Result.success(Unit)
        coEvery { journeyService.recordRelapse(any(), any()) } returns Result.success(Unit)
        coEvery { achievementService.checkAndUnlockAchievements() } returns emptyList()
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state is clean day`() = runTest {
        // When
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.wasClean.value)
        assertNull(viewModel.moodRating.value)
        assertEquals("", viewModel.notes.value)
        assertFalse(viewModel.isSubmitting.value)
        assertFalse(viewModel.hasCheckedInToday.value)
        assertFalse(viewModel.submitSuccess.value)
    }
    
    @Test
    fun `setWasClean updates state`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        
        // When
        viewModel.setWasClean(false)
        
        // Then
        assertFalse(viewModel.wasClean.value)
    }
    
    @Test
    fun `setMoodRating updates state`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        
        // When
        viewModel.setMoodRating(4)
        
        // Then
        assertEquals(4, viewModel.moodRating.value)
    }
    
    @Test
    fun `setNotes updates state`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        
        // When
        viewModel.setNotes("Test notes")
        
        // Then
        assertEquals("Test notes", viewModel.notes.value)
    }
    
    @Test
    fun `submitCheckIn saves daily log for clean day`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        viewModel.setWasClean(true)
        viewModel.setMoodRating(5)
        viewModel.setNotes("Great day!")
        
        // When
        viewModel.submitCheckIn()
        advanceUntilIdle()
        
        // Then
        coVerify { dailyLogDao.insert(match { it.wasClean && it.moodRating == 5 }) }
        coVerify { journeyService.checkInToday(any(), any()) }
        coVerify { achievementService.checkAndUnlockAchievements() }
        assertTrue(viewModel.submitSuccess.value)
    }
    
    @Test
    fun `submitCheckIn saves daily log for relapse`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        viewModel.setWasClean(false)
        viewModel.setMoodRating(2)
        viewModel.setNotes("Struggled today")
        
        // When
        viewModel.submitCheckIn()
        advanceUntilIdle()
        
        // Then
        coVerify { dailyLogDao.insert(match { !it.wasClean && it.moodRating == 2 }) }
        coVerify { journeyService.recordRelapse(any(), any()) }
        coVerify { achievementService.checkAndUnlockAchievements() }
        assertTrue(viewModel.submitSuccess.value)
    }
    
    @Test
    fun `submitCheckIn trims notes`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        viewModel.setNotes("  Test notes  ")
        
        // When
        viewModel.submitCheckIn()
        advanceUntilIdle()
        
        // Then
        coVerify { dailyLogDao.insert(match { it.notes == "Test notes" }) }
    }
    
    @Test
    fun `submitCheckIn saves null for blank notes`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        viewModel.setNotes("   ")
        
        // When
        viewModel.submitCheckIn()
        advanceUntilIdle()
        
        // Then
        coVerify { dailyLogDao.insert(match { it.notes == null }) }
    }
    
    @Test
    fun `hasCheckedInToday is true when log exists`() = runTest {
        // Given
        val today = LocalDate.now()
        val existingLog = DailyLogEntity(
            id = 1,
            date = today,
            wasClean = false,
            notes = "Previous notes",
            moodRating = 3,
            triggers = null,
            blockedAttempts = null,
            createdAt = LocalDateTime.now()
        )
        coEvery { dailyLogDao.getByDate(today.toEpochDay()) } returns existingLog
        
        // When
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        
        // Then
        assertTrue(viewModel.hasCheckedInToday.value)
        assertFalse(viewModel.wasClean.value)
        assertEquals(3, viewModel.moodRating.value)
        assertEquals("Previous notes", viewModel.notes.value)
    }
    
    @Test
    fun `hasCheckedInToday is false when no log exists`() = runTest {
        // Given
        coEvery { dailyLogDao.getByDate(any()) } returns null
        
        // When
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.hasCheckedInToday.value)
    }
    
    @Test
    fun `isSubmitting is true during submission`() = runTest {
        // Given: make the insert suspend so the in-flight state is observable
        coEvery { dailyLogDao.insert(any()) } coAnswers {
            kotlinx.coroutines.delay(1_000)
            1L
        }
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()

        // When
        viewModel.submitCheckIn()
        runCurrent()

        // Then (before advanceUntilIdle)
        assertTrue(viewModel.isSubmitting.value)

        // After completion
        advanceUntilIdle()
        assertFalse(viewModel.isSubmitting.value)
    }
    
    @Test
    fun `resetSubmitSuccess resets state`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        viewModel.submitCheckIn()
        advanceUntilIdle()
        assertTrue(viewModel.submitSuccess.value)
        
        // When
        viewModel.resetSubmitSuccess()
        
        // Then
        assertFalse(viewModel.submitSuccess.value)
    }
    
    @Test
    fun `submitCheckIn with null mood rating`() = runTest {
        // Given
        viewModel = DailyCheckInViewModel(dailyLogDao, journeyService, achievementService)
        advanceUntilIdle()
        viewModel.setWasClean(true)
        // Don't set mood rating (leave as null)
        
        // When
        viewModel.submitCheckIn()
        advanceUntilIdle()
        
        // Then
        coVerify { dailyLogDao.insert(match { it.moodRating == null }) }
        assertTrue(viewModel.submitSuccess.value)
    }
}

