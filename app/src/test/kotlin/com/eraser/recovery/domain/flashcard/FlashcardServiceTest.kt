package com.eraser.recovery.domain.flashcard

import com.eraser.recovery.data.local.dao.FlashcardDao
import com.eraser.recovery.data.local.dao.InterventionSessionDao
import com.eraser.recovery.data.local.entity.DifficultyLevel
import com.eraser.recovery.data.local.entity.FlashcardEntity
import com.eraser.recovery.data.local.entity.MessageCategory
import com.eraser.recovery.data.local.entity.TaskType
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * FlashcardService Test
 * 
 * Tests truly random flashcard selection algorithm.
 * 
 * Key Tests:
 * 1. Random selection returns different flashcards
 * 2. No sequential pattern in selection
 * 3. All flashcards eventually selected
 * 4. Recent history prevents immediate repeats
 * 5. Fisher-Yates shuffle produces different orders
 */
class FlashcardServiceTest {
    
    private lateinit var flashcardDao: FlashcardDao
    private lateinit var interventionSessionDao: InterventionSessionDao
    private lateinit var flashcardService: FlashcardService
    
    private val testFlashcards = listOf(
        FlashcardEntity(
            id = 1,
            flashcardId = "test_01",
            frontMessage = "Test message 1",
            backTask = "Test task 1",
            taskType = TaskType.REFLECTION,
            taskDuration = 5,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.GOAL_ORIENTED
        ),
        FlashcardEntity(
            id = 2,
            flashcardId = "test_02",
            frontMessage = "Test message 2",
            backTask = "Test task 2",
            taskType = TaskType.PHYSICAL,
            taskDuration = 10,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.EMPOWERMENT
        ),
        FlashcardEntity(
            id = 3,
            flashcardId = "test_03",
            frontMessage = "Test message 3",
            backTask = "Test task 3",
            taskType = TaskType.MINDFULNESS,
            taskDuration = 15,
            difficultyLevel = DifficultyLevel.HARD,
            messageCategory = MessageCategory.MINDFULNESS
        ),
        FlashcardEntity(
            id = 4,
            flashcardId = "test_04",
            frontMessage = "Test message 4",
            backTask = "Test task 4",
            taskType = TaskType.CREATIVE,
            taskDuration = 20,
            difficultyLevel = DifficultyLevel.EASY,
            messageCategory = MessageCategory.CONSEQUENCE_AWARENESS
        ),
        FlashcardEntity(
            id = 5,
            flashcardId = "test_05",
            frontMessage = "Test message 5",
            backTask = "Test task 5",
            taskType = TaskType.SOCIAL,
            taskDuration = 25,
            difficultyLevel = DifficultyLevel.MEDIUM,
            messageCategory = MessageCategory.REDIRECTION
        )
    )
    
    @Before
    fun setup() {
        flashcardDao = mock()
        interventionSessionDao = mock()
        flashcardService = FlashcardService(flashcardDao, interventionSessionDao)
    }
    
    @Test
    fun `getRandomFlashcard returns a flashcard`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When
        val result = flashcardService.getRandomFlashcard()
        
