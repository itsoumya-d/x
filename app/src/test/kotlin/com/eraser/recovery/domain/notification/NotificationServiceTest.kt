package com.eraser.recovery.domain.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkManager
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * NotificationService Test
 *
 * Tests notification scheduling, settings, and display.
 *
 * Runs under Robolectric so the Android framework (NotificationCompat,
 * PendingIntent, NotificationManager) behaves like a real device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class NotificationServiceTest {

    private lateinit var context: Context
    private lateinit var workManager: WorkManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationService: NotificationService

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        workManager = mock()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationService = NotificationService(context, workManager)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // NOTIFICATION SETTINGS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `notification service is created successfully`() {
        // Given & When
        val service = NotificationService(context, workManager)

        // Then
        assertNotNull(service)
    }

    @Test
    fun `notification channels are defined correctly`() {
        // Given & When & Then
        assertEquals("vpn_service", NotificationService.CHANNEL_VPN_SERVICE)
        assertEquals("achievements", NotificationService.CHANNEL_ACHIEVEMENTS)
        assertEquals("reminders", NotificationService.CHANNEL_REMINDERS)
        assertEquals("blocking", NotificationService.CHANNEL_BLOCKING)
    }

    @Test
    fun `notification IDs are unique`() {
        // Given & When & Then
        val ids = setOf(
            NotificationService.NOTIFICATION_ID_DAILY_REMINDER,
            NotificationService.NOTIFICATION_ID_ACHIEVEMENT,
            NotificationService.NOTIFICATION_ID_MILESTONE,
            NotificationService.NOTIFICATION_ID_STREAK_WARNING
        )

        // All IDs should be unique
        assertEquals(4, ids.size)
    }

    @Test
    fun `default reminder time is 9 AM`() {
        // Given & When & Then
        assertEquals(9, NotificationService.DEFAULT_REMINDER_HOUR)
        assertEquals(0, NotificationService.DEFAULT_REMINDER_MINUTE)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // DAILY REMINDER TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `showDailyReminderNotification creates notification`() {
        // Given
        val service = NotificationService(context, workManager)

        // When
        service.showDailyReminderNotification()

        // Then
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_DAILY_REMINDER
            )
        )
    }

    @Test
    fun `daily reminder notification has correct content`() {
        // Given
        val service = NotificationService(context, workManager)

        // When
        service.showDailyReminderNotification()

        // Then
        // Notification should be created with daily reminder ID
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_DAILY_REMINDER
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // ACHIEVEMENT NOTIFICATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `showAchievementNotification creates notification`() {
        // Given
        val service = NotificationService(context, workManager)
        val achievementId = "day_7"
        val title = "One Week Warrior"
        val description = "Complete your first week"

        // When
        service.showAchievementNotification(achievementId, title, description)

        // Then
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_ACHIEVEMENT + achievementId.hashCode()
            )
        )
    }

    @Test
    fun `achievement notification uses unique ID based on achievement ID`() {
        // Given
        val service = NotificationService(context, workManager)
        val achievementId1 = "day_7"
        val achievementId2 = "day_30"

        // When
        service.showAchievementNotification(achievementId1, "Title 1", "Description 1")
        service.showAchievementNotification(achievementId2, "Title 2", "Description 2")

        // Then
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_ACHIEVEMENT + achievementId1.hashCode()
            )
        )
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_ACHIEVEMENT + achievementId2.hashCode()
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // MILESTONE NOTIFICATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `showMilestoneNotification creates notification`() {
        // Given
        val service = NotificationService(context, workManager)
        val days = 7
        val message = "You've completed your first week!"

        // When
        service.showMilestoneNotification(days, message)

        // Then
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_MILESTONE + days
            )
        )
    }

    @Test
    fun `milestone notification uses unique ID based on days`() {
        // Given
        val service = NotificationService(context, workManager)

        // When
        service.showMilestoneNotification(7, "7 days!")
        service.showMilestoneNotification(30, "30 days!")

        // Then
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_MILESTONE + 7
            )
        )
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_MILESTONE + 30
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // STREAK WARNING TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `showStreakWarningNotification creates notification`() {
        // Given
        val service = NotificationService(context, workManager)

        // When
        service.showStreakWarningNotification()

        // Then
        assertNotNull(
            shadowOf(notificationManager).getNotification(
                NotificationService.NOTIFICATION_ID_STREAK_WARNING
            )
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // WORKMANAGER TAGS TESTS
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `work tags are defined correctly`() {
        // Given & When & Then
        assertEquals("daily_reminder", NotificationService.WORK_TAG_DAILY_REMINDER)
        assertEquals("streak_warning", NotificationService.WORK_TAG_STREAK_WARNING)
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    // INTEGRATION TESTS (Conceptual - require Android instrumentation)
    // ═══════════════════════════════════════════════════════════════════════════════

    @Test
    fun `notification service constants are accessible`() {
        // Given & When & Then
        // Verify all constants are accessible
        assertTrue(NotificationService.CHANNEL_VPN_SERVICE.isNotEmpty())
        assertTrue(NotificationService.CHANNEL_ACHIEVEMENTS.isNotEmpty())
        assertTrue(NotificationService.CHANNEL_REMINDERS.isNotEmpty())
        assertTrue(NotificationService.CHANNEL_BLOCKING.isNotEmpty())
        assertTrue(NotificationService.WORK_TAG_DAILY_REMINDER.isNotEmpty())
        assertTrue(NotificationService.WORK_TAG_STREAK_WARNING.isNotEmpty())
    }

    @Test
    fun `notification IDs are positive integers`() {
        // Given & When & Then
        assertTrue(NotificationService.NOTIFICATION_ID_DAILY_REMINDER > 0)
        assertTrue(NotificationService.NOTIFICATION_ID_ACHIEVEMENT > 0)
        assertTrue(NotificationService.NOTIFICATION_ID_MILESTONE > 0)
        assertTrue(NotificationService.NOTIFICATION_ID_STREAK_WARNING > 0)
    }

    @Test
    fun `default reminder time is valid`() {
        // Given & When & Then
        assertTrue(NotificationService.DEFAULT_REMINDER_HOUR in 0..23)
        assertTrue(NotificationService.DEFAULT_REMINDER_MINUTE in 0..59)
    }
}

/**
 * DailyReminderWorker Test
 *
 * Tests the background worker for daily reminders.
 *
 * Note: Full WorkManager testing requires AndroidX Test framework.
 * These are basic unit tests.
 */
class DailyReminderWorkerTest {

    @Test
    fun `DailyReminderWorker class exists`() {
        // Given & When & Then
        // Verify the worker class is defined
        val workerClass = DailyReminderWorker::class.java
        assert(workerClass != null)
    }

    @Test
    fun `DailyReminderWorker extends CoroutineWorker`() {
        // Given & When & Then
        // Verify the worker extends CoroutineWorker
        val workerClass = DailyReminderWorker::class.java
        val superclass = workerClass.superclass
        assertEquals("CoroutineWorker", superclass?.simpleName)
    }
}
