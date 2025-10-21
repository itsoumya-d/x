package com.eraser.recovery.ui.screens.reports

import com.eraser.recovery.domain.statistics.ChartDataPoint
import com.eraser.recovery.domain.statistics.StatisticsService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ReportsViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {
    
    private lateinit var viewModel: ReportsViewModel
    private lateinit var statisticsService: StatisticsService
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        statisticsService = mockk(relaxed = true)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `loadStatistics updates all state flows`() = runTest {
        // Given
        val dashboardStats = mapOf(
            "currentStreak" to 5,
            "longestStreak" to 10,
            "totalCleanDays" to 15,
            "interventionSuccessRate" to 85.5,
            "totalBlockedAttempts" to 20,
            "completedInterventions" to 18
        )
        val chartData = listOf(
            ChartDataPoint("Day 1", 1f, LocalDate.now().minusDays(6)),
            ChartDataPoint("Day 2", 2f, LocalDate.now().minusDays(5))
        )
        coEvery { statisticsService.getDashboardStatistics() } returns dashboardStats
        coEvery { statisticsService.getStreakProgressChartData(7) } returns chartData
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        viewModel.loadStatistics()
        advanceUntilIdle()
        
        // Then
        assertEquals(5, viewModel.currentStreak.value)
        assertEquals(10, viewModel.longestStreak.value)
        assertEquals(15, viewModel.totalDaysClean.value)
        assertEquals(85.5, viewModel.successRate.value)
        assertEquals(20, viewModel.blockedAttempts.value)
        assertEquals(18, viewModel.completedInterventions.value)
        assertEquals(2, viewModel.chartData.value.size)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `loading state is true initially`() = runTest {
        // Given
        coEvery { statisticsService.getDashboardStatistics() } returns emptyMap()
        coEvery { statisticsService.getStreakProgressChartData(7) } returns emptyList()
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        
        // Then
        assertTrue(viewModel.isLoading.value)
    }
    
    @Test
    fun `loading state is false after statistics loaded`() = runTest {
        // Given
        val dashboardStats = mapOf(
            "currentStreak" to 5,
            "longestStreak" to 10,
            "totalCleanDays" to 15,
            "interventionSuccessRate" to 85.5,
            "totalBlockedAttempts" to 20,
            "completedInterventions" to 18
        )
        coEvery { statisticsService.getDashboardStatistics() } returns dashboardStats
        coEvery { statisticsService.getStreakProgressChartData(7) } returns emptyList()
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        viewModel.loadStatistics()
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `handles missing dashboard stats gracefully`() = runTest {
        // Given
        val dashboardStats = mapOf<String, Any>()
        coEvery { statisticsService.getDashboardStatistics() } returns dashboardStats
        coEvery { statisticsService.getStreakProgressChartData(7) } returns emptyList()
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        viewModel.loadStatistics()
        advanceUntilIdle()
        
        // Then
        assertEquals(0, viewModel.currentStreak.value)
        assertEquals(0, viewModel.longestStreak.value)
        assertEquals(0, viewModel.totalDaysClean.value)
        assertEquals(0.0, viewModel.successRate.value)
        assertEquals(0, viewModel.blockedAttempts.value)
        assertEquals(0, viewModel.completedInterventions.value)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `handles empty chart data`() = runTest {
        // Given
        val dashboardStats = mapOf(
            "currentStreak" to 5,
            "longestStreak" to 10,
            "totalCleanDays" to 15,
            "interventionSuccessRate" to 85.5,
            "totalBlockedAttempts" to 20,
            "completedInterventions" to 18
        )
        coEvery { statisticsService.getDashboardStatistics() } returns dashboardStats
        coEvery { statisticsService.getStreakProgressChartData(7) } returns emptyList()
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        viewModel.loadStatistics()
        advanceUntilIdle()
        
        // Then
        assertEquals(0, viewModel.chartData.value.size)
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `refresh reloads statistics`() = runTest {
        // Given
        val initialStats = mapOf(
            "currentStreak" to 5,
            "longestStreak" to 10,
            "totalCleanDays" to 15,
            "interventionSuccessRate" to 85.5,
            "totalBlockedAttempts" to 20,
            "completedInterventions" to 18
        )
        val updatedStats = mapOf(
            "currentStreak" to 6,
            "longestStreak" to 10,
            "totalCleanDays" to 16,
            "interventionSuccessRate" to 86.0,
            "totalBlockedAttempts" to 22,
            "completedInterventions" to 20
        )
        coEvery { statisticsService.getDashboardStatistics() } returns initialStats andThen updatedStats
        coEvery { statisticsService.getStreakProgressChartData(7) } returns emptyList()
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        viewModel.loadStatistics()
        advanceUntilIdle()
        assertEquals(5, viewModel.currentStreak.value)
        
        viewModel.refresh()
        advanceUntilIdle()
        
        // Then
        assertEquals(6, viewModel.currentStreak.value)
        assertEquals(16, viewModel.totalDaysClean.value)
        assertEquals(22, viewModel.blockedAttempts.value)
    }
    
    @Test
    fun `handles exception during load`() = runTest {
        // Given
        coEvery { statisticsService.getDashboardStatistics() } throws Exception("Test error")
        coEvery { statisticsService.getStreakProgressChartData(7) } returns emptyList()
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        viewModel.loadStatistics()
        advanceUntilIdle()
        
        // Then
        assertFalse(viewModel.isLoading.value)
        assertEquals(0, viewModel.currentStreak.value)
    }
    
    @Test
    fun `chart data is populated correctly`() = runTest {
        // Given
        val dashboardStats = mapOf(
            "currentStreak" to 5,
            "longestStreak" to 10,
            "totalCleanDays" to 15,
            "interventionSuccessRate" to 85.5,
            "totalBlockedAttempts" to 20,
            "completedInterventions" to 18
        )
        val chartData = listOf(
            ChartDataPoint("Mon", 1f, LocalDate.now().minusDays(6)),
            ChartDataPoint("Tue", 2f, LocalDate.now().minusDays(5)),
            ChartDataPoint("Wed", 3f, LocalDate.now().minusDays(4)),
            ChartDataPoint("Thu", 4f, LocalDate.now().minusDays(3)),
            ChartDataPoint("Fri", 5f, LocalDate.now().minusDays(2)),
            ChartDataPoint("Sat", 6f, LocalDate.now().minusDays(1)),
            ChartDataPoint("Sun", 7f, LocalDate.now())
        )
        coEvery { statisticsService.getDashboardStatistics() } returns dashboardStats
        coEvery { statisticsService.getStreakProgressChartData(7) } returns chartData
        
        // When
        viewModel = ReportsViewModel(statisticsService)
        viewModel.loadStatistics()
        advanceUntilIdle()
        
        // Then
        assertEquals(7, viewModel.chartData.value.size)
        assertEquals("Mon", viewModel.chartData.value.first().label)
        assertEquals("Sun", viewModel.chartData.value.last().label)
        assertEquals(1f, viewModel.chartData.value.first().value)
        assertEquals(7f, viewModel.chartData.value.last().value)
    }
}

