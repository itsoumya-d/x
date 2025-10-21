package com.eraser.recovery.ui.navigation

/**
 * Navigation Routes
 *
 * Defines all screen routes in the app using string-based navigation.
 * Matches Flutter app's routing structure.
 *
 * Research findings from Material Design 3 and Jetpack Navigation:
 * - Use sealed class for type-safe route definitions
 * - Define routes as strings for compatibility
 * - Support deep links for SafePage
 */
sealed class Screen(val route: String) {
    // Onboarding
    object Onboarding : Screen("onboarding")

    // Main Navigation Screen (contains bottom nav)
    object MainNavigation : Screen("main_navigation")

    // Bottom Nav Tabs
    object Home : Screen("home")
    object Rewards : Screen("rewards")
    object Reports : Screen("reports")

    // Feature Screens
    object SafePage : Screen("safe_page?domain={domain}&url={url}") {
        fun createRoute(domain: String, url: String? = null): String {
            return if (url != null) {
                "safe_page?domain=$domain&url=$url"
            } else {
                "safe_page?domain=$domain&url=null"
            }
        }
    }

    object BlockedHistory : Screen("blocked_history")
    object DailyCheckIn : Screen("daily_checkin")
    object Settings : Screen("settings")

    object BadgeDetail : Screen("badge_detail/{achievementId}") {
        fun createRoute(achievementId: String): String {
            return "badge_detail/$achievementId"
        }
    }
}

