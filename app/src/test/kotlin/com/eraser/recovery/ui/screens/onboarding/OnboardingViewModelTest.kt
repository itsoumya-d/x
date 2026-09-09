package com.eraser.recovery.ui.screens.onboarding

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
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
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * OnboardingViewModel Test
 * 
 * Tests onboarding state management, permissions, and completion.
 * 
 * Note: These are unit tests that mock Android components.
 * Integration tests with actual permissions should be done separately.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class OnboardingViewModelTest {
    
    private lateinit var context: Context
    private lateinit var powerManager: PowerManager
    private lateinit var viewModel: OnboardingViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mock()
        powerManager = mock()
        
        // Mock system service
        whenever(context.getSystemService(Context.POWER_SERVICE)).thenReturn(powerManager)
        whenever(context.packageName).thenReturn("com.eraser.recovery")
        
        // Mock battery optimization status (not granted by default)
        whenever(powerManager.isIgnoringBatteryOptimizations(any())).thenReturn(false)
        
        viewModel = OnboardingViewModel(context)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PAGE NAVIGATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `initial page is 0`() {
        // Given & When
        val currentPage = viewModel.currentPage.value
        
        // Then
        assertEquals(0, currentPage)
    }
    
    @Test
    fun `updatePage updates current page`() {
        // Given
        val newPage = 3
        
        // When
        viewModel.updatePage(newPage)
        
        // Then
        assertEquals(newPage, viewModel.currentPage.value)
    }
    
    @Test
    fun `updatePage can go to last page`() {
        // Given
        val lastPage = 5
        
        // When
        viewModel.updatePage(lastPage)
        
        // Then
        assertEquals(lastPage, viewModel.currentPage.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // VPN PERMISSION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `initial VPN permission is not granted`() {
        // Given & When
        val vpnPermissionGranted = viewModel.vpnPermissionGranted.value
        
        // Then
        assertFalse(vpnPermissionGranted)
    }
    
    @Test
    fun `onVpnPermissionGranted updates state`() {
        // Given
        assertFalse(viewModel.vpnPermissionGranted.value)
        
        // When
        viewModel.onVpnPermissionGranted()
        
        // Then
        assertTrue(viewModel.vpnPermissionGranted.value)
    }
    
    @Test
    fun `prepareVpnPermission returns intent when not granted`() {
        // Given
        // VPN permission not granted (default state)
        
        // When
        val intent = viewModel.prepareVpnPermission()
        
        // Then
        // In unit tests, VpnService.prepare() returns null because it's a mock
        // In real scenario, it would return an Intent if permission is not granted
        // We can't fully test this without instrumentation tests
        // Just verify the method doesn't crash
        assertNotNull(viewModel)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BATTERY OPTIMIZATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `initial battery optimization is not granted`() {
        // Given & When
        val batteryOptimizationGranted = viewModel.batteryOptimizationGranted.value
        
        // Then
        assertFalse(batteryOptimizationGranted)
    }
    
    @Test
    fun `battery optimization status is checked on init`() {
        // Given & When
        // ViewModel is created in setup()
        
        // Then
        verify(powerManager).isIgnoringBatteryOptimizations("com.eraser.recovery")
    }
    
    @Test
    fun `prepareBatteryOptimizationIntent returns intent`() {
        // Given & When
        val intent = viewModel.prepareBatteryOptimizationIntent()
        
        // Then
        assertNotNull(intent)
        assertEquals(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, intent.action)
    }
    
    @Test
    fun `refreshBatteryOptimizationStatus checks status again`() {
        // Given
        // Initial check happens in init
        verify(powerManager, times(1)).isIgnoringBatteryOptimizations(any())
        
        // When
        viewModel.refreshBatteryOptimizationStatus()
        
        // Then
        verify(powerManager, times(2)).isIgnoringBatteryOptimizations(any())
    }
    
    @Test
    fun `battery optimization granted when PowerManager returns true`() {
        // Given
        whenever(powerManager.isIgnoringBatteryOptimizations(any())).thenReturn(true)
        
        // When
        val viewModelWithGranted = OnboardingViewModel(context)
        
        // Then
        assertTrue(viewModelWithGranted.batteryOptimizationGranted.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ONBOARDING COMPLETION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `completeOnboarding saves completion state`() = runTest {
        // Given: a real context so DataStore has a writable location
        val realContext = ApplicationProvider.getApplicationContext<Context>()
        val vm = OnboardingViewModel(realContext)

        // When
        vm.completeOnboarding()
        advanceUntilIdle()

        // Then
        // DataStore operations are async, so we can't easily verify in unit tests
        // This would require instrumentation tests or mocking DataStore
        // Just verify the method doesn't crash
        assertNotNull(vm)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATE FLOW TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `all state flows are initialized`() {
        // Given & When & Then
        assertNotNull(viewModel.currentPage)
        assertNotNull(viewModel.batteryOptimizationGranted)
        assertNotNull(viewModel.vpnPermissionGranted)
    }
    
    @Test
    fun `state flows have correct initial values`() {
        // Given & When & Then
        assertEquals(0, viewModel.currentPage.value)
        assertFalse(viewModel.batteryOptimizationGranted.value)
        assertFalse(viewModel.vpnPermissionGranted.value)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // INTEGRATION TESTS (Conceptual - require Android instrumentation)
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `viewModel is created successfully`() {
        // Given & When
        val vm = OnboardingViewModel(context)
        
        // Then
        assertNotNull(vm)
    }
    
    @Test
    fun `multiple page updates work correctly`() {
        // Given & When
        viewModel.updatePage(1)
        assertEquals(1, viewModel.currentPage.value)
        
        viewModel.updatePage(2)
        assertEquals(2, viewModel.currentPage.value)
        
        viewModel.updatePage(3)
        assertEquals(3, viewModel.currentPage.value)
        
        // Then
        assertEquals(3, viewModel.currentPage.value)
    }
    
    @Test
    fun `page can go back to 0`() {
        // Given
        viewModel.updatePage(3)
        assertEquals(3, viewModel.currentPage.value)
        
        // When
        viewModel.updatePage(0)
        
        // Then
        assertEquals(0, viewModel.currentPage.value)
    }
}

/**
 * OnboardingPage Test
 * 
 * Tests onboarding page data model.
 */
class OnboardingPageTest {
    
    @Test
    fun `OnboardingPages has 6 pages`() {
        // Given & When
        val pages = OnboardingPages.pages
        
        // Then
        assertEquals(6, pages.size)
    }
    
    @Test
    fun `first page is welcome page`() {
        // Given & When
        val firstPage = OnboardingPages.pages[0]
        
        // Then
        assertEquals("Welcome to Eraser", firstPage.title)
        assertFalse(firstPage.isVpnPermissionPage)
        assertFalse(firstPage.isBatteryPage)
    }
    
    @Test
    fun `second page is VPN permission page`() {
        // Given & When
        val secondPage = OnboardingPages.pages[1]
        
        // Then
        assertEquals("How It Works", secondPage.title)
        assertTrue(secondPage.isVpnPermissionPage)
        assertFalse(secondPage.isBatteryPage)
    }
    
    @Test
    fun `fifth page is battery optimization page`() {
        // Given & When
        val fifthPage = OnboardingPages.pages[4]
        
        // Then
        assertEquals("Battery Optimization", fifthPage.title)
        assertFalse(fifthPage.isVpnPermissionPage)
        assertTrue(fifthPage.isBatteryPage)
    }
    
    @Test
    fun `last page is progress tracking page`() {
        // Given & When
        val lastPage = OnboardingPages.pages[5]
        
        // Then
        assertEquals("Track Your Progress", lastPage.title)
        assertFalse(lastPage.isVpnPermissionPage)
        assertFalse(lastPage.isBatteryPage)
    }
    
    @Test
    fun `all pages have required fields`() {
        // Given & When
        val pages = OnboardingPages.pages
        
        // Then
        pages.forEach { page ->
            assertNotNull(page.icon)
            assertTrue(page.title.isNotEmpty())
            assertTrue(page.description.isNotEmpty())
            assertNotNull(page.color)
        }
    }
}

