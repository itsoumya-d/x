package com.eraser.recovery.domain.achievement

import com.eraser.recovery.data.local.dao.AchievementDao
import com.eraser.recovery.data.local.dao.InterventionSessionDao
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.data.local.entity.AchievementEntity
import com.eraser.recovery.data.local.entity.UserEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * AchievementService Test
 * 
 * Tests achievement unlocking, progress tracking, and special achievements.
 */
class AchievementServiceTest {
    
    private lateinit var achievementDao: AchievementDao
    private lateinit var userDao: UserDao
    private lateinit var interventionSessionDao: InterventionSessionDao
    private lateinit var achievementService: AchievementService
    
    @Before
    fun setup() {
        achievementDao = mock()
        userDao = mock()
        interventionSessionDao = mock()
        achievementService = AchievementService(
            achievementDao,
            userDao,
            interventionSessionDao
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT QUERIES TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `getAllAchievements returns flow of achievements`() = runBlocking {
        // Given
        val achievements = listOf(
            createAchievement("day_1", "First Step", 1),
            createAchievement("day_3", "Three Days", 3)
        )
        whenever(achievementDao.getAll()).thenReturn(flowOf(achievements))
        
        // When
        val result = achievementService.getAllAchievements()
        
        // Then
        assertNotNull(result)
        verify(achievementDao).getAll()
    }
    
    @Test
    fun `getUnlockedAchievements returns flow of unlocked achievements`() = runBlocking {
        // Given
        val achievements = listOf(
            createAchievement("day_1", "First Step", 1, isUnlocked = true)
        )
        whenever(achievementDao.getUnlocked()).thenReturn(flowOf(achievements))
        
        // When
        val result = achievementService.getUnlockedAchievements()
        
        // Then
        assertNotNull(result)
        verify(achievementDao).getUnlocked()
    }
    
    @Test
    fun `getLockedAchievements returns flow of locked achievements`() = runBlocking {
        // Given
        val achievements = listOf(
            createAchievement("day_3", "Three Days", 3, isUnlocked = false)
        )
        whenever(achievementDao.getLocked()).thenReturn(flowOf(achievements))
        
        // When
        val result = achievementService.getLockedAchievements()
        
        // Then
        assertNotNull(result)
        verify(achievementDao).getLocked()
    }
    
    @Test
    fun `getAchievementById returns achievement when found`() = runBlocking {
        // Given
        val achievement = createAchievement("day_1", "First Step", 1)
        whenever(achievementDao.getByAchievementId("day_1")).thenReturn(achievement)
        
        // When
        val result = achievementService.getAchievementById("day_1")
        
        // Then
        assertNotNull(result)
        assertEquals("day_1", result.achievementId)
        assertEquals("First Step", result.title)
    }
    
    @Test
    fun `getAchievementById returns null when not found`() = runBlocking {
        // Given
        whenever(achievementDao.getByAchievementId("invalid")).thenReturn(null)
        
        // When
        val result = achievementService.getAchievementById("invalid")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun `getNextToUnlock returns next locked achievement`() = runBlocking {
        // Given
        val achievement = createAchievement("day_3", "Three Days", 3)
        whenever(achievementDao.getNextToUnlock()).thenReturn(achievement)
        
        // When
        val result = achievementService.getNextToUnlock()
        
        // Then
        assertNotNull(result)
        assertEquals(3, result.milestone)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT UNLOCKING TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `checkAndUnlockAchievements unlocks achievements based on streak`() = runBlocking {
        // Given
        val user = createUser(currentStreak = 7)
        whenever(userDao.getUserOnce()).thenReturn(user)
        
        val day1 = createAchievement("day_1", "First Step", 1, isUnlocked = false)
        val day3 = createAchievement("day_3", "Three Days", 3, isUnlocked = false)
        val day7 = createAchievement("day_7", "One Week", 7, isUnlocked = false)
        
        whenever(achievementDao.getByMilestone(1)).thenReturn(day1)
        whenever(achievementDao.getByMilestone(3)).thenReturn(day3)
        whenever(achievementDao.getByMilestone(7)).thenReturn(day7)
        whenever(achievementDao.getByMilestone(14)).thenReturn(null)
        whenever(achievementDao.getByMilestone(30)).thenReturn(null)
        whenever(achievementDao.getByMilestone(60)).thenReturn(null)
        whenever(achievementDao.getByMilestone(90)).thenReturn(null)
        whenever(achievementDao.getByMilestone(180)).thenReturn(null)
        whenever(achievementDao.getByMilestone(365)).thenReturn(null)
        
        whenever(achievementDao.getByAchievementId("day_1")).thenReturn(day1.copy(isUnlocked = true))
        whenever(achievementDao.getByAchievementId("day_3")).thenReturn(day3.copy(isUnlocked = true))
        whenever(achievementDao.getByAchievementId("day_7")).thenReturn(day7.copy(isUnlocked = true))
        
        whenever(achievementDao.getUnlockedCount()).thenReturn(3)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        
        // When
        val result = achievementService.checkAndUnlockAchievements()
        
        // Then
        assertEquals(3, result.size)
        verify(achievementDao, times(3)).unlock(any(), any())
    }
    
    @Test
    fun `checkAndUnlockAchievements does not unlock already unlocked achievements`() = runBlocking {
        // Given
        val user = createUser(currentStreak = 3)
        whenever(userDao.getUserOnce()).thenReturn(user)
        
        val day1 = createAchievement("day_1", "First Step", 1, isUnlocked = true)
        val day3 = createAchievement("day_3", "Three Days", 3, isUnlocked = false)
        
        whenever(achievementDao.getByMilestone(1)).thenReturn(day1)
        whenever(achievementDao.getByMilestone(3)).thenReturn(day3)
        whenever(achievementDao.getByMilestone(7)).thenReturn(null)
        whenever(achievementDao.getByMilestone(14)).thenReturn(null)
        whenever(achievementDao.getByMilestone(30)).thenReturn(null)
        whenever(achievementDao.getByMilestone(60)).thenReturn(null)
        whenever(achievementDao.getByMilestone(90)).thenReturn(null)
        whenever(achievementDao.getByMilestone(180)).thenReturn(null)
        whenever(achievementDao.getByMilestone(365)).thenReturn(null)
        
        whenever(achievementDao.getByAchievementId("day_3")).thenReturn(day3.copy(isUnlocked = true))
        
        whenever(achievementDao.getUnlockedCount()).thenReturn(2)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        
        // When
        val result = achievementService.checkAndUnlockAchievements()
        
        // Then
        assertEquals(1, result.size) // Only day_3 should be unlocked
        verify(achievementDao, times(1)).unlock(eq("day_3"), any())
    }
    
    @Test
    fun `checkAndUnlockAchievements returns empty list when no user`() = runBlocking {
        // Given
        whenever(userDao.getUserOnce()).thenReturn(null)
        
        // When
        val result = achievementService.checkAndUnlockAchievements()
        
        // Then
        assertTrue(result.isEmpty())
        verify(achievementDao, never()).unlock(any(), any())
    }
    
    @Test
    fun `unlockAchievement unlocks specific achievement`() = runBlocking {
        // Given
        val achievement = createAchievement("day_1", "First Step", 1, isUnlocked = false)
        whenever(achievementDao.getByAchievementId("day_1")).thenReturn(
            achievement,
            achievement.copy(isUnlocked = true)
        )
        whenever(achievementDao.getUnlockedCount()).thenReturn(1)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        
        // When
        val result = achievementService.unlockAchievement("day_1")
        
        // Then
        assertNotNull(result)
        assertTrue(result.isUnlocked)
        verify(achievementDao).unlock(eq("day_1"), any())
    }
    
    @Test
    fun `unlockAchievement returns null when achievement not found`() = runBlocking {
        // Given
        whenever(achievementDao.getByAchievementId("invalid")).thenReturn(null)
        
        // When
        val result = achievementService.unlockAchievement("invalid")
        
        // Then
        assertNull(result)
        verify(achievementDao, never()).unlock(any(), any())
    }
    
    @Test
    fun `unlockAchievement returns null when already unlocked`() = runBlocking {
        // Given
        val achievement = createAchievement("day_1", "First Step", 1, isUnlocked = true)
        whenever(achievementDao.getByAchievementId("day_1")).thenReturn(achievement)
        
        // When
        val result = achievementService.unlockAchievement("day_1")
        
        // Then
        assertNull(result)
        verify(achievementDao, never()).unlock(any(), any())
    }
    
    @Test
    fun `unlockByMilestone unlocks achievement by milestone`() = runBlocking {
        // Given
        val achievement = createAchievement("day_7", "One Week", 7, isUnlocked = false)
        whenever(achievementDao.getByMilestone(7)).thenReturn(achievement)
        whenever(achievementDao.getByAchievementId("day_7")).thenReturn(
            achievement,
            achievement.copy(isUnlocked = true)
        )
        whenever(achievementDao.getUnlockedCount()).thenReturn(1)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        
        // When
        val result = achievementService.unlockByMilestone(7)
        
        // Then
        assertNotNull(result)
        assertTrue(result.isUnlocked)
        verify(achievementDao).unlock(eq("day_7"), any())
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // SPECIAL ACHIEVEMENTS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `checkFirstIntervention unlocks when completed count is 1 or more`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getCompletedCount()).thenReturn(1)
        val achievement = createAchievement("first_intervention", "First Intervention", 0, isSpecial = true)
        whenever(achievementDao.getByAchievementId("first_intervention")).thenReturn(
            achievement,
            achievement.copy(isUnlocked = true)
        )
        whenever(achievementDao.getUnlockedCount()).thenReturn(1)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        
        // When
        val result = achievementService.checkFirstIntervention()
        
        // Then
        assertNotNull(result)
        verify(achievementDao).unlock(eq("first_intervention"), any())
    }
    
    @Test
    fun `checkFirstIntervention returns null when no interventions completed`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getCompletedCount()).thenReturn(0)
        
        // When
        val result = achievementService.checkFirstIntervention()
        
        // Then
        assertNull(result)
        verify(achievementDao, never()).unlock(any(), any())
    }
    
    @Test
    fun `checkStreakKeeper unlocks when streak is 30 or more`() = runBlocking {
        // Given
        val user = createUser(currentStreak = 30)
        whenever(userDao.getUserOnce()).thenReturn(user)
        val achievement = createAchievement("streak_keeper", "Streak Keeper", 30, isSpecial = true)
        whenever(achievementDao.getByAchievementId("streak_keeper")).thenReturn(
            achievement,
            achievement.copy(isUnlocked = true)
        )
        whenever(achievementDao.getUnlockedCount()).thenReturn(1)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        
        // When
        val result = achievementService.checkStreakKeeper()
        
        // Then
        assertNotNull(result)
        verify(achievementDao).unlock(eq("streak_keeper"), any())
    }
    
    @Test
    fun `checkStreakKeeper returns null when streak is less than 30`() = runBlocking {
        // Given
        val user = createUser(currentStreak = 15)
        whenever(userDao.getUserOnce()).thenReturn(user)
        
        // When
        val result = achievementService.checkStreakKeeper()
        
        // Then
        assertNull(result)
        verify(achievementDao, never()).unlock(any(), any())
    }
    
    @Test
    fun `checkSpecialAchievements checks all special achievements`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getCompletedCount()).thenReturn(1)
        val user = createUser(currentStreak = 30)
        whenever(userDao.getUserOnce()).thenReturn(user)
        
        val firstIntervention = createAchievement("first_intervention", "First Intervention", 0, isSpecial = true)
        val streakKeeper = createAchievement("streak_keeper", "Streak Keeper", 30, isSpecial = true)
        
        whenever(achievementDao.getByAchievementId("first_intervention")).thenReturn(
            firstIntervention,
            firstIntervention.copy(isUnlocked = true)
        )
        whenever(achievementDao.getByAchievementId("streak_keeper")).thenReturn(
            streakKeeper,
            streakKeeper.copy(isUnlocked = true)
        )
        whenever(achievementDao.getUnlockedCount()).thenReturn(2)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        
        // When
        val result = achievementService.checkSpecialAchievements()
        
        // Then
        assertEquals(2, result.size)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT PROGRESS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `getAchievementProgress returns progress statistics`() = runBlocking {
        // Given
        whenever(achievementDao.getUnlockedCount()).thenReturn(3)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        val nextAchievement = createAchievement("day_7", "One Week", 7)
        whenever(achievementDao.getNextToUnlock()).thenReturn(nextAchievement)
        val user = createUser(currentStreak = 5)
        whenever(userDao.getUserOnce()).thenReturn(user)

        // When
        val result = achievementService.getAchievementProgress()

        // Then
        assertEquals(3, result["unlockedCount"])
        assertEquals(11, result["totalCount"])
        assertEquals(27.27, (result["progress"] as Double), 0.01)
        assertEquals("One Week", result["nextAchievement"])
        assertEquals(7, result["nextMilestone"])
        assertEquals(2, result["daysUntilNext"]) // 7 - 5 = 2
        assertEquals(5, result["currentStreak"])
    }

    @Test
    fun `getAchievementProgress returns zero days until next when milestone reached`() = runBlocking {
        // Given
        whenever(achievementDao.getUnlockedCount()).thenReturn(3)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        val nextAchievement = createAchievement("day_7", "One Week", 7)
        whenever(achievementDao.getNextToUnlock()).thenReturn(nextAchievement)
        val user = createUser(currentStreak = 10)
        whenever(userDao.getUserOnce()).thenReturn(user)

        // When
        val result = achievementService.getAchievementProgress()

        // Then
        assertEquals(0, result["daysUntilNext"]) // Already past milestone
    }

    @Test
    fun `getUnlockedCount returns correct count`() = runBlocking {
        // Given
        whenever(achievementDao.getUnlockedCount()).thenReturn(5)

        // When
        val result = achievementService.getUnlockedCount()

        // Then
        assertEquals(5, result)
    }

    @Test
    fun `getTotalCount returns correct count`() = runBlocking {
        // Given
        whenever(achievementDao.getTotalCount()).thenReturn(11)

        // When
        val result = achievementService.getTotalCount()

        // Then
        assertEquals(11, result)
    }

    @Test
    fun `getProgressPercentage calculates correctly`() = runBlocking {
        // Given
        whenever(achievementDao.getUnlockedCount()).thenReturn(5)
        whenever(achievementDao.getTotalCount()).thenReturn(10)

        // When
        val result = achievementService.getProgressPercentage()

        // Then
        assertEquals(50.0, result)
    }

    @Test
    fun `getProgressPercentage returns zero when no achievements`() = runBlocking {
        // Given
        whenever(achievementDao.getUnlockedCount()).thenReturn(0)
        whenever(achievementDao.getTotalCount()).thenReturn(0)

        // When
        val result = achievementService.getProgressPercentage()

        // Then
        assertEquals(0.0, result)
    }

    @Test
    fun `isAchievementUnlocked returns true when unlocked`() = runBlocking {
        // Given
        val achievement = createAchievement("day_1", "First Step", 1, isUnlocked = true)
        whenever(achievementDao.getByAchievementId("day_1")).thenReturn(achievement)

        // When
        val result = achievementService.isAchievementUnlocked("day_1")

        // Then
        assertTrue(result)
    }

    @Test
    fun `isAchievementUnlocked returns false when locked`() = runBlocking {
        // Given
        val achievement = createAchievement("day_1", "First Step", 1, isUnlocked = false)
        whenever(achievementDao.getByAchievementId("day_1")).thenReturn(achievement)

        // When
        val result = achievementService.isAchievementUnlocked("day_1")

        // Then
        assertFalse(result)
    }

    @Test
    fun `isAchievementUnlocked returns false when not found`() = runBlocking {
        // Given
        whenever(achievementDao.getByAchievementId("invalid")).thenReturn(null)

        // When
        val result = achievementService.isAchievementUnlocked("invalid")

        // Then
        assertFalse(result)
    }

    @Test
    fun `getAchievementSummary returns comprehensive summary`() = runBlocking {
        // Given
        whenever(achievementDao.getUnlockedCount()).thenReturn(3)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        val nextAchievement = createAchievement("day_7", "One Week", 7)
        whenever(achievementDao.getNextToUnlock()).thenReturn(nextAchievement)
        val user = createUser(currentStreak = 5)
        whenever(userDao.getUserOnce()).thenReturn(user)

        // When
        val result = achievementService.getAchievementSummary()

        // Then
        assertEquals(3, result["unlockedCount"])
        assertEquals(11, result["totalCount"])
        assertEquals("One Week", result["nextAchievement"])
        assertEquals(7, result["nextMilestone"])
        assertEquals("Test description", result["nextDescription"])
        assertEquals(2, result["daysUntilNext"])
        assertEquals(true, result["hasNext"])
    }

    @Test
    fun `getAchievementSummary shows all unlocked when no next achievement`() = runBlocking {
        // Given
        whenever(achievementDao.getUnlockedCount()).thenReturn(11)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        whenever(achievementDao.getNextToUnlock()).thenReturn(null)
        val user = createUser(currentStreak = 365)
        whenever(userDao.getUserOnce()).thenReturn(user)

        // When
        val result = achievementService.getAchievementSummary()

        // Then
        assertEquals("All unlocked!", result["nextAchievement"])
        assertEquals(0, result["nextMilestone"])
        assertEquals("You've unlocked all achievements!", result["nextDescription"])
        assertEquals(false, result["hasNext"])
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun createAchievement(
        achievementId: String,
        title: String,
        milestone: Int,
        isUnlocked: Boolean = false,
        isSpecial: Boolean = false
    ): AchievementEntity {
        return AchievementEntity(
            id = 0,
            achievementId = achievementId,
            title = title,
            description = "Test description",
            imageUrl = null,
            milestone = milestone,
            earnedDate = if (isUnlocked) LocalDateTime.now() else null,
            isUnlocked = isUnlocked,
            isSpecial = isSpecial,
            createdAt = LocalDateTime.now()
        )
    }
    
    private fun createUser(currentStreak: Int = 0): UserEntity {
        return UserEntity(
            id = 1,
            startDate = LocalDate.now(),
            currentStreak = currentStreak,
            longestStreak = currentStreak,
            totalDaysClean = currentStreak,
            lastCheckInDate = null,
            isActive = true,
            isJourneyActive = false,
            journeyStartTime = null,
            journeyStopTime = null,
            isVpnEnabled = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
}

