package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val sittingDurationMinutes: Int = 25,
    val isCustomDuration: Boolean = false,
    val customDurationMinutes: Int = 25,
    val reminderSound: String = "Default", // Default, Bell, Soft Chime, Nature, Digital Beep
    val isVibrationEnabled: Boolean = true,
    val selectedTheme: String = "Green Nature", // Light, Dark, System Default, Green Nature, Blue Ocean, Purple Night
    val languageCode: String = "fa" // "fa" or "en"
)

@Entity(tableName = "sitting_sessions")
data class SittingSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val sittingDurationSeconds: Long,
    val standingDurationSeconds: Long,
    val standingSuccess: Boolean,
    val dateString: String // yyyy-MM-dd
)

@Entity(tableName = "daily_expenses")
data class DailyExpense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val category: String, // Health, Gym, Food, Workspace, Garden, Other
    val description: String,
    val date: Long,
    val dateString: String // yyyy-MM-dd
)

@Entity(tableName = "garden_state")
data class GardenState(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val coins: Int = 50,
    val xp: Int = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val lastStandingTime: Long = 0,
    val selectedTheme: String = "Spring", // Spring, Summer, Autumn, Winter, Japanese Garden, Minimal, Tropical Forest
    val plantedTreesCount: Int = 0,
    val successfulBreaksCount: Int = 0
)

@Entity(tableName = "garden_decorations")
data class GardenDecoration(
    @PrimaryKey val id: String, // "flowers", "grass", "bench", "pond", "birds", "butterflies", "fountain", "statue", "lights", "stone_path"
    val displayNamePersian: String,
    val displayNameEnglish: String,
    val cost: Int,
    val purchased: Boolean = false,
    val count: Int = 0,
    val iconName: String
)

@Entity(tableName = "daily_missions")
data class DailyMission(
    @PrimaryKey val id: String, // "stand_5", "no_long_sit", "streak_3"
    val titlePersian: String,
    val titleEnglish: String,
    val targetCount: Int,
    val currentCount: Int,
    val completed: Boolean = false,
    val rewardCoins: Int,
    val rewardXp: Int
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey val id: String, // "first_stand", "one_week_streak", "100_breaks", "healthy_worker", "forest_builder"
    val titlePersian: String,
    val titleEnglish: String,
    val descriptionPersian: String,
    val descriptionEnglish: String,
    val completed: Boolean = false,
    val completedDate: Long? = null
)
