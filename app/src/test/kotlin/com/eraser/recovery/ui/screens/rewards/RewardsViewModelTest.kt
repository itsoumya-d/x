package com.eraser.recovery.ui.screens.rewards

import com.eraser.recovery.data.local.entity.AchievementEntity
import com.eraser.recovery.domain.achievement.AchievementService
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
 * Unit tests for RewardsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RewardsViewModelTest {
    
    private lateinit var viewModel: RewardsViewModel
    private lateinit var achievementService: AchievementService
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        achievementService = mockk(relaxed = true)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `init loads achievements`() = runTest {
        // Given
        val achievements = listOf(
            createAchievement(id = 1, title = "First Day", milestone = 1, isUnlocked = true),
            createAchievement(id = 2, title = "Three Days", milestone = 3, isUnlocked = false)
        )
        coEvery { achievementService.getAllAchievements() } returns flowOf(achievements)
        
        // When
        viewModel = RewardsViewModel(achievementService)
        advanceUntilIdle()
        
        // Then
        assertEquals(2, viewModel.achievements.value.size)
        assertEquals("First Day", viewModel.achievements.value[0].title)
        assertEquals("Three Days", viewModel.achievements.value[1].title)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `loading state is true initially`() = runTest {
        // Given
        coEvery { achievementService.getAllAchievements() } returns flowOf(emptyList())
        
        // When
        viewModel = RewardsViewModel(achievementService)
        
        // Then
        assertTrue(viewModel.isLoading.value)
    }
    
    @Test
    fun `loading state is false after achievements loaded`() = runTest {
        // Given
        val achievements = listOf(
            createAchievement(id = 1, title = "First Day", milestone = 1, isUnlocked = true)
        )
        coEvery { achievementService.getAllAchievements() } returns flowOf(achievements)
        
        // When
        viewModel = RewardsViewModel(achievementService)
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `achievements are sorted by milestone`() = runTest {
        // Given
        val achievements = listOf(
            createAchievement(id = 1, title = "First Day", milestone = 1, isUnlocked = true),
            createAchievement(id = 2, title = "Three Days", milestone = 3, isUnlocked = false),
            createAchievement(id = 3, title = "Seven Days", milestone = 7, isUnlocked = false)
        )
        coEvery { achievementService.getAllAchievements() } returns flowOf(achievements)
        
        // When
        viewModel = RewardsViewModel(achievementService)
        advanceUntilIdle()
        
        // Then
        assertEquals(3, viewModel.achievements.value.size)
        assertEquals(1, viewModel.achievements.value[0].milestone)
        assertEquals(3, viewModel.achievements.value[1].milestone)
        assertEquals(7, viewModel.achievements.value[2].milestone)
    }
    
    @Test
    fun `refresh reloads achievements`() = runTest {
        // Given
        val initialAchievements = listOf(
            createAchievement(id = 1, title = "First Day", milestone = 1, isUnlocked = true)
        )
        val updatedAchievements = listOf(
            createAchievement(id = 1, title = "First Day", milestone = 1, isUnlocked = true),
            createAchievement(id = 2, title = "Three Days", milestone = 3, isUnlocked = true)
        )
        coEvery { achievementService.getAllAchievements() } returns flowOf(initialAchievements) andThen flowOf(updatedAchievements)
        
        // When
        viewModel = RewardsViewModel(achievementService)
        advanceUntilIdle()
        assertEquals(1, viewModel.achievements.value.size)
        
        viewModel.refresh()
        advanceUntilIdle()
        
        // Then
        assertEquals(2, viewModel.achievements.value.size)
    }
    
    @Test
    fun `empty achievements list is handled`() = runTest {
        // Given
        coEvery { achievementService.getAllAchievements() } returns flowOf(emptyList())
        
        // When
        viewModel = RewardsViewModel(achievementService)
        advanceUntilIdle()
        
        // Then
        assertEquals(0, viewModel.achievements.value.size)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `unlocked achievements are included`() = runTest {
        // Given
        val achievements = listOf(
            createAchievement(id = 1, title = "First Day", milestone = 1, isUnlocked = true),
            createAchievement(id = 2, title = "Three Days", milestone = 3, isUnlocked = true),
            createAchievement(id = 3, title = "Seven Days", milestone = 7, isUnlocked = false)
        )
        coEvery { achievementService.getAllAchievements() } returns flowOf(achievements)
        
        // When
        viewModel = RewardsViewModel(achievementService)
        advanceUntilIdle()
        
        // Then
        val unlockedCount = viewModel.achievements.value.count { it.isUnlocked }
        assertEquals(2, unlockedCount)
    }
    
    @Test
    fun `locked achievements are included`() = runTest {
        // Given
        val achievements = listOf(
            createAchievement(id = 1, title = "First Day", milestone = 1, isUnlocked = true),
            createAchievement(id = 2, title = "Three Days", milestone = 3, isUnlocked = false),
            createAchievement(id = 3, title = "Seven Days", milestone = 7, isUnlocked = false)
        )
        coEvery { achievementService.getAllAchievements() } returns flowOf(achievements)
        
        // When
        viewModel = RewardsViewModel(achievementService)
        advanceUntilIdle()
        
        // Then
        val lockedCount = viewModel.achievements.value.count { !it.isUnlocked }
        assertEquals(2, lockedCount)
    }
    
    // Helper function to create test achievements
    private fun createAchievement(
        id: Long,
        title: String,
        milestone: Int,
        isUnlocked: Boolean
    ): AchievementEntity {
        return AchievementEntity(
            id = id,
            achievementId = "achievement_$id",
            title = title,
            description = "Test description",
            milestone = milestone,
            isUnlocked = isUnlocked,
            earnedDate = if (isUnlocked) LocalDateTime.now() else null,
            createdAt = LocalDateTime.now()
        )
    }
}

