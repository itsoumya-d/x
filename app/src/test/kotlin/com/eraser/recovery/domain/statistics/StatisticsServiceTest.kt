package com.eraser.recovery.domain.statistics

import com.eraser.recovery.data.local.dao.*
import com.eraser.recovery.data.local.entity.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * StatisticsService Test
 * 
 * Tests analytics, chart data generation, and trend analysis.
 */
class StatisticsServiceTest {
    
    private lateinit var userDao: UserDao
    private lateinit var blockedAttemptDao: BlockedAttemptDao
    private lateinit var interventionSessionDao: InterventionSessionDao
    private lateinit var dailyLogDao: DailyLogDao
    private lateinit var achievementDao: AchievementDao
    private lateinit var flashcardDao: FlashcardDao
    private lateinit var statisticsService: StatisticsService
    
    @Before
    fun setup() {
        userDao = mock()
        blockedAttemptDao = mock()
        interventionSessionDao = mock()
        dailyLogDao = mock()
        achievementDao = mock()
        flashcardDao = mock()
        statisticsService = StatisticsService(
            userDao,
            blockedAttemptDao,
            interventionSessionDao,
            dailyLogDao,
            achievementDao,
            flashcardDao
        )
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DASHBOARD STATISTICS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `getDashboardStatistics returns comprehensive metrics`() = runBlocking {
        // Given
        val user = createTestUser(currentStreak = 5, longestStreak = 10, totalDaysClean = 15)
        whenever(userDao.getUserOnce()).thenReturn(user)
        whenever(blockedAttemptDao.getCount()).thenReturn(100)
        whenever(blockedAttemptDao.getCountBetween(any(), any())).thenReturn(10)
        whenever(interventionSessionDao.getCount()).thenReturn(50)
        whenever(interventionSessionDao.getCompletedCount()).thenReturn(40)
        whenever(interventionSessionDao.getSkippedCount()).thenReturn(10)
        whenever(interventionSessionDao.getAverageCompletionTime()).thenReturn(120.0)
        whenever(achievementDao.getUnlockedCount()).thenReturn(3)
        whenever(achievementDao.getTotalCount()).thenReturn(11)
        whenever(dailyLogDao.getCount()).thenReturn(15)
        whenever(dailyLogDao.getAverageMoodRating()).thenReturn(4.5)
        
        // When
        val stats = statisticsService.getDashboardStatistics()
        
        // Then
        assertNotNull(stats)
        assertEquals(100, stats["totalBlockedAttempts"])
        assertEquals(50, stats["totalInterventions"])
        assertEquals(40, stats["completedInterventions"])
        assertEquals(80.0, stats["interventionCompletionRate"])
        assertEquals(5, stats["currentStreak"])
        assertEquals(10, stats["longestStreak"])
        assertEquals(15, stats["totalCleanDays"])
        assertEquals(3, stats["unlockedAchievements"])
        assertEquals(11, stats["totalAchievements"])
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BLOCKED ATTEMPTS ANALYTICS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `getBlockedAttemptsToday returns correct count`() = runBlocking {
        // Given
        whenever(blockedAttemptDao.getCountBetween(any(), any())).thenReturn(5)
        
        // When
        val count = statisticsService.getBlockedAttemptsToday()
        
        // Then
        assertEquals(5, count)
        verify(blockedAttemptDao).getCountBetween(any(), any())
    }
    
    @Test
    fun `getBlockedAttemptsThisWeek returns correct count`() = runBlocking {
        // Given
        whenever(blockedAttemptDao.getCountBetween(any(), any())).thenReturn(25)
        
        // When
        val count = statisticsService.getBlockedAttemptsThisWeek()
        
        // Then
        assertEquals(25, count)
        verify(blockedAttemptDao).getCountBetween(any(), any())
    }
    
    @Test
    fun `getBlockedAttemptsThisMonth returns correct count`() = runBlocking {
        // Given
        whenever(blockedAttemptDao.getCountBetween(any(), any())).thenReturn(100)
        
        // When
        val count = statisticsService.getBlockedAttemptsThisMonth()
        
        // Then
        assertEquals(100, count)
        verify(blockedAttemptDao).getCountBetween(any(), any())
    }
    
    @Test
    fun `getTopBlockedDomains returns domain counts`() = runBlocking {
        // Given
        val domainCounts = listOf(
            DomainCount("example.com", 50),
            DomainCount("test.com", 30),
            DomainCount("demo.com", 20)
        )
        whenever(blockedAttemptDao.getTopBlockedDomains(10)).thenReturn(domainCounts)
        
        // When
        val result = statisticsService.getTopBlockedDomains(10)
        
        // Then
        assertEquals(3, result.size)
        assertEquals("example.com", result[0].domain)
        assertEquals(50, result[0].count)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INTERVENTION ANALYTICS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `getInterventionCompletionRate calculates correctly`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getCount()).thenReturn(100)
        whenever(interventionSessionDao.getCompletedCount()).thenReturn(80)
        
        // When
        val rate = statisticsService.getInterventionCompletionRate()
        
        // Then
        assertEquals(80.0, rate)
    }
    
    @Test
    fun `getInterventionCompletionRate returns zero when no interventions`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getCount()).thenReturn(0)
        whenever(interventionSessionDao.getCompletedCount()).thenReturn(0)
        
        // When
        val rate = statisticsService.getInterventionCompletionRate()
        
