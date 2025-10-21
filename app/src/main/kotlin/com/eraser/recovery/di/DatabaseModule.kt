package com.eraser.recovery.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eraser.recovery.data.local.EraserDatabase
import com.eraser.recovery.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database Module
 * 
 * Provides database and DAO instances via Hilt dependency injection.
 * 
 * Research findings:
 * - Use singleton scope for database instance
 * - Enable WAL mode for better concurrent access
 * - Provide individual DAOs for clean architecture
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    /**
     * Provide Eraser Database instance
     */
    @Provides
    @Singleton
    fun provideEraserDatabase(
        @ApplicationContext context: Context
    ): EraserDatabase {
        return Room.databaseBuilder(
            context,
            EraserDatabase::class.java,
            EraserDatabase.DATABASE_NAME
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration() // TODO: Add proper migrations for production
            .build()
    }
    
    /**
     * Provide Flashcard DAO
     */
    @Provides
    @Singleton
    fun provideFlashcardDao(database: EraserDatabase): FlashcardDao {
        return database.flashcardDao()
    }
    
    /**
     * Provide User DAO
     */
    @Provides
    @Singleton
    fun provideUserDao(database: EraserDatabase): UserDao {
        return database.userDao()
    }
    
    /**
     * Provide Blocked Attempt DAO
     */
    @Provides
    @Singleton
    fun provideBlockedAttemptDao(database: EraserDatabase): BlockedAttemptDao {
        return database.blockedAttemptDao()
    }
    
    /**
     * Provide Achievement DAO
     */
    @Provides
    @Singleton
    fun provideAchievementDao(database: EraserDatabase): AchievementDao {
        return database.achievementDao()
    }
    
    /**
     * Provide Daily Log DAO
     */
    @Provides
    @Singleton
    fun provideDailyLogDao(database: EraserDatabase): DailyLogDao {
        return database.dailyLogDao()
    }
    
    /**
     * Provide Intervention Session DAO
     */
    @Provides
    @Singleton
    fun provideInterventionSessionDao(database: EraserDatabase): InterventionSessionDao {
        return database.interventionSessionDao()
    }
}

