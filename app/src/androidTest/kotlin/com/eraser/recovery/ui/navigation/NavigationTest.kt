package com.eraser.recovery.ui.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.eraser.recovery.ui.theme.EraserTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Navigation Tests
 * 
 * Tests for navigation flow and routing.
 * Verifies proper navigation between screens and back stack management.
 */
class NavigationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var navController: TestNavHostController
    
    @Before
    fun setupNavHost() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            
            EraserTheme {
                EraserNavHost(navController = navController)
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // START DESTINATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun navHost_verifyStartDestination_isOnboarding() {
        // Verify onboarding screen is displayed
        composeTestRule
            .onNodeWithText("Welcome to Eraser")
            .assertIsDisplayed()
        
        // Verify current route is Onboarding
        assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<Onboarding>() ?: false)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // ONBOARDING NAVIGATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun onboarding_completeFlow_navigatesToMainNavigation() {
        // Navigate through onboarding pages
        repeat(6) {
            composeTestRule
                .onNodeWithText("Next")
                .performClick()
        }
        
        // Complete onboarding
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()
        
        // Verify navigation to main navigation
        composeTestRule.waitForIdle()
        assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<MainNavigation>() ?: false)
    }
    
    @Test
    fun onboarding_complete_clearsBackStack() {
        // Navigate through onboarding
        repeat(6) {
            composeTestRule
                .onNodeWithText("Next")
                .performClick()
        }
        
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify onboarding is not in back stack
        val hasOnboarding = navController.backQueue.any { entry ->
            entry.destination.hasRoute<Onboarding>()
        }
        assertTrue(!hasOnboarding, "Onboarding should be removed from back stack")
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // BOTTOM NAVIGATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun mainNavigation_verifyHomeTabDisplayed() {
        // Navigate to main navigation
        navigateToMainNavigation()
        
        // Verify Home tab is displayed
        composeTestRule
            .onNodeWithText("Home")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainNavigation_clickRewardsTab_showsRewardsScreen() {
        navigateToMainNavigation()
        
        // Click Rewards tab
        composeTestRule
            .onNodeWithContentDescription("Rewards")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify Rewards screen is displayed
        composeTestRule
            .onNodeWithText("Rewards Screen")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainNavigation_clickReportsTab_showsReportsScreen() {
        navigateToMainNavigation()
        
        // Click Reports tab
        composeTestRule
            .onNodeWithContentDescription("Reports")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify Reports screen is displayed
        composeTestRule
            .onNodeWithText("Reports Screen")
            .assertIsDisplayed()
    }
    
    @Test
    fun mainNavigation_switchBetweenTabs_preservesState() {
        navigateToMainNavigation()
        
        // Click Rewards tab
        composeTestRule
            .onNodeWithContentDescription("Rewards")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Click Home tab
        composeTestRule
            .onNodeWithContentDescription("Home")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify Home screen is displayed again
        composeTestRule
            .onNodeWithText("Home")
            .assertIsDisplayed()
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // DEEP LINK TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun safePage_deepLink_navigatesToSafePage() {
        // Navigate to main navigation first
        navigateToMainNavigation()
        
        // Navigate to SafePage using deep link
        navController.navigate(SafePage(domain = "example.com", url = "https://example.com"))
        
        composeTestRule.waitForIdle()
        
        // Verify SafePage is displayed
        assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<SafePage>() ?: false)
    }
    
    @Test
    fun safePage_dismiss_returnsToMainNavigation() {
        navigateToMainNavigation()
        
        // Navigate to SafePage
        navController.navigate(SafePage(domain = "example.com"))
        composeTestRule.waitForIdle()
        
        // Dismiss SafePage (click Skip Task button)
        composeTestRule
            .onNodeWithText("Skip Task")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify back to main navigation
        assertTrue(navController.currentBackStackEntry?.destination?.hasRoute<MainNavigation>() ?: false)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    private fun navigateToMainNavigation() {
        // Navigate through onboarding
        repeat(6) {
            composeTestRule
                .onNodeWithText("Next")
                .performClick()
        }
        
        composeTestRule
            .onNodeWithText("Get Started")
            .performClick()
        
        composeTestRule.waitForIdle()
    }
}

