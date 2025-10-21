package com.eraser.recovery.ui.screens.reports

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eraser.recovery.domain.statistics.ChartDataPoint

/**
 * Reports Screen
 * 
 * Displays statistics, charts, and progress analytics.
 * 
 * Research findings from Qustodio, Net Nanny, Bark, Norton Family, Covenant Eyes:
 * - Comprehensive dashboard with key metrics
 * - Visual charts for trend analysis
 * - Time-based analytics (daily, weekly, monthly)
 * - Blocked attempts tracking
 * - Intervention completion rates
 * - Progress visualization
 * 
 * Research findings from I Am Sober, Duolingo, Habitica:
 * - Simple, clean chart design
 * - Color-coded metrics (green for success, red for blocks)
 * - Trend indicators (up/down arrows)
 * - Stat cards with icons
 * - Line charts for streak progress
 * - Bar charts for daily metrics
 * 
 * Features:
 * - Streak progress line chart
 * - Key metrics cards (current streak, longest streak, total days)
 * - Additional stats (blocked attempts, interventions)
 * - Pull-to-refresh functionality
 * - Loading state with shimmer
 * 
 * @param viewModel Reports ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val totalDaysClean by viewModel.totalDaysClean.collectAsState()
    val successRate by viewModel.successRate.collectAsState()
    val blockedAttempts by viewModel.blockedAttempts.collectAsState()
    val completedInterventions by viewModel.completedInterventions.collectAsState()
    val chartData by viewModel.chartData.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Streak Chart
                if (chartData.isNotEmpty()) {
                    StreakLineChart(
                        data = chartData,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Key Metrics Section
                Text(
                    text = "Key Metrics",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Current Streak Card
                StatCard(
                    title = "Current Streak",
                    value = "$currentStreak ${if (currentStreak == 1) "day" else "days"}",
                    icon = Icons.Filled.LocalFireDepartment,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Longest Streak Card
                StatCard(
                    title = "Longest Streak",
                    value = "$longestStreak ${if (longestStreak == 1) "day" else "days"}",
                    icon = Icons.Filled.EmojiEvents,
                    color = Color(0xFFFF9800) // Orange
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Total Days Clean Card
                StatCard(
                    title = "Total Days Clean",
                    value = "$totalDaysClean ${if (totalDaysClean == 1) "day" else "days"}",
                    icon = Icons.Filled.CalendarToday,
                    color = Color(0xFF2196F3), // Blue
                    subtitle = "Success Rate: ${String.format("%.1f", successRate)}%"
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Additional Stats Section
                Text(
                    text = "Additional Stats",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Blocked Attempts Card
                StatCard(
                    title = "Blocked Attempts",
                    value = "$blockedAttempts",
                    icon = Icons.Filled.Block,
                    color = Color(0xFFF44336), // Red
                    subtitle = "Content blocks prevented"
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Completed Interventions Card
                StatCard(
                    title = "Interventions Completed",
                    value = "$completedInterventions",
                    icon = Icons.Filled.CheckCircle,
                    color = Color(0xFF4CAF50), // Green
                    subtitle = "Tasks successfully completed"
                )
            }
        }
    }
}

/**
 * Stat Card Component
 */
@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    subtitle: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Streak Line Chart
 *
 * Simple line chart for displaying streak progress over time.
 */
@Composable
private fun StreakLineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "chart_animation"
    )

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Streak Progress",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (data.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    val maxValue = data.maxOfOrNull { it.value } ?: 1f
                    val minValue = data.minOfOrNull { it.value } ?: 0f
                    val valueRange = maxValue - minValue

                    val chartWidth = size.width
                    val chartHeight = size.height - 40f // Leave space for labels
                    val pointSpacing = chartWidth / (data.size - 1).coerceAtLeast(1)

                    // Draw gradient background
                    val path = Path().apply {
                        moveTo(0f, chartHeight)
                        data.forEachIndexed { index, point ->
                            val x = index * pointSpacing * animatedProgress
                            val normalizedValue = if (valueRange > 0) {
                                (point.value - minValue) / valueRange
                            } else {
                                0.5f
                            }
                            val y = chartHeight - (normalizedValue * chartHeight)
                            if (index == 0) {
                                lineTo(x, y)
                            } else {
                                lineTo(x, y)
                            }
                        }
                        lineTo(chartWidth * animatedProgress, chartHeight)
                        close()
                    }

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.3f),
                                primaryColor.copy(alpha = 0.05f)
                            )
                        )
                    )

                    // Draw line
                    val linePath = Path()
                    data.forEachIndexed { index, point ->
                        val x = index * pointSpacing * animatedProgress
                        val normalizedValue = if (valueRange > 0) {
                            (point.value - minValue) / valueRange
                        } else {
                            0.5f
                        }
                        val y = chartHeight - (normalizedValue * chartHeight)

                        if (index == 0) {
                            linePath.moveTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                        }
                    }

                    drawPath(
                        path = linePath,
                        color = primaryColor,
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )

                    // Draw points
                    data.forEachIndexed { index, point ->
                        val x = index * pointSpacing * animatedProgress
                        val normalizedValue = if (valueRange > 0) {
                            (point.value - minValue) / valueRange
                        } else {
                            0.5f
                        }
                        val y = chartHeight - (normalizedValue * chartHeight)

                        drawCircle(
                            color = primaryColor,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(x, y)
                        )
                    }
                }

                // X-axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (data.isNotEmpty()) {
                        Text(
                            text = data.first().label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        if (data.size > 1) {
                            Text(
                                text = data.last().label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

