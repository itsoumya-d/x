package com.eraser.recovery.domain.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import com.eraser.recovery.data.local.dao.UserDao
import com.eraser.recovery.vpn.CustomVpnService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDao: UserDao
) {

    companion object {
        private const val TAG = "VpnManager"
        const val VPN_PERMISSION_REQUEST_CODE = 100
    }

    private val _isVpnEnabled = MutableStateFlow(false)
    val isVpnEnabled: Flow<Boolean> = _isVpnEnabled.asStateFlow()

    private val _isVpnConnecting = MutableStateFlow(false)
    val isVpnConnecting: Flow<Boolean> = _isVpnConnecting.asStateFlow()

    private val _vpnError = MutableStateFlow<String?>(null)
    val vpnError: Flow<String?> = _vpnError.asStateFlow()

    fun requestVpnPermission(activity: Activity): Intent? {
        val intent = VpnService.prepare(activity)
        if (intent != null) {
            Log.d(TAG, "VPN permission required")
        } else {
            Log.d(TAG, "VPN permission already granted")
        }
        return intent
    }

    fun hasVpnPermission(): Boolean {
        val intent = VpnService.prepare(context)
        return intent == null
    }

    suspend fun startVpn(): Boolean {
        if (!hasVpnPermission()) {
            Log.e(TAG, "Cannot start VPN: permission not granted")
            _vpnError.value = "VPN permission not granted"
            return false
        }

        try {
            Log.i(TAG, "Starting VPN service...")
            _isVpnConnecting.value = true
            _vpnError.value = null

            val intent = Intent(context, CustomVpnService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            val now = java.time.LocalDateTime.now()
            userDao.updateVpnStatus(true, now.toEpochSecond(java.time.ZoneOffset.UTC))

            _isVpnEnabled.value = true
            _isVpnConnecting.value = false

            Log.i(TAG, "VPN service started successfully")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error starting VPN service", e)
            _vpnError.value = "Failed to start VPN: ${e.message}"
            _isVpnConnecting.value = false
            return false
        }
    }

    suspend fun stopVpn() {
        try {
            Log.i(TAG, "Stopping VPN service...")

            val intent = Intent(context, CustomVpnService::class.java)
            context.stopService(intent)

            val now = java.time.LocalDateTime.now()
            userDao.updateVpnStatus(false, now.toEpochSecond(java.time.ZoneOffset.UTC))

            _isVpnEnabled.value = false
            _vpnError.value = null

            Log.i(TAG, "VPN service stopped successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping VPN service", e)
            _vpnError.value = "Failed to stop VPN: ${e.message}"
        }
    }

    suspend fun toggleVpn(): Boolean {
        return if (_isVpnEnabled.value) {
            stopVpn()
            false
        } else {
            startVpn()
        }
    }

    fun clearError() {
        _vpnError.value = null
    }
}