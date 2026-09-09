package com.eraser.recovery.ui.screens.home

import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.eraser.recovery.data.local.dao.BlockedAttemptDao
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.data.local.entity.UserEntity
import com.eraser.recovery.domain.journey.JourneyService
import com.eraser.recovery.domain.vpn.VpnManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Home ViewModel Tests
 * 
 * Tests for HomeViewModel functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    
    private lateinit var viewModel: HomeViewModel
    private lateinit var context: Context
    private lateinit var userDao: UserDao
    private lateinit var blockedAttemptDao: BlockedAttemptDao
    private lateinit var journeyService: JourneyService
    private lateinit var vpnManager: VpnManager
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        context = mockk(relaxed = true)
        userDao = mockk(relaxed = true)
        blockedAttemptDao = mockk(relaxed = true)
        journeyService = mockk(relaxed = true)
        vpnManager = mockk(relaxed = true)
        
        // Mock VpnService.prepare
        mockkStatic(VpnService::class)
        every { VpnService.prepare(any()) } returns null
        
        // Default mocks
        coEvery { userDao.getUser() } returns flowOf(null)
        coEvery { blockedAttemptDao.getCountByDate(any()) } returns 0
        coEvery { blockedAttemptDao.getCountBetweenDates(any(), any()) } returns 0
        coEvery { blockedAttemptDao.getTotalCount() } returns 0
        coEvery { journeyService.isJourneyActive } returns MutableStateFlow(false)
        coEvery { journeyService.startJourney() } returns Result.success(Unit)
        coEvery { journeyService.stopJourney() } returns Result.success(Unit)
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
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertNotNull(viewModel)
    }
    
    @Test
    fun `initial state is correct`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        
        assertEquals(0, viewModel.daysSinceStart.value)
        assertEquals(0, viewModel.currentStreak.value)
        assertFalse(viewModel.isProtectionActive.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals(0, viewModel.blockedToday.value)
        assertEquals(0, viewModel.blockedThisWeek.value)
        assertEquals(0, viewModel.blockedTotal.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // USER DATA TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `loadUserData updates days since start`() = runTest {
        val startDate = LocalDate.now().minusDays(10)
        val user = UserEntity(
            id = 1,
            startDate = startDate,
            currentStreak = 5,
            longestStreak = 10,
            lastCheckInDate = LocalDate.now()
        )
        coEvery { userDao.getUser() } returns flowOf(user)
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        advanceUntilIdle()
        
        assertEquals(10, viewModel.daysSinceStart.value)
        assertEquals(5, viewModel.currentStreak.value)
    }
    
    @Test
    fun `loadUserData handles null user`() = runTest {
        coEvery { userDao.getUser() } returns flowOf(null)
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        advanceUntilIdle()
        
        assertEquals(0, viewModel.daysSinceStart.value)
        assertEquals(0, viewModel.currentStreak.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `loadStatistics updates blocked counts`() = runTest {
        coEvery { blockedAttemptDao.getCountByDate(any()) } returns 5
        coEvery { blockedAttemptDao.getCountBetweenDates(any(), any()) } returns 20
        coEvery { blockedAttemptDao.getTotalCount() } returns 100
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        advanceUntilIdle()
        
        assertEquals(5, viewModel.blockedToday.value)
        assertEquals(20, viewModel.blockedThisWeek.value)
        assertEquals(100, viewModel.blockedTotal.value)
    }
    
    @Test
    fun `loadStatistics handles zero counts`() = runTest {
        coEvery { blockedAttemptDao.getCountByDate(any()) } returns 0
        coEvery { blockedAttemptDao.getCountBetweenDates(any(), any()) } returns 0
        coEvery { blockedAttemptDao.getTotalCount() } returns 0
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        advanceUntilIdle()
        
        assertEquals(0, viewModel.blockedToday.value)
        assertEquals(0, viewModel.blockedThisWeek.value)
        assertEquals(0, viewModel.blockedTotal.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // JOURNEY STATE TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `observeJourneyState updates protection active`() = runTest {
        coEvery { journeyService.isJourneyActive } returns MutableStateFlow(true)
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        advanceUntilIdle()
        
        assertTrue(viewModel.isProtectionActive.value)
    }
    
    @Test
    fun `observeJourneyState handles inactive journey`() = runTest {
        coEvery { journeyService.isJourneyActive } returns MutableStateFlow(false)

        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        advanceUntilIdle()
        
        assertFalse(viewModel.isProtectionActive.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PROTECTION CONTROL TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `prepareVpnPermission returns null when granted`() {
        every { VpnService.prepare(any()) } returns null
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        
        val intent = viewModel.prepareVpnPermission()
        assertEquals(null, intent)
    }
    
    @Test
    fun `prepareVpnPermission returns intent when not granted`() {
        val mockIntent = mockk<Intent>()
        every { VpnService.prepare(any()) } returns mockIntent
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        
        val intent = viewModel.prepareVpnPermission()
        assertEquals(mockIntent, intent)
    }
    
    @Test
    fun `startProtection calls journeyService startJourney`() = runTest {
        coEvery { journeyService.startJourney() } returns Result.success(Unit)
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        viewModel.startProtection()
        advanceUntilIdle()
        
        coVerify { journeyService.startJourney() }
    }
    
    @Test
    fun `startProtection sets loading state`() = runTest {
        coEvery { journeyService.startJourney() } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        viewModel.startProtection()
        
        // Loading should be true during operation
        advanceTimeBy(50)
        assertTrue(viewModel.isLoading.value)
        
        // Loading should be false after completion
        advanceUntilIdle()
        assertFalse(viewModel.isLoading.value)
    }
    
    @Test
    fun `stopProtection calls journeyService stopJourney`() = runTest {
        coEvery { journeyService.stopJourney() } returns Result.success(Unit)
        
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        viewModel.stopProtection()
        advanceUntilIdle()
        
        coVerify { journeyService.stopJourney() }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // REFRESH TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `refresh reloads all data`() = runTest {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        advanceUntilIdle()
        
        // Clear invocations
        clearMocks(userDao, blockedAttemptDao, answers = false)
        
        // Refresh
        viewModel.refresh()
        advanceUntilIdle()
        
        // Verify data is reloaded
        coVerify { userDao.getUser() }
        coVerify { blockedAttemptDao.getCountByDate(any()) }
        coVerify { blockedAttemptDao.getTotalCount() }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // MOTIVATIONAL MESSAGE TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `getMotivationalMessage returns correct message for day 0`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("Start your journey today", viewModel.getMotivationalMessage(0))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for day 1`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("Great start! Keep going", viewModel.getMotivationalMessage(1))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for days 2-6`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("Building momentum", viewModel.getMotivationalMessage(5))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for days 7-13`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("You're doing amazing!", viewModel.getMotivationalMessage(10))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for days 14-29`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("Incredible progress!", viewModel.getMotivationalMessage(20))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for days 30-89`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("You're unstoppable!", viewModel.getMotivationalMessage(60))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for days 90-179`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("Legendary streak!", viewModel.getMotivationalMessage(120))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for days 180-364`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("Almost a year!", viewModel.getMotivationalMessage(300))
    }
    
    @Test
    fun `getMotivationalMessage returns correct message for days 365+`() {
        viewModel = HomeViewModel(context, userDao, blockedAttemptDao, journeyService, vpnManager)
        assertEquals("You're a champion!", viewModel.getMotivationalMessage(400))
    }
}