        // Then
        assertEquals(0.0, rate)
    }
    
    @Test
    fun `getInterventionSkipRate calculates correctly`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getCount()).thenReturn(100)
        whenever(interventionSessionDao.getSkippedCount()).thenReturn(20)
        
        // When
        val rate = statisticsService.getInterventionSkipRate()
        
        // Then
        assertEquals(20.0, rate)
    }
    
    @Test
    fun `getAverageInterventionTime returns correct value`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getAverageCompletionTime()).thenReturn(120.0)
        
        // When
        val avgTime = statisticsService.getAverageInterventionTime()
        
        // Then
        assertEquals(120.0, avgTime)
    }
    
    @Test
    fun `getInterventionStatistics returns comprehensive metrics`() = runBlocking {
        // Given
        whenever(interventionSessionDao.getCount()).thenReturn(100)
        whenever(interventionSessionDao.getCompletedCount()).thenReturn(80)
        whenever(interventionSessionDao.getSkippedCount()).thenReturn(20)
        whenever(interventionSessionDao.getAverageCompletionTime()).thenReturn(120.0)
        
        // When
        val stats = statisticsService.getInterventionStatistics()
        
        // Then
        assertEquals(100, stats["totalInterventions"])
        assertEquals(80, stats["completedInterventions"])
        assertEquals(20, stats["skippedInterventions"])
        assertEquals(80.0, stats["completionRate"])
        assertEquals(20.0, stats["skipRate"])
        assertEquals(120.0, stats["averageCompletionTime"])
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // CHART DATA GENERATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `getBlockedAttemptsChartData returns data for 7 days`() = runBlocking {
        // Given
        whenever(blockedAttemptDao.getCountBetween(any(), any())).thenReturn(5)
        
        // When
        val chartData = statisticsService.getBlockedAttemptsChartData(7)
        
        // Then
        assertEquals(7, chartData.size)
        assertTrue(chartData.all { it.value == 5f })
    }
    
    @Test
    fun `getInterventionCompletionChartData returns data for 7 days`() = runBlocking {
        // Given
        val sessions = listOf(
            createInterventionSession(taskCompleted = true),
            createInterventionSession(taskCompleted = true),
            createInterventionSession(taskCompleted = false)
        )
        whenever(interventionSessionDao.getBetweenDates(any(), any())).thenReturn(sessions)
        
        // When
        val chartData = statisticsService.getInterventionCompletionChartData(7)
        
        // Then
        assertEquals(7, chartData.size)
        // Each day has 2/3 completion rate = 66.67%
        assertTrue(chartData.all { it.value > 66f && it.value < 67f })
    }
    
    @Test
    fun `getStreakProgressChartData returns data for 30 days`() = runBlocking {
        // Given
        val today = LocalDate.now()
        val logs = (0 until 30).map { i ->
            createDailyLog(today.minusDays(i.toLong()), wasClean = true)
        }
        whenever(dailyLogDao.getBetweenDates(any(), any())).thenReturn(logs)
        
        // When
        val chartData = statisticsService.getStreakProgressChartData(30)
        
        // Then
        assertEquals(30, chartData.size)
    }
    
    @Test
    fun `getMoodRatingChartData returns data for 30 days`() = runBlocking {
        // Given
        val today = LocalDate.now()
        val logs = (0 until 30).map { i ->
            createDailyLog(today.minusDays(i.toLong()), wasClean = true, moodRating = 4)
        }
        whenever(dailyLogDao.getBetweenDates(any(), any())).thenReturn(logs)
        
        // When
        val chartData = statisticsService.getMoodRatingChartData(30)
        
        // Then
        assertEquals(30, chartData.size)
        assertTrue(chartData.all { it.value == 4f })
    }
    
    @Test
    fun `getTopDomainsPieChartData returns pie chart data`() = runBlocking {
        // Given
        val domainCounts = listOf(
            DomainCount("example.com", 50),
            DomainCount("test.com", 30),
            DomainCount("demo.com", 20)
        )
        whenever(blockedAttemptDao.getTopBlockedDomains(5)).thenReturn(domainCounts)
        
        // When
        val pieData = statisticsService.getTopDomainsPieChartData(5)
        
        // Then
        assertEquals(3, pieData.size)
        assertEquals("example.com", pieData[0].label)
        assertEquals(50f, pieData[0].value)
        assertEquals(50f, pieData[0].percentage) // 50/100 = 50%
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun createTestUser(
        currentStreak: Int = 0,
        longestStreak: Int = 0,
        totalDaysClean: Int = 0
    ): UserEntity {
        return UserEntity(
            id = 1,
            startDate = LocalDate.now(),
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalDaysClean = totalDaysClean,
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
    
    private fun createDailyLog(
        date: LocalDate,
        wasClean: Boolean,
        moodRating: Int? = null
    ): DailyLogEntity {
        return DailyLogEntity(
            id = 0,
            date = date.toEpochDay(),
            wasClean = wasClean,
            notes = null,
            moodRating = moodRating,
            triggers = emptyList(),
            createdAt = LocalDateTime.now()
        )
    }
    
    private fun createInterventionSession(taskCompleted: Boolean): InterventionSessionEntity {
        return InterventionSessionEntity(
            id = 0,
            sessionId = "session_${System.currentTimeMillis()}",
            flashcardId = "flashcard_1",
            blockedUrl = "https://example.com",
            timestamp = LocalDateTime.now().toEpochSecond(java.time.ZoneOffset.UTC),
            taskCompleted = taskCompleted,
            taskSkipped = !taskCompleted,
            completionTime = if (taskCompleted) 120 else null,
            createdAt = LocalDateTime.now()
        )
    }
}

