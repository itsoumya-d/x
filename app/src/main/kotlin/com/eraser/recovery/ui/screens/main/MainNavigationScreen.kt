package com.eraser.recovery.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.eraser.recovery.ui.navigation.Screen
import com.eraser.recovery.ui.screens.home.HomeScreen
import com.eraser.recovery.ui.screens.reports.ReportsScreen
import com.eraser.recovery.ui.screens.rewards.RewardsScreen

/**
 * Main Navigation Screen
 * 
 * Container screen with bottom navigation bar.
 * Manages navigation between Home, Rewards, and Reports tabs.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family:
 * - Bottom navigation is standard for 3-5 primary destinations
 * - Use Material 3 NavigationBar for modern look
 * - Preserve state across tab switches (IndexedStack pattern)
 * - Smooth tab transitions with fade animations
 * - Selected/unselected icon variants for better UX
 * - Clear labels for accessibility
 * 
 * Material Design 3 Best Practices:
 * - NavigationBar at bottom for easy thumb access
 * - 3 items is optimal (not too few, not too many)
 * - Icons should be recognizable and distinct
 * - Selected state should be clearly visible
 * - Smooth transitions between tabs
 */
@Composable
fun MainNavigationScreen(
    navController: NavController = rememberNavController()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Home Tab
            AnimatedVisibility(
                visible = selectedTab == 0,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                HomeScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToBlockedHistory = {
                        navController.navigate(Screen.BlockedHistory.route)
                    },
                    onNavigateToDailyCheckIn = {
                        navController.navigate(Screen.DailyCheckIn.route)
                    }
                )
            }
            
            // Rewards Tab
            AnimatedVisibility(
                visible = selectedTab == 1,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                RewardsScreen(
                    onNavigateToBadgeDetail = { achievement ->
                        // TODO: Navigate to badge detail screen in Phase 4.2
                    }
                )
            }
            
            // Reports Tab
            AnimatedVisibility(
                visible = selectedTab == 2,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                ReportsScreen()
            }
        }
    }
}

/**
 * Bottom Navigation Bar
 * 
 * Material 3 NavigationBar with 3 tabs.
 */
@Composable
private fun BottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf(
        NavigationTab(
            label = "Home",
            selectedIcon = Icons.Filled.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        NavigationTab(
            label = "Rewards",
            selectedIcon = Icons.Filled.EmojiEvents,
            unselectedIcon = Icons.Outlined.EmojiEvents
        ),
        NavigationTab(
            label = "Reports",
            selectedIcon = Icons.Filled.BarChart,
            unselectedIcon = Icons.Outlined.BarChart
        )
    )
    
    NavigationBar {
        tabs.forEachIndexed { index, tab ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == index) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) },
                alwaysShowLabel = true
            )
        }
    }
}

/**
 * Navigation Tab Data Class
 */
private data class NavigationTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Placeholder Screen
 * 
 * Temporary placeholder for screens not yet implemented.
 * Will be replaced in Phase 4.
 */
@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text = "$title Screen\n(Coming in Phase 4)",
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

