package com.eraser.recovery.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Onboarding Screen
 *
 * First-time setup wizard explaining VPN permission, privacy, and how blocking works.
 *
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - 6-page onboarding flow is optimal for user education
 * - Progressive permission requests (ask when needed)
 * - Skip button for flexibility
 * - Visual icons and colors help with comprehension
 * - Smooth animations improve user experience
 * - Battery optimization exemption for VPN reliability
 *
 * Features:
 * - HorizontalPager for page navigation
 * - Page indicators
 * - Skip button
 * - VPN permission request on appropriate page
 * - Battery optimization prompt
 * - Smooth animations
 *
 * @param onOnboardingComplete Callback when onboarding is complete
 * @param viewModel Onboarding ViewModel
 */
@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState()
    val coroutineScope = rememberCoroutineScope()

    val currentPage by viewModel.currentPage.collectAsState()
    val batteryOptimizationGranted by viewModel.batteryOptimizationGranted.collectAsState()
    val vpnPermissionGranted by viewModel.vpnPermissionGranted.collectAsState()

    val pages = OnboardingPages.pages

    // VPN permission launcher
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onVpnPermissionGranted()
        }
    }

    // Battery optimization launcher
    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBatteryOptimizationStatus()
    }

    // Update current page in ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.updatePage(pagerState.currentPage)
    }

    Scaffold(
        topBar = {
            // Skip button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                TextButton(
                    onClick = {
                        viewModel.completeOnboarding()
                        onOnboardingComplete()
                    }
                ) {
                    Text("Skip")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // HorizontalPager
            HorizontalPager(
                count = pages.size,
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(
                    page = pages[page],
                    batteryOptimizationGranted = batteryOptimizationGranted,
                    vpnPermissionGranted = vpnPermissionGranted,
                    onRequestVpnPermission = {
                        val intent = viewModel.prepareVpnPermission()
                        if (intent != null) {
                            vpnPermissionLauncher.launch(intent)
                        }
                    },
                    onRequestBatteryOptimization = {
                        val intent = viewModel.prepareBatteryOptimizationIntent()
                        batteryOptimizationLauncher.launch(intent)
                    }
                )
            }

            // Page indicators
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(16.dp),
                activeColor = MaterialTheme.colorScheme.primary,
                inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                indicatorWidth = 8.dp,
                indicatorHeight = 8.dp,
                spacing = 8.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                AnimatedVisibility(
                    visible = pagerState.currentPage > 0,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back")
                    }
                }

                if (pagerState.currentPage == 0) {
                    Spacer(modifier = Modifier.width(100.dp))
                }

                // Next/Get Started button
                Button(
                    onClick = {
                        if (pagerState.currentPage == pages.size - 1) {
                            viewModel.completeOnboarding()
                            onOnboardingComplete()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.size - 1) "Get Started" else "Next",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Onboarding Page Content
 *
 * Displays the content for a single onboarding page.
 *
 * @param page Onboarding page data
 * @param batteryOptimizationGranted Whether battery optimization is granted
 * @param vpnPermissionGranted Whether VPN permission is granted
 * @param onRequestVpnPermission Callback to request VPN permission
 * @param onRequestBatteryOptimization Callback to request battery optimization
 */
@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    batteryOptimizationGranted: Boolean,
    vpnPermissionGranted: Boolean,
    onRequestVpnPermission: () -> Unit,
    onRequestBatteryOptimization: () -> Unit
) {
    // Animation state
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon with gradient background
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = tween(600)
            )
        ) {
            IconWithGradient(
                icon = page.icon,
                color = page.color
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 200)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(600, delayMillis = 200)
            )
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 400)) + slideInVertically(
                initialOffsetY = { it / 4 },
                animationSpec = tween(600, delayMillis = 400)
            )
        ) {
            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // VPN Permission button (only on VPN permission page)
        if (page.isVpnPermissionPage) {
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(600, delayMillis = 600)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onRequestVpnPermission,
                        enabled = !vpnPermissionGranted,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (vpnPermissionGranted) Color(0xFF4CAF50) else page.color,
                            disabledContainerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = if (vpnPermissionGranted) Icons.Default.Check else page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (vpnPermissionGranted) "Permission Granted" else "Grant VPN Permission",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (vpnPermissionGranted) "✓ VPN permission granted" else "Tap to grant VPN permission",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (vpnPermissionGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Battery optimization button (only on battery page)
        if (page.isBatteryPage) {
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 600)) + slideInVertically(
                    initialOffsetY = { it / 4 },
                    animationSpec = tween(600, delayMillis = 600)
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = onRequestBatteryOptimization,
                        enabled = !batteryOptimizationGranted,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (batteryOptimizationGranted) Color(0xFF4CAF50) else page.color,
                            disabledContainerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = if (batteryOptimizationGranted) Icons.Default.Check else page.icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (batteryOptimizationGranted) "Exemption Granted" else "Grant Exemption",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (batteryOptimizationGranted) "✓ Your VPN will stay active reliably" else "Tap to disable battery optimization",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (batteryOptimizationGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center
                    )

                    if (!batteryOptimizationGranted) {
                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { /* Skip - do nothing */ }
                        ) {
                            Text("Skip (Not Recommended)")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Icon with Gradient Background
 *
 * Displays an icon with an animated gradient background.
 *
 * @param icon Icon to display
 * @param color Theme color
 */
@Composable
fun IconWithGradient(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    // Shimmer animation
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    // Scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        color,
                        color.copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shimmer overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = shimmerAlpha))
        )

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Color.White
        )
    }
}

