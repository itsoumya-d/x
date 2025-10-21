package com.eraser.recovery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.eraser.recovery.ui.navigation.EraserNavHost
import com.eraser.recovery.ui.theme.EraserTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity
 * 
 * Main entry point for the Eraser app.
 * Sets up Jetpack Compose UI with navigation.
 * 
 * Research findings from Qustodio, Net Nanny, Bark:
 * - Use edge-to-edge display for modern look
 * - Implement splash screen for smooth startup
 * - Keep activity lightweight, delegate to Compose
 * - Use Hilt for dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            EraserTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EraserNavHost()
                }
            }
        }
    }
}

