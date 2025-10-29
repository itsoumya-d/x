package com.eraser.recovery.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class BlocklistManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BlocklistManager"
        
        const val ACTION_UPDATE_BLOCKLIST = "com.eraser.recovery.UPDATE_BLOCKLIST"
        const val EXTRA_BLOCKED_DOMAINS = "blocked_domains"
        const val EXTRA_WHITELISTED_DOMAINS = "whitelisted_domains"
        
        private const val BLOCKLIST_FILE = "blocklist.json"
        private const val WHITELIST_FILE = "whitelist.json"
    }
    
    private val blockedDomains = ConcurrentHashMap.newKeySet<String>()
    private val whitelistedDomains = ConcurrentHashMap.newKeySet<String>()
    
    private val totalLookupsCount = AtomicLong(0)
    private val blockedLookupsCount = AtomicLong(0)
    private val whitelistedLookupsCount = AtomicLong(0)
    private val allowedLookupsCount = AtomicLong(0)
    
    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_UPDATE_BLOCKLIST) {
                val blocked = intent.getStringArrayExtra(EXTRA_BLOCKED_DOMAINS)
                val whitelisted = intent.getStringArrayExtra(EXTRA_WHITELISTED_DOMAINS)
                
                if (blocked != null || whitelisted != null) {
                    updateBlocklist(
                        blocked?.toList() ?: emptyList(),
                        whitelisted?.toList() ?: emptyList()
                    )
                    Log.i(TAG, "Blocklist updated via broadcast")
                }
            }
        }
    }
    
    private var isReceiverRegistered = false
    
    fun initialize() {
        try {
            if (!isReceiverRegistered) {
                val filter = IntentFilter(ACTION_UPDATE_BLOCKLIST)
                context.registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                isReceiverRegistered = true
                Log.d(TAG, "Broadcast receiver registered")
            }
            
            loadFromFiles()
            
            Log.i(TAG, "BlocklistManager initialized: ${blockedDomains.size} blocked, ${whitelistedDomains.size} whitelisted")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing BlocklistManager", e)
        }
    }
    
    fun shutdown() {
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(updateReceiver)
                isReceiverRegistered = false
                Log.d(TAG, "Broadcast receiver unregistered")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down BlocklistManager", e)
        }
    }
    
    fun updateBlocklist(blocked: List<String>, whitelisted: List<String>) {
        try {
            blockedDomains.clear()
            whitelistedDomains.clear()
            
            blockedDomains.addAll(blocked.map { it.lowercase() })
            whitelistedDomains.addAll(whitelisted.map { it.lowercase() })
            
            Log.i(TAG, "Blocklist updated: ${blockedDomains.size} blocked, ${whitelistedDomains.size} whitelisted")
            
            saveToFiles()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating blocklist", e)
        }
    }
    
    fun shouldBlockDomain(domain: String): Boolean {
        totalLookupsCount.incrementAndGet()
        
        val lowerDomain = domain.lowercase()
        
        if (isWhitelisted(lowerDomain)) {
            whitelistedLookupsCount.incrementAndGet()
            return false
        }
        
        if (isBlocked(lowerDomain)) {
            blockedLookupsCount.incrementAndGet()
            return true
        }
        
        allowedLookupsCount.incrementAndGet()
        return false
    }
    
    fun getStatistics(): Map<String, Any> {
        return mapOf(
            "blockedDomainsCount" to blockedDomains.size,
            "whitelistedDomainsCount" to whitelistedDomains.size,
            "totalLookups" to totalLookupsCount.get(),
            "blockedLookups" to blockedLookupsCount.get(),
            "whitelistedLookups" to whitelistedLookupsCount.get(),
            "allowedLookups" to allowedLookupsCount.get()
        )
    }
    
    fun getBlockedDomains(limit: Int = 100): List<String> {
        return blockedDomains.take(limit)
    }
    
    fun getWhitelistedDomains(limit: Int = 100): List<String> {
        return whitelistedDomains.take(limit)
    }
    
    private fun isWhitelisted(domain: String): Boolean {
        if (whitelistedDomains.contains(domain)) {
            return true
        }
        
        val parts = domain.split(".")
        for (i in 1 until parts.size) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (whitelistedDomains.contains(parentDomain)) {
                return true
            }
        }
        
        return false
    }
    
    private fun isBlocked(domain: String): Boolean {
        if (blockedDomains.contains(domain)) {
            return true
        }
        
        val parts = domain.split(".")
        for (i in 1 until parts.size) {
            val parentDomain = parts.subList(i, parts.size).joinToString(".")
            if (blockedDomains.contains(parentDomain)) {
                return true
            }
        }
        
        return false
    }
    
    private fun loadFromFiles() {
        try {
            val blockedFile = File(context.filesDir, BLOCKLIST_FILE)
            if (blockedFile.exists()) {
                val json = blockedFile.readText()
                val jsonArray = JSONArray(json)
                val domains = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    domains.add(jsonArray.getString(i))
                }
                blockedDomains.addAll(domains.map { it.lowercase() })
                Log.d(TAG, "Loaded ${domains.size} blocked domains from file")
            }
            
            val whitelistFile = File(context.filesDir, WHITELIST_FILE)
            if (whitelistFile.exists()) {
                val json = whitelistFile.readText()
                val jsonArray = JSONArray(json)
                val domains = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    domains.add(jsonArray.getString(i))
                }
                whitelistedDomains.addAll(domains.map { it.lowercase() })
                Log.d(TAG, "Loaded ${domains.size} whitelisted domains from file")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not load blocklist from files (this is normal on first run)", e)
        }
    }
    
    private fun saveToFiles() {
        try {
            val blockedFile = File(context.filesDir, BLOCKLIST_FILE)
            val blockedJson = JSONArray(blockedDomains.toList())
            blockedFile.writeText(blockedJson.toString())
            
            val whitelistFile = File(context.filesDir, WHITELIST_FILE)
            val whitelistJson = JSONArray(whitelistedDomains.toList())
            whitelistFile.writeText(whitelistJson.toString())
            
            Log.d(TAG, "Saved blocklist to files")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving blocklist to files", e)
        }
    }
}