        // Then
        assertNotNull(result)
        assertTrue(testFlashcards.contains(result))
        verify(flashcardDao).incrementShown(eq(result.flashcardId), any())
    }
    
    @Test
    fun `getRandomFlashcard returns different flashcards over multiple calls`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When - Get 20 random flashcards
        val selectedFlashcards = mutableListOf<FlashcardEntity>()
        repeat(20) {
            flashcardService.getRandomFlashcard()?.let { selectedFlashcards.add(it) }
        }
        
        // Then - Should have selected multiple different flashcards
        val uniqueFlashcards = selectedFlashcards.map { it.flashcardId }.distinct()
        assertTrue(uniqueFlashcards.size > 1, "Expected multiple different flashcards, got ${uniqueFlashcards.size}")
        
        println("Selected ${uniqueFlashcards.size} unique flashcards out of ${testFlashcards.size} total")
    }
    
    @Test
    fun `getRandomFlashcard is not sequential`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When - Get 10 random flashcards
        val selectedIds = mutableListOf<String>()
        repeat(10) {
            flashcardService.getRandomFlashcard()?.let { selectedIds.add(it.flashcardId) }
        }
        
        // Then - Should NOT be in sequential order (test_01, test_02, test_03, ...)
        val isSequential = selectedIds.zipWithNext().all { (a, b) ->
            val aNum = a.substringAfter("_").toInt()
            val bNum = b.substringAfter("_").toInt()
            bNum == aNum + 1
        }
        
        assertTrue(!isSequential, "Flashcards should NOT be sequential")
        println("Selected flashcards: $selectedIds")
    }
    
    @Test
    fun `getRandomFlashcard avoids immediate repeats`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When - Get 10 random flashcards
        val selectedIds = mutableListOf<String>()
        repeat(10) {
            flashcardService.getRandomFlashcard()?.let { selectedIds.add(it.flashcardId) }
        }
        
        // Then - No immediate consecutive repeats
        val hasImmediateRepeat = selectedIds.zipWithNext().any { (a, b) -> a == b }
        assertTrue(!hasImmediateRepeat, "Should not have immediate consecutive repeats")
        
        println("Selected flashcards: $selectedIds")
    }
    
    @Test
    fun `getRandomFlashcard eventually selects all flashcards`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When - Get many random flashcards
        val selectedIds = mutableSetOf<String>()
        repeat(100) {
            flashcardService.getRandomFlashcard()?.let { selectedIds.add(it.flashcardId) }
        }
        
        // Then - Should have selected all flashcards at least once
        assertEquals(testFlashcards.size, selectedIds.size, "All flashcards should be selected eventually")
        
        println("Selected all ${selectedIds.size} flashcards")
    }
    
    @Test
    fun `getShuffledFlashcards returns all flashcards in random order`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When
        val shuffled = flashcardService.getShuffledFlashcards()
        
        // Then
        assertEquals(testFlashcards.size, shuffled.size)
        assertTrue(shuffled.containsAll(testFlashcards))
        
        // Should be in different order (with high probability)
        val isDifferentOrder = shuffled.zip(testFlashcards).any { (a, b) -> a.flashcardId != b.flashcardId }
        assertTrue(isDifferentOrder, "Shuffled list should be in different order")
        
        println("Original: ${testFlashcards.map { it.flashcardId }}")
        println("Shuffled: ${shuffled.map { it.flashcardId }}")
    }
    
    @Test
    fun `getShuffledFlashcards produces different orders on multiple calls`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When - Shuffle 5 times
        val shuffledLists = mutableListOf<List<String>>()
        repeat(5) {
            val shuffled = flashcardService.getShuffledFlashcards()
            shuffledLists.add(shuffled.map { it.flashcardId })
        }
        
        // Then - Should have at least 2 different orders
        val uniqueOrders = shuffledLists.distinct()
        assertTrue(uniqueOrders.size > 1, "Should produce different orders on multiple shuffles")
        
        println("Unique shuffle orders: ${uniqueOrders.size} out of 5")
        shuffledLists.forEachIndexed { index, list ->
            println("Shuffle ${index + 1}: $list")
        }
    }
    
    @Test
    fun `getRandomFlashcard returns null when no flashcards available`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(emptyList())
        
        // When
        val result = flashcardService.getRandomFlashcard()
        
        // Then
        assertEquals(null, result)
    }
    
    @Test
    fun `clearRecentHistory resets selection pool`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getAllActiveList()).thenReturn(testFlashcards)
        
        // When - Select some flashcards
        repeat(5) {
            flashcardService.getRandomFlashcard()
        }
        
        // Clear history
        flashcardService.clearRecentHistory()
        
        // Then - Should be able to select any flashcard again
        val result = flashcardService.getRandomFlashcard()
        assertNotNull(result)
    }
    
    @Test
    fun `getStatistics returns correct data`() = runBlocking<Unit> {
        // Given
        whenever(flashcardDao.getActiveCount()).thenReturn(60)
        whenever(flashcardDao.getAverageEffectiveness()).thenReturn(75.5)
        whenever(flashcardDao.getTopEffective(5)).thenReturn(testFlashcards.take(5))
        whenever(flashcardDao.getLeastShown(5)).thenReturn(testFlashcards.take(5))
        
        // When
        val stats = flashcardService.getStatistics()
        
        // Then
        assertEquals(60, stats["totalFlashcards"])
        assertEquals(75.5, stats["averageEffectiveness"])
        assertEquals(5, stats["topEffectiveCount"])
        assertEquals(5, stats["leastShownCount"])
    }
}

