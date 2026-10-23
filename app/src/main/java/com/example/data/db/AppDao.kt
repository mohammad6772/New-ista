package com.example.data.db

import androidx.room.*
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    // --- SETTINGS ---
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: SettingsEntity)

    // --- SITTING SESSIONS ---
    @Query("SELECT * FROM sitting_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SittingSession>>

    @Query("SELECT * FROM sitting_sessions ORDER BY startTime DESC LIMIT 1")
    fun getLatestSession(): Flow<SittingSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SittingSession)

    // --- DAILY EXPENSES ---
    @Query("SELECT * FROM daily_expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<DailyExpense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: DailyExpense)

    @Query("DELETE FROM daily_expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Int)

    // --- GARDEN STATE ---
    @Query("SELECT * FROM garden_state WHERE id = 1 LIMIT 1")
    fun getGardenState(): Flow<GardenState?>

    @Query("SELECT * FROM garden_state WHERE id = 1 LIMIT 1")
    suspend fun getGardenStateDirect(): GardenState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGardenState(state: GardenState)

    // --- DECORATIONS ---
    @Query("SELECT * FROM garden_decorations")
    fun getAllDecorations(): Flow<List<GardenDecoration>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecorations(decorations: List<GardenDecoration>)

    @Update
    suspend fun updateDecoration(decoration: GardenDecoration)

    // --- DAILY MISSIONS ---
    @Query("SELECT * FROM daily_missions")
    fun getAllMissions(): Flow<List<DailyMission>>

    @Query("SELECT * FROM daily_missions")
    suspend fun getAllMissionsDirect(): List<DailyMission>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMissions(missions: List<DailyMission>)

    @Update
    suspend fun updateMission(mission: DailyMission)

    // --- ACHIEVEMENTS ---
    @Query("SELECT * FROM achievements")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Update
    suspend fun updateAchievement(achievement: Achievement)
}
