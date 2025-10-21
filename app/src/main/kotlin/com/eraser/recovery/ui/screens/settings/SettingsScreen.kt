package com.eraser.recovery.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings Screen
 * 
 * Main settings screen with preferences and configuration options.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family:
 * - Group settings into logical categories (Notifications, VPN, Theme, Data)
 * - Use Material 3 cards for visual separation
 * - Provide clear labels and descriptions
 * - Confirmation dialogs for destructive actions
 * - Easy access to app information
 * 
 * Material Design 3 Best Practices:
 * - Use switches for binary options
 * - Use list items for navigation
 * - Group related settings together
 * - Provide feedback for actions
 * - Clear visual hierarchy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetProgressDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications Section
            SettingsSection(
                title = "Notifications",
                icon = Icons.Filled.Notifications
            ) {
                SettingsSwitchItem(
                    title = "Daily Reminders",
                    description = "Receive daily motivation and check-in reminders",
                    checked = uiState.dailyRemindersEnabled,
                    onCheckedChange = { viewModel.toggleDailyReminders(it) }
                )
                
                SettingsSwitchItem(
                    title = "Streak Warnings",
                    description = "Get notified when your streak is at risk",
                    checked = uiState.streakWarningsEnabled,
                    onCheckedChange = { viewModel.toggleStreakWarnings(it) }
                )
                
                SettingsSwitchItem(
                    title = "Achievement Unlocks",
                    description = "Celebrate when you unlock new achievements",
                    checked = uiState.achievementNotificationsEnabled,
                    onCheckedChange = { viewModel.toggleAchievementNotifications(it) }
                )
            }
            
            // VPN Settings Section
            SettingsSection(
                title = "VPN & Protection",
                icon = Icons.Filled.Security
            ) {
                SettingsSwitchItem(
                    title = "Auto-Start on Boot",
                    description = "Automatically start VPN when device boots",
                    checked = uiState.autoStartOnBoot,
                    onCheckedChange = { viewModel.toggleAutoStartOnBoot(it) }
                )
                
                SettingsSwitchItem(
                    title = "Battery Optimization Exempt",
                    description = "Prevent system from stopping VPN to save battery",
                    checked = uiState.batteryOptimizationExempt,
                    onCheckedChange = { viewModel.toggleBatteryOptimization(it) }
                )
            }
            
            // Theme Section
            SettingsSection(
                title = "Appearance",
                icon = Icons.Filled.Brightness4
            ) {
                SettingsClickableItem(
                    title = "Theme",
                    description = uiState.themeMode.displayName,
                    onClick = { viewModel.showThemeDialog() }
                )
            }
            
            // Data Management Section
            SettingsSection(
                title = "Data Management",
                icon = Icons.Filled.Delete
            ) {
                SettingsClickableItem(
                    title = "Clear Blocked History",
                    description = "Remove all blocked attempt records",
                    onClick = { showClearHistoryDialog = true }
                )
                
                SettingsClickableItem(
                    title = "Reset Progress",
                    description = "Reset journey, streak, and achievements",
                    onClick = { showResetProgressDialog = true },
                    isDestructive = true
                )
            }
            
            // About Section
            SettingsSection(
                title = "About",
                icon = Icons.Filled.Info
            ) {
                SettingsClickableItem(
                    title = "App Version",
                    description = uiState.appVersion,
                    onClick = {}
                )
                
                SettingsClickableItem(
                    title = "Privacy Policy",
                    description = "View our privacy policy",
                    onClick = { viewModel.openPrivacyPolicy() }
                )
                
                SettingsClickableItem(
                    title = "Developer Info",
                    description = "About the developer",
                    onClick = { viewModel.openDeveloperInfo() }
                )
            }
        }
    }
    
    // Clear History Confirmation Dialog
    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = "Clear Blocked History?",
            message = "This will permanently delete all blocked attempt records. This action cannot be undone.",
            confirmText = "Clear",
            onConfirm = {
                viewModel.clearBlockedHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }
    
    // Reset Progress Confirmation Dialog
    if (showResetProgressDialog) {
        ConfirmationDialog(
            title = "Reset All Progress?",
            message = "This will reset your journey, streak, achievements, and all progress. This action cannot be undone.",
            confirmText = "Reset",
            onConfirm = {
                viewModel.resetProgress()
                showResetProgressDialog = false
            },
            onDismiss = { showResetProgressDialog = false },
            isDestructive = true
        )
    }
}

/**
 * Settings Section Card
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Section Content
            content()
        }
    }
}

/**
 * Settings Switch Item
 */
@Composable
private fun SettingsSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * Settings Clickable Item
 */
@Composable
private fun SettingsClickableItem(
    title: String,
    description: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Confirmation Dialog
 */
@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmText,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

