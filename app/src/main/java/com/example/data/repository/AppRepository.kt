package com.example.data.repository

import com.example.data.db.AppDao
import com.example.data.models.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {

    // Settings
    val settings: Flow<SettingsEntity?> = appDao.getSettings()
    suspend fun getSettingsDirect(): SettingsEntity? = appDao.getSettingsDirect()
    suspend fun updateSettings(settings: SettingsEntity) = appDao.insertOrUpdateSettings(settings)

    // Sessions
    val allSessions: Flow<List<SittingSession>> = appDao.getAllSessions()
    val latestSession: Flow<SittingSession?> = appDao.getLatestSession()
    suspend fun insertSession(session: SittingSession) = appDao.insertSession(session)

    // Daily Expenses
    val allExpenses: Flow<List<DailyExpense>> = appDao.getAllExpenses()
    suspend fun insertExpense(expense: DailyExpense) = appDao.insertExpense(expense)
    suspend fun deleteExpense(id: Int) = appDao.deleteExpenseById(id)

    // Garden State
    val gardenState: Flow<GardenState?> = appDao.getGardenState()
    suspend fun getGardenStateDirect(): GardenState? = appDao.getGardenStateDirect()
    suspend fun updateGardenState(state: GardenState) = appDao.insertOrUpdateGardenState(state)

    // Decorations
    val decorations: Flow<List<GardenDecoration>> = appDao.getAllDecorations()
    suspend fun updateDecoration(decoration: GardenDecoration) = appDao.updateDecoration(decoration)
    suspend fun saveAllDecorations(list: List<GardenDecoration>) = appDao.insertDecorations(list)

    // Daily Missions
    val missions: Flow<List<DailyMission>> = appDao.getAllMissions()
    suspend fun getMissionsDirect(): List<DailyMission> = appDao.getAllMissionsDirect()
    suspend fun updateMission(mission: DailyMission) = appDao.updateMission(mission)
    suspend fun saveAllMissions(list: List<DailyMission>) = appDao.insertMissions(list)

    // Achievements
    val achievements: Flow<List<Achievement>> = appDao.getAllAchievements()
    suspend fun updateAchievement(achievement: Achievement) = appDao.updateAchievement(achievement)
    suspend fun saveAllAchievements(list: List<Achievement>) = appDao.insertAchievements(list)
}
