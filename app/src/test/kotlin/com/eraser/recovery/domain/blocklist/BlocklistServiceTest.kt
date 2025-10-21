package com.eraser.recovery.domain.blocklist

import android.content.Context
import com.eraser.recovery.domain.vpn.VpnManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * BlocklistService Test
 * 
 * Tests domain filtering with multiple matching strategies:
 * 1. Exact match
 * 2. Parent domain matching
 * 3. Wildcard patterns
 * 4. Keyword matching
 * 5. Whitelist checking
 */
class BlocklistServiceTest {
    
    private lateinit var context: Context
    private lateinit var vpnManager: VpnManager
    private lateinit var blocklistService: BlocklistService
    
    @Before
    fun setup() {
        context = mock()
        vpnManager = mock()
        blocklistService = BlocklistService(context, vpnManager)
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // EXACT MATCH TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `shouldBlock returns true for exact domain match`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("example.com")
        
        // When
        val result = blocklistService.shouldBlock("example.com")
        
        // Then
        assertTrue(result, "Exact domain match should be blocked")
    }
    
    @Test
    fun `shouldBlock returns false for non-blocked domain`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("example.com")
        
        // When
        val result = blocklistService.shouldBlock("google.com")
        
        // Then
        assertFalse(result, "Non-blocked domain should not be blocked")
    }
    
    @Test
    fun `shouldBlock is case-insensitive`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("example.com")
        
        // When & Then
        assertTrue(blocklistService.shouldBlock("EXAMPLE.COM"))
        assertTrue(blocklistService.shouldBlock("Example.Com"))
        assertTrue(blocklistService.shouldBlock("eXaMpLe.CoM"))
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // PARENT DOMAIN MATCHING TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `shouldBlock matches subdomain when parent domain is blocked`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("example.com")
        
        // When & Then
        assertTrue(blocklistService.shouldBlock("sub.example.com"), "Subdomain should be blocked")
        assertTrue(blocklistService.shouldBlock("a.b.example.com"), "Multi-level subdomain should be blocked")
        assertTrue(blocklistService.shouldBlock("deep.nested.sub.example.com"), "Deep subdomain should be blocked")
    }
    
    @Test
    fun `shouldBlock does not match unrelated domains`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("example.com")
        
        // When & Then
        assertFalse(blocklistService.shouldBlock("notexample.com"), "Different domain should not be blocked")
        assertFalse(blocklistService.shouldBlock("example.org"), "Different TLD should not be blocked")
        assertFalse(blocklistService.shouldBlock("examplecom.net"), "Similar name should not be blocked")
    }
    
    @Test
    fun `shouldBlock matches multiple subdomain levels`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("badsite.com")
        
        // When & Then
        assertTrue(blocklistService.shouldBlock("www.badsite.com"))
        assertTrue(blocklistService.shouldBlock("api.badsite.com"))
        assertTrue(blocklistService.shouldBlock("cdn.api.badsite.com"))
        assertTrue(blocklistService.shouldBlock("a.b.c.d.badsite.com"))
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // WILDCARD PATTERN TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `shouldBlock matches wildcard patterns`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("*.example.com")
        
        // When & Then
        assertTrue(blocklistService.shouldBlock("sub.example.com"), "Subdomain should match wildcard")
        assertTrue(blocklistService.shouldBlock("www.example.com"), "www subdomain should match wildcard")
        assertTrue(blocklistService.shouldBlock("api.example.com"), "api subdomain should match wildcard")
        assertTrue(blocklistService.shouldBlock("a.b.example.com"), "Multi-level should match wildcard")
    }
    
    @Test
    fun `shouldBlock wildcard matches root domain`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("*.example.com")
        
        // When
        val result = blocklistService.shouldBlock("example.com")
        
        // Then
        assertTrue(result, "Root domain should match wildcard pattern")
    }
    
    @Test
    fun `shouldBlock wildcard does not match different domains`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("*.example.com")
        
        // When & Then
        assertFalse(blocklistService.shouldBlock("example.org"))
        assertFalse(blocklistService.shouldBlock("notexample.com"))
        assertFalse(blocklistService.shouldBlock("google.com"))
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // KEYWORD MATCHING TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `shouldBlock matches adult keywords`() = runBlocking {
        // When & Then - Adult keywords should be blocked
        assertTrue(blocklistService.shouldBlock("pornsite.com"), "Domain with 'porn' should be blocked")
        assertTrue(blocklistService.shouldBlock("xxxvideos.com"), "Domain with 'xxx' should be blocked")
        assertTrue(blocklistService.shouldBlock("adultvideo.com"), "Domain with 'adult' should be blocked")
        assertTrue(blocklistService.shouldBlock("sexchat.com"), "Domain with 'sex' should be blocked")
    }
    
    @Test
    fun `shouldBlock does not match non-adult keywords`() = runBlocking {
        // When & Then - Non-adult domains should NOT be blocked by keywords
        assertFalse(blocklistService.shouldBlock("facebook.com"), "Social media should not be blocked")
        assertFalse(blocklistService.shouldBlock("gambling.com"), "Gambling should not be blocked")
        assertFalse(blocklistService.shouldBlock("drugs.com"), "Drugs should not be blocked")
        assertFalse(blocklistService.shouldBlock("casino.com"), "Casino should not be blocked")
    }
    
    @Test
    fun `shouldBlock matches known adult site names`() = runBlocking {
        // When & Then - Known adult sites should be blocked by keyword
        assertTrue(blocklistService.shouldBlock("pornhub.com"))
        assertTrue(blocklistService.shouldBlock("xvideos.com"))
        assertTrue(blocklistService.shouldBlock("xhamster.com"))
        assertTrue(blocklistService.shouldBlock("redtube.com"))
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // WHITELIST TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `shouldBlock returns false for whitelisted domains`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("example.com")
        blocklistService.addWhitelistedDomain("example.com")
        
        // When
        val result = blocklistService.shouldBlock("example.com")
        
        // Then
        assertFalse(result, "Whitelisted domain should not be blocked")
    }
    
    @Test
    fun `shouldBlock whitelist takes precedence over blocklist`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("*.example.com")
        blocklistService.addWhitelistedDomain("safe.example.com")
        
        // When & Then
        assertTrue(blocklistService.shouldBlock("bad.example.com"), "Non-whitelisted subdomain should be blocked")
        assertFalse(blocklistService.shouldBlock("safe.example.com"), "Whitelisted subdomain should not be blocked")
    }
    
    @Test
    fun `shouldBlock whitelist applies to subdomains`() = runBlocking {
        // Given
        blocklistService.addWhitelistedDomain("google.com")
        
        // When & Then
        assertFalse(blocklistService.shouldBlock("google.com"))
        assertFalse(blocklistService.shouldBlock("www.google.com"))
        assertFalse(blocklistService.shouldBlock("mail.google.com"))
        assertFalse(blocklistService.shouldBlock("drive.google.com"))
    }
    
    @Test
    fun `shouldBlock whitelist overrides keyword matching`() = runBlocking {
        // Given - Domain contains adult keyword but is whitelisted
        blocklistService.addWhitelistedDomain("sexeducation.org")
        
        // When
        val result = blocklistService.shouldBlock("sexeducation.org")
        
        // Then
        assertFalse(result, "Whitelisted domain should not be blocked even with keyword match")
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // COMBINED STRATEGY TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `shouldBlock combines all matching strategies`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("blocked.com")
        blocklistService.addBlockedDomain("*.wildcard.com")
        blocklistService.addWhitelistedDomain("safe.com")
        
        // When & Then
        // Exact match
        assertTrue(blocklistService.shouldBlock("blocked.com"))
        
        // Parent domain match
        assertTrue(blocklistService.shouldBlock("sub.blocked.com"))
        
        // Wildcard match
        assertTrue(blocklistService.shouldBlock("any.wildcard.com"))
        
        // Keyword match
        assertTrue(blocklistService.shouldBlock("pornsite.net"))
        
        // Whitelist override
        assertFalse(blocklistService.shouldBlock("safe.com"))
        assertFalse(blocklistService.shouldBlock("www.safe.com"))
        
        // No match
        assertFalse(blocklistService.shouldBlock("google.com"))
    }
    
    // ═══════════════════════════════════════════════════════════════════════════════
    // STATISTICS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════
    
    @Test
    fun `getStatistics returns correct counts`() = runBlocking {
        // Given
        blocklistService.addBlockedDomain("blocked1.com")
        blocklistService.addBlockedDomain("blocked2.com")
        blocklistService.addWhitelistedDomain("safe1.com")
        blocklistService.addWhitelistedDomain("safe2.com")
        blocklistService.addWhitelistedDomain("safe3.com")
        
        // When
        val stats = blocklistService.getStatistics()
        
        // Then
        assertEquals(2, stats["blockedDomainsCount"])
        assertEquals(3, stats["whitelistedDomainsCount"])
    }
}

