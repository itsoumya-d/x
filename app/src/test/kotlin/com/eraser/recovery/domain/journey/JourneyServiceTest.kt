package com.eraser.recovery.domain.journey

import com.eraser.recovery.data.local.dao.AchievementDao
import com.eraser.recovery.data.local.dao.DailyLogDao
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.data.local.entity.AchievementEntity
import com.eraser.recovery.data.local.entity.DailyLogEntity
import com.eraser.recovery.data.local.entity.UserEntity
import com.eraser.recovery.domain.vpn.VpnManager
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
import kotlin.test.assertTrue

/**
 * JourneyService Test
 * 
 * Tests journey lifecycle, streak calculation, milestone tracking, and statistics.
 */
class JourneyServiceTest {
    
    private lateinit var userDao: UserDao
    private lateinit var dailyLogDao: DailyLogDao
    private lateinit var achievementDao: AchievementDao
    private lateinit var vpnManager: VpnManager
    private lateinit var journeyService: JourneyService
    
    @Before
    fun setup() {
        userDao = mock()
        dailyLogDao = mock()
        achievementDao = mock()
        vpnManager = mock()
        journeyService = JourneyService(userDao, dailyLogDao, achievementDao, vpnManager)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // JOURNEY LIFECYCLE TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `startJourney creates user if not exists`() = runBlocking<Unit> {
        // Given
        whenever(userDao.getUserOnce()).thenReturn(null)
        whenever(vpnManager.startVpn()).thenReturn(true)
        
        // When
        val result = journeyService.startJourney()
        
        // Then
        assertTrue(result.isSuccess)
        verify(userDao).insert(any())
        verify(userDao).updateJourneyStatus(eq(true), any(), eq(null), any())
        verify(vpnManager).startVpn()
    }
    
    @Test
    fun `startJourney fails if journey already active`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(isJourneyActive = true)
        whenever(userDao.getUserOnce()).thenReturn(user)
        
        // When
        val result = journeyService.startJourney()
        
        // Then
        assertTrue(result.isFailure)
        verify(vpnManager, never()).startVpn()
    }
    
    @Test
    fun `startJourney fails if VPN fails to start`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(isJourneyActive = false)
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(vpnManager.startVpn()).thenReturn(false)
        
        // When
        val result = journeyService.startJourney()
        
        // Then
        assertTrue(result.isFailure)
        verify(userDao, never()).updateJourneyStatus(any(), any(), any(), any())
    }
    
    @Test
    fun `stopJourney stops VPN and updates user`() = runBlocking<Unit> {
        // Given
        whenever(vpnManager.stopVpn()).thenReturn(Unit)
        
        // When
        val result = journeyService.stopJourney()
        
        // Then
        assertTrue(result.isSuccess)
        verify(vpnManager).stopVpn()
        verify(userDao).updateJourneyStatus(eq(false), eq(null), any(), any())
        verify(userDao).updateVpnStatus(eq(false), any())
    }
    
    @Test
    fun `toggleJourney starts journey if stopped`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(isJourneyActive = false)
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(vpnManager.startVpn()).thenReturn(true)
        
        // When
        val result = journeyService.toggleJourney()
        
        // Then
        assertTrue(result.isSuccess)
        verify(vpnManager).startVpn()
    }
    
    @Test
    fun `toggleJourney stops journey if started`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(isJourneyActive = true)
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(vpnManager.stopVpn()).thenReturn(Unit)
        
        // When
        val result = journeyService.toggleJourney()
        
        // Then
        assertTrue(result.isSuccess)
        verify(vpnManager).stopVpn()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STREAK CALCULATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `updateStreak calculates current streak correctly`() = runBlocking<Unit> {
        // Given
        val user = createTestUser()
        val today = LocalDate.now()
        val logs = listOf(
            createDailyLog(today.minusDays(2), wasClean = true),
            createDailyLog(today.minusDays(1), wasClean = true),
            createDailyLog(today, wasClean = true)
        )
        
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(dailyLogDao.getBetweenDates(any(), any())).thenReturn(logs)
        
        // When
        val result = journeyService.updateStreak()
        
        // Then
        assertTrue(result.isSuccess)
        verify(userDao).updateStreak(
            currentStreak = eq(3),
            longestStreak = eq(3),
            lastCheckInDate = any(),
            updatedAt = any()
        )
    }
    
    @Test
    fun `updateStreak handles broken streak`() = runBlocking<Unit> {
        // Given
        val user = createTestUser()
        val today = LocalDate.now()
        val logs = listOf(
            createDailyLog(today.minusDays(5), wasClean = true),
            createDailyLog(today.minusDays(4), wasClean = true),
            createDailyLog(today.minusDays(3), wasClean = false), // Broken streak
            createDailyLog(today.minusDays(1), wasClean = true),
            createDailyLog(today, wasClean = true)
        )
        
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(dailyLogDao.getBetweenDates(any(), any())).thenReturn(logs)
        
        // When
        val result = journeyService.updateStreak()
        
        // Then
        assertTrue(result.isSuccess)
        verify(userDao).updateStreak(
            currentStreak = eq(2), // Only last 2 days
            longestStreak = eq(2), // Longest was 2 days
            lastCheckInDate = any(),
            updatedAt = any()
        )
    }
    
    @Test
    fun `updateStreak calculates longest streak correctly`() = runBlocking<Unit> {
        // Given
        val user = createTestUser()
        val today = LocalDate.now()
        val logs = listOf(
            // First streak: 5 days
            createDailyLog(today.minusDays(10), wasClean = true),
            createDailyLog(today.minusDays(9), wasClean = true),
            createDailyLog(today.minusDays(8), wasClean = true),
            createDailyLog(today.minusDays(7), wasClean = true),
            createDailyLog(today.minusDays(6), wasClean = true),
            // Break
            createDailyLog(today.minusDays(5), wasClean = false),
            // Second streak: 2 days (current)
            createDailyLog(today.minusDays(1), wasClean = true),
            createDailyLog(today, wasClean = true)
        )
        
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(dailyLogDao.getBetweenDates(any(), any())).thenReturn(logs)
        
        // When
        val result = journeyService.updateStreak()
        
        // Then
        assertTrue(result.isSuccess)
        verify(userDao).updateStreak(
            currentStreak = eq(2),
            longestStreak = eq(5), // Longest was 5 days
            lastCheckInDate = any(),
            updatedAt = any()
        )
    }
    
    @Test
    fun `updateStreak resets to zero with no logs`() = runBlocking<Unit> {
        // Given
        val user = createTestUser()
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(dailyLogDao.getBetweenDates(any(), any())).thenReturn(emptyList())
        
        // When
        val result = journeyService.updateStreak()
        
        // Then
        assertTrue(result.isSuccess)
        verify(userDao).updateStreak(
            currentStreak = eq(0),
            longestStreak = eq(0),
            lastCheckInDate = eq(0),
            updatedAt = any()
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MILESTONE TRACKING TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `checkAndUnlockAchievements unlocks milestone achievements`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(currentStreak = 7)
        val achievement1 = createAchievement(milestone = 1, isUnlocked = false)
        val achievement3 = createAchievement(milestone = 3, isUnlocked = false)
        val achievement7 = createAchievement(milestone = 7, isUnlocked = false)
        
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(achievementDao.getByMilestone(1)).thenReturn(achievement1)
        whenever(achievementDao.getByMilestone(3)).thenReturn(achievement3)
        whenever(achievementDao.getByMilestone(7)).thenReturn(achievement7)
        whenever(achievementDao.getByMilestone(14)).thenReturn(null)
        
        // When
        val result = journeyService.checkAndUnlockAchievements()
        
        // Then
        assertTrue(result.isSuccess)
        val unlocked = result.getOrNull()
        assertNotNull(unlocked)
        assertEquals(3, unlocked.size)
        verify(achievementDao).unlock(eq(achievement1.achievementId), any())
        verify(achievementDao).unlock(eq(achievement3.achievementId), any())
        verify(achievementDao).unlock(eq(achievement7.achievementId), any())
    }
    
    @Test
    fun `checkAndUnlockAchievements does not unlock already unlocked achievements`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(currentStreak = 7)
        val achievement1 = createAchievement(milestone = 1, isUnlocked = true)
        val achievement3 = createAchievement(milestone = 3, isUnlocked = true)
        val achievement7 = createAchievement(milestone = 7, isUnlocked = false)
        
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(achievementDao.getByMilestone(1)).thenReturn(achievement1)
        whenever(achievementDao.getByMilestone(3)).thenReturn(achievement3)
        whenever(achievementDao.getByMilestone(7)).thenReturn(achievement7)
        
        // When
        val result = journeyService.checkAndUnlockAchievements()
        
        // Then
        assertTrue(result.isSuccess)
        val unlocked = result.getOrNull()
        assertNotNull(unlocked)
        assertEquals(1, unlocked.size) // Only achievement7
        verify(achievementDao, never()).unlock(eq(achievement1.achievementId), any())
        verify(achievementDao, never()).unlock(eq(achievement3.achievementId), any())
        verify(achievementDao).unlock(eq(achievement7.achievementId), any())
    }
    
    @Test
    fun `getNextMilestone returns correct next milestone`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(currentStreak = 5)
        whenever(userDao.getUserOnce()).thenReturn(user)
        
        // When
        val nextMilestone = journeyService.getNextMilestone()
        
        // Then
        assertEquals(7, nextMilestone)
    }
    
    @Test
    fun `getNextMilestone returns null when all milestones reached`() = runBlocking<Unit> {
        // Given
        val user = createTestUser(currentStreak = 365)
        whenever(userDao.getUserOnce()).thenReturn(user)
        
        // When
        val nextMilestone = journeyService.getNextMilestone()
        
        // Then
        assertEquals(null, nextMilestone)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun createTestUser(
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        isJourneyActive: Boolean = false
    ): UserEntity {
        return UserEntity(
            id = 1,
            startDate = LocalDate.now(),
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalDaysClean = 0,
            lastCheckInDate = null,
            isActive = true,
            isJourneyActive = isJourneyActive,
            journeyStartTime = if (isJourneyActive) LocalDateTime.now() else null,
            journeyStopTime = null,
            isVpnEnabled = isJourneyActive,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
    }
    
    private fun createDailyLog(date: LocalDate, wasClean: Boolean): DailyLogEntity {
        return DailyLogEntity(
            id = 0,
            date = date,
            wasClean = wasClean,
            notes = null,
            moodRating = null,
            triggers = null,
            createdAt = LocalDateTime.now()
        )
    }
    
    private fun createAchievement(milestone: Int, isUnlocked: Boolean): AchievementEntity {
        return AchievementEntity(
            id = 0,
            achievementId = "achievement_$milestone",
            title = "$milestone Day Badge",
            description = "Reached $milestone days",
            imageUrl = null,
            milestone = milestone,
            earnedDate = if (isUnlocked) LocalDateTime.now() else null,
            isUnlocked = isUnlocked,
            isSpecial = false,
            createdAt = LocalDateTime.now()
        )
    }
}

