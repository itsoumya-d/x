package com.eraser.recovery.ui.screens.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Onboarding Page Data Model
 * 
 * Represents a single page in the onboarding flow.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - 4-6 pages is optimal (not too short, not too long)
 * - Each page should have a clear purpose
 * - Visual icons help with comprehension
 * - Color coding helps differentiate pages
 * - Special pages for permission requests
 * 
 * @param icon Icon to display
 * @param title Page title
 * @param description Page description
 * @param color Theme color for the page
 * @param isVpnPermissionPage Whether this page requests VPN permission
 * @param isBatteryPage Whether this page requests battery optimization exemption
 */
data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color,
    val isVpnPermissionPage: Boolean = false,
    val isBatteryPage: Boolean = false
)

/**
 * Onboarding Pages
 * 
 * Defines all pages in the onboarding flow.
 */
object OnboardingPages {
    
    val pages = listOf(
        // Page 1: Welcome
        OnboardingPage(
            icon = Icons.Default.Shield,
            title = "Welcome to Eraser",
            description = "Your personal digital wellness companion. Block adult content and build healthier habits.",
            color = Color(0xFF2196F3) // Blue
        ),
        
        // Page 2: How It Works
        OnboardingPage(
            icon = Icons.Default.VpnLock,
            title = "How It Works",
            description = "Eraser uses a VPN connection to filter content at the network level. All filtering happens on your device - no data is sent to external servers.",
            color = Color(0xFF4CAF50), // Green
            isVpnPermissionPage = true
        ),
        
        // Page 3: Privacy
        OnboardingPage(
            icon = Icons.Default.PrivacyTip,
            title = "100% Private",
            description = "Your privacy is our priority. We don't collect, store, or share any of your browsing data. Everything stays on your device.",
            color = Color(0xFF9C27B0) // Purple
        ),
        
        // Page 4: Comprehensive Blocking
        OnboardingPage(
            icon = Icons.Default.Block,
            title = "Comprehensive Blocking",
            description = "We block 156,000+ adult domains system-wide. Works in all browsers and apps on your device.",
            color = Color(0xFFFF9800) // Orange
        ),
        
        // Page 5: Battery Optimization
        OnboardingPage(
            icon = Icons.Default.BatteryChargingFull,
            title = "Battery Optimization",
            description = "For reliable protection, we recommend disabling battery optimization. This ensures the VPN stays active even when your device is idle.",
            color = Color(0xFFFFC107), // Amber
            isBatteryPage = true
        ),
        
        // Page 6: Track Progress
        OnboardingPage(
            icon = Icons.Default.TrendingUp,
            title = "Track Your Progress",
            description = "See your blocked attempts, build streaks, and celebrate your journey to digital wellness.",
            color = Color(0xFF009688) // Teal
        )
    )
}

