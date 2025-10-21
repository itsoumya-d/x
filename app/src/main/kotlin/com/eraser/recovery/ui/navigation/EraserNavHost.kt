package com.eraser.recovery.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.eraser.recovery.ui.screens.checkin.DailyCheckInScreen
import com.eraser.recovery.ui.screens.history.BlockedHistoryScreen
import com.eraser.recovery.ui.screens.main.MainNavigationScreen
import com.eraser.recovery.ui.screens.onboarding.OnboardingScreen
import com.eraser.recovery.ui.screens.safepage.SafePageScreen
import com.eraser.recovery.ui.screens.settings.SettingsScreen

/**
 * Eraser Navigation Host
 *
 * Main navigation setup for the app using string-based navigation.
 * Handles routing between all screens with proper back stack management.
 *
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family:
 * - Check onboarding completion on app start
 * - Redirect to onboarding if not completed
 * - Use bottom navigation for main screens (Home, Rewards, Reports)
 * - Support deep links for Safe Page (blocked content screen)
 * - Proper back stack management (clear onboarding after completion)
 *
 * Jetpack Navigation Best Practices:
 * - Use string-based routes for compatibility
 * - Define deep links for external navigation
 * - Clear back stack when appropriate (onboarding → main)
 * - Use NavHost for centralized navigation logic
 */
@Composable
fun EraserNavHost(
    navController: NavHostController = rememberNavController()
) {
    // TODO: Check onboarding completion from preferences
    // For now, start with onboarding
    // In Phase 5, check SharedPreferences for onboarding completion
    val startDestination = Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding Flow
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingComplete = {
                    // Navigate to main navigation and clear onboarding from back stack
                    navController.navigate(Screen.MainNavigation.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Main Navigation (Bottom Nav Container)
        composable(route = Screen.MainNavigation.route) {
            MainNavigationScreen(navController = navController)
        }

        // Safe Page (Blocked Content Screen)
        // Supports deep links for VPN service to trigger
        composable(
            route = Screen.SafePage.route,
            arguments = listOf(
                navArgument("domain") { type = NavType.StringType },
                navArgument("url") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            deepLinks = listOf(
                navDeepLink {
                    uriPattern = "eraser://safepage?domain={domain}&url={url}"
                    action = Intent.ACTION_VIEW
                }
            )
        ) { backStackEntry ->
            val domain = backStackEntry.arguments?.getString("domain") ?: ""
            val url = backStackEntry.arguments?.getString("url")
            SafePageScreen(
                blockedDomain = domain,
                blockedUrl = url,
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }

        // Settings Screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Blocked History Screen
        composable(route = Screen.BlockedHistory.route) {
            BlockedHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Daily Check-In Screen
        composable(route = Screen.DailyCheckIn.route) {
            DailyCheckInScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCheckInComplete = {
                    navController.popBackStack()
                }
            )
        }

        // TODO: Add other screens in Phase 4
        // Phase 4:
        // - BadgeDetail
    }
}

