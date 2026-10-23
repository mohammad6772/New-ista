package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.*
import com.example.data.repository.AppRepository
import com.example.service.TimerManager
import com.example.service.TimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class IstaViewModel(private val repository: AppRepository) : ViewModel() {

    // Timer States (Exposed directly from singleton background TimerManager)
    val timerState: StateFlow<TimerState> = TimerManager.timerState
    val sittingSeconds: StateFlow<Int> = TimerManager.sittingSeconds
    val sittingRemainingSeconds: StateFlow<Int> = TimerManager.sittingRemainingSeconds
    val standingSeconds: StateFlow<Int> = TimerManager.standingSeconds
    val motivationMessage: StateFlow<String> = TimerManager.motivationMessage

    // Persistent Room Database flows
    val settings: StateFlow<SettingsEntity> = repository.settings
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsEntity()
        )

    val sessions: StateFlow<List<SittingSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val expenses: StateFlow<List<DailyExpense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val gardenState: StateFlow<GardenState> = repository.gardenState
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GardenState()
        )

    val decorations: StateFlow<List<GardenDecoration>> = repository.decorations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val missions: StateFlow<List<DailyMission>> = repository.missions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val achievements: StateFlow<List<Achievement>> = repository.achievements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- TIMERS ACTIONS ---
    fun startSitting(context: Context) {
        val duration = if (settings.value.isCustomDuration) {
            settings.value.customDurationMinutes
        } else {
            settings.value.sittingDurationMinutes
        }
        TimerManager.startSitting(context, duration)
    }

    fun stopSitting(context: Context) {
        TimerManager.stopSitting(context)
    }

    fun triggerStandManually(context: Context) {
        TimerManager.triggerStandManually(context)
    }

    fun stopStandingManually(context: Context) {
        TimerManager.stopStandingManually(context)
    }

    fun snoozeSitting(context: Context, minutes: Int = 5) {
        TimerManager.snoozeSitting(context, minutes)
    }

    // --- SETTINGS ACTIONS ---
    fun updateVibration(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(settings.value.copy(isVibrationEnabled = enabled))
        }
    }

    fun updateReminderSound(sound: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(settings.value.copy(reminderSound = sound))
        }
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(settings.value.copy(selectedTheme = themeName))
        }
    }

    fun updateLanguage(langCode: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(settings.value.copy(languageCode = langCode))
        }
    }

    fun updateSittingDurationPreset(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(
                settings.value.copy(
                    sittingDurationMinutes = minutes,
                    isCustomDuration = false
                )
            )
        }
    }

    fun updateCustomSittingDuration(minutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(
                settings.value.copy(
                    customDurationMinutes = minutes,
                    isCustomDuration = true
                )
            )
        }
    }

    // --- HEALTH GARDEN ACTIONS ---
    fun changeGardenTheme(theme: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = gardenState.value.copy(selectedTheme = theme)
            repository.updateGardenState(updated)
        }
    }

    fun buyDecoration(decoration: GardenDecoration, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentCoins = gardenState.value.coins
        if (currentCoins < decoration.cost) {
            onError("سکه کافی ندارید! بایستید و گام بردارید.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Deduct coins & increment purchased count structure
            val newCoins = currentCoins - decoration.cost
            val newDecoration = decoration.copy(
                purchased = true,
                count = decoration.count + 1
            )
            val updatedState = gardenState.value.copy(
                coins = newCoins,
                successfulBreaksCount = gardenState.value.successfulBreaksCount + 1 // trigger decorations unlocks if any
            )
            repository.updateDecoration(newDecoration)
            repository.updateGardenState(updatedState)
            
            launch(Dispatchers.Main) {
                onSuccess()
            }
        }
    }

    // --- DAILY EXPENSES ACTIONS ---
    fun addExpense(amount: Double, category: String, description: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = sdf.format(Date())
            val newExpense = DailyExpense(
                amount = amount,
                category = category,
                description = description,
                date = System.currentTimeMillis(),
                dateString = dateStr
            )
            repository.insertExpense(newExpense)
        }
    }

    fun removeExpense(expenseId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expenseId)
        }
    }
}
