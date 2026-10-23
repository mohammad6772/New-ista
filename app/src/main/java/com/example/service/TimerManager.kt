package com.example.service

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.models.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class TimerState {
    IDLE, SITTING, STANDING
}

object TimerManager {
    val timerState = MutableStateFlow(TimerState.IDLE)
    val sittingSeconds = MutableStateFlow(0)
    val sittingRemainingSeconds = MutableStateFlow(0)
    val standingSeconds = MutableStateFlow(0)
    val motivationMessage = MutableStateFlow("نشستن بیش از حد برای سلامتی مضر است. ایستا باشید!")

    private var sittingLimitSeconds = 25 * 60

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var startTimeMillis: Long = 0

    private var sensorManager: SensorManager? = null
    private var stepDetectorSensor: Sensor? = null
    private var accelerometerSensor: Sensor? = null
    private var stepCountSinceSitting = 0
    private var accelerometerLastX = 0f
    private var accelerometerLastY = 0f
    private var accelerometerLastZ = 0f
    private var lastAccUpdate: Long = 0
    private val SHAKE_THRESHOLD = 600

    private var repository: AppRepository? = null

    fun initRepository(context: Context) {
        if (repository == null) {
            val db = AppDatabase.getDatabase(context, scope)
            repository = AppRepository(db.appDao())
        }
    }

    fun startSitting(context: Context, durationMinutes: Int) {
        initRepository(context)
        sittingLimitSeconds = durationMinutes * 60
        timerState.value = TimerState.SITTING
        sittingSeconds.value = 0
        sittingRemainingSeconds.value = sittingLimitSeconds
        standingSeconds.value = 0
        startTimeMillis = System.currentTimeMillis()
        stepCountSinceSitting = 0

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_SITTING
            putExtra(TimerService.EXTRA_DURATION_MIN, durationMinutes)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        registerSensors(context)

        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            while (timerState.value == TimerState.SITTING) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    sittingSeconds.value += 1
                    if (sittingRemainingSeconds.value > 0) {
                        sittingRemainingSeconds.value -= 1
                        if (sittingRemainingSeconds.value == 0) {
                            triggerReminderAlert(context)
                            // Start foreground service alert state notification (time up)
                            val intent = Intent(context, TimerService::class.java).apply {
                                action = TimerService.ACTION_SITTING_TIME_UP
                            }
                            context.startService(intent)
                        }
                    } else {
                        // Periodic alert every 30 seconds if they continue sitting at 0
                        if (sittingSeconds.value % 30 == 0) {
                            triggerReminderAlert(context)
                        }
                    }
                }
            }
        }

        updateMotivationMessage()
    }

    fun stopSitting(context: Context) {
        if (timerState.value == TimerState.SITTING) {
            val sittingSec = sittingSeconds.value
            saveSession(context, sittingSec, 0, false)
        }
        cancelAll(context)
    }

    private fun startStandingAutomatically(context: Context) {
        val sittingSec = sittingSeconds.value
        timerState.value = TimerState.STANDING
        standingSeconds.value = 0
        unregisterSensors()

        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START_STANDING
        }
        context.startService(intent)

        job?.cancel()
        job = scope.launch(Dispatchers.IO) {
            while (timerState.value == TimerState.STANDING) {
                delay(1000)
                withContext(Dispatchers.Main) {
                    standingSeconds.value += 1
                    if (standingSeconds.value >= 600) { // 10 minutes max limit
                        saveSession(context, sittingSec, standingSeconds.value, true)
                        cancelAll(context)
                    }
                }
            }
        }
    }

    fun onStandingDetectedAutomatically(context: Context) {
        if (timerState.value == TimerState.SITTING) {
            val sittingSec = sittingSeconds.value
            triggerFeedback(context)
            
            scope.launch {
                rewardSuccessfulStand(context)
            }
            startStandingAutomatically(context)
        }
    }

    fun triggerStandManually(context: Context) {
        if (timerState.value == TimerState.SITTING) {
            onStandingDetectedAutomatically(context)
        }
    }

    fun stopStandingManually(context: Context) {
        if (timerState.value == TimerState.STANDING) {
            val sittingSec = sittingSeconds.value
            val standingSec = standingSeconds.value
            saveSession(context, sittingSec, standingSec, true)
        }
        cancelAll(context)
    }

    private fun cancelAll(context: Context) {
        timerState.value = TimerState.IDLE
        sittingSeconds.value = 0
        sittingRemainingSeconds.value = 0
        standingSeconds.value = 0
        job?.cancel()
        unregisterSensors()

        val intent = Intent(context, TimerService::class.java)
        context.stopService(intent)
        updateMotivationMessage()
    }

    private fun registerSensors(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepDetectorSensor != null) {
            sensorManager?.registerListener(sensorListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            accelerometerSensor?.let {
                sensorManager?.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    private fun unregisterSensors() {
        sensorManager?.unregisterListener(sensorListener)
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (timerState.value != TimerState.SITTING) return
            event ?: return

            if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                stepCountSinceSitting++
                if (stepCountSinceSitting >= 5) {
                    Log.d("TimerManager", "Stood up automatically detected via steps!")
                    MainActivity.instance?.let {
                        onStandingDetectedAutomatically(it)
                    }
                }
            } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val curTime = System.currentTimeMillis()
                if ((curTime - lastAccUpdate) > 150) {
                    val diffTime = curTime - lastAccUpdate
                    lastAccUpdate = curTime

                    val speed = abs(x + y + z - accelerometerLastX - accelerometerLastY - accelerometerLastZ) / diffTime * 10000
                    if (speed > SHAKE_THRESHOLD) {
                        stepCountSinceSitting++
                        if (stepCountSinceSitting >= 8) {
                            Log.d("TimerManager", "Stood up automatically detected via accelerometer!")
                            MainActivity.instance?.let {
                                onStandingDetectedAutomatically(it)
                            }
                        }
                    }
                    accelerometerLastX = x
                    accelerometerLastY = y
                    accelerometerLastZ = z
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private fun saveSession(context: Context, sittingSec: Int, standingSec: Int, success: Boolean) {
        initRepository(context)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateStr = sdf.format(Date())

        scope.launch(Dispatchers.IO) {
            val session = SittingSession(
                startTime = startTimeMillis,
                endTime = System.currentTimeMillis(),
                sittingDurationSeconds = sittingSec.toLong(),
                standingDurationSeconds = standingSec.toLong(),
                standingSuccess = success,
                dateString = dateStr
            )
            repository?.insertSession(session)
        }
    }

    private suspend fun rewardSuccessfulStand(context: Context) {
        val directState = repository?.getGardenStateDirect() ?: return

        val coinsBounty = 10
        val xpGain = 20
        var streak = directState.streak
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val lastTime = directState.lastStandingTime
        if (lastTime > 0) {
            val lastDateStr = sdf.format(Date(lastTime))
            try {
                val calLast = Calendar.getInstance().apply { time = sdf.parse(lastDateStr)!! }
                val calToday = Calendar.getInstance().apply { time = sdf.parse(today)!! }
                val diffDays = ((calToday.timeInMillis - calLast.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                if (diffDays == 1) {
                    streak += 1
                } else if (diffDays > 1) {
                    streak = 1
                }
            } catch (e: Exception) {
                streak = 1
            }
        } else {
            streak = 1
        }

        val longestStreak = if (streak > directState.longestStreak) streak else directState.longestStreak

        val newXp = directState.xp + xpGain
        var newLevel = (newXp / 100) + 1
        if (newLevel > 7) newLevel = 7

        val updatedState = directState.copy(
            coins = directState.coins + coinsBounty,
            xp = newXp,
            level = newLevel,
            streak = streak,
            longestStreak = longestStreak,
            lastStandingTime = System.currentTimeMillis(),
            successfulBreaksCount = directState.successfulBreaksCount + 1,
            plantedTreesCount = directState.plantedTreesCount + (if ((directState.successfulBreaksCount + 1) % 3 == 0) 1 else 0)
        )
        repository?.updateGardenState(updatedState)

        // Target active achievements on reaching levels/counts
        checkAchievements(updatedState)

        // Daily mission update
        val currentMissions = repository?.getMissionsDirect() ?: emptyList()
        currentMissions.forEach { mission ->
            if (mission.id == "stand_5" && !mission.completed) {
                val nextCount = mission.currentCount + 1
                val isCompleted = nextCount >= mission.targetCount
                val rewardsCoins = if (isCompleted) mission.rewardCoins else 0
                val rewardsXp = if (isCompleted) mission.rewardXp else 0
                
                repository?.updateMission(mission.copy(
                    currentCount = nextCount,
                    completed = isCompleted
                ))
                if (isCompleted) {
                    val gs = repository?.getGardenStateDirect()
                    if (gs != null) {
                        repository?.updateGardenState(gs.copy(
                            coins = gs.coins + rewardsCoins,
                            xp = gs.xp + rewardsXp
                        ))
                    }
                }
            }
        }
    }

    private suspend fun checkAchievements(state: GardenState) {
        val repo = repository ?: return
        
        // 1. first_stand
        if (state.successfulBreaksCount >= 1) {
            triggerUnlockAchievement("first_stand")
        }
        // 2. one_week_streak
        if (state.streak >= 7) {
            triggerUnlockAchievement("one_week_streak")
        }
        // 3. 100_breaks
        if (state.successfulBreaksCount >= 100) {
            triggerUnlockAchievement("100_breaks")
        }
        // 4. forest_builder
        if (state.level >= 6) {
            triggerUnlockAchievement("forest_builder")
        }
    }

    private suspend fun triggerUnlockAchievement(id: String) {
        val repo = repository ?: return
        repo.achievements.collect { list ->
            val ach = list.find { it.id == id }
            if (ach != null && !ach.completed) {
                repo.updateAchievement(ach.copy(
                    completed = true,
                    completedDate = System.currentTimeMillis()
                ))
            }
        }
    }

    private fun triggerReminderAlert(context: Context) {
        scope.launch {
            val settings = repository?.getSettingsDirect() ?: SettingsEntity()
            
            // Vibration alert
            if (settings.isVibrationEnabled) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vm?.defaultVibrator
                } else {
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500), -1))
            }

            // High Quality Audio Alarm Alert Playback
            try {
                when (settings.reminderSound) {
                    "Digital Beep" -> {
                        val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
                        toneGen.startTone(android.media.ToneGenerator.TONE_CDMA_HIGH_L, 1000)
                    }
                    else -> {
                        val type = when (settings.reminderSound) {
                            "Bell" -> android.media.RingtoneManager.TYPE_RINGTONE
                            "Soft Chime" -> android.media.RingtoneManager.TYPE_NOTIFICATION
                            "Nature" -> android.media.RingtoneManager.TYPE_ALL
                            else -> android.media.RingtoneManager.TYPE_ALARM
                        }
                        val uri = android.media.RingtoneManager.getDefaultUri(type)
                            ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                        val ringtone = android.media.RingtoneManager.getRingtone(context, uri)
                        ringtone?.play()
                    }
                }
            } catch (e: Exception) {
                Log.e("TimerManager", "Error playing sound: ${e.message}")
            }
        }
    }

    fun snoozeSitting(context: Context, minutes: Int = 5) {
        if (timerState.value == TimerState.SITTING) {
            sittingRemainingSeconds.value = minutes * 60
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_START_SITTING
                putExtra(TimerService.EXTRA_DURATION_MIN, minutes)
            }
            context.startService(intent)
        }
    }

    private fun triggerFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    fun updateMotivationMessage() {
        val messagesPersian = listOf(
            "نشستن بیش از حد برای سلامتی مضر است. ایستا باشید!",
            "یک لحظه بایستید، نفس عمیق بکشید و بدنتان را بکشید.",
            "برخیزید و باغچه زیبایتان را سرسبزتر کنید!",
            "سلامتی شما ارزشمندترین دارایی شماست. حرکت کنید!",
            "کمی راه رفتن جریان خون شما را بهبود می‌بخشد.",
            "با ایستادگی مداوم، هم انگیزه بگیرید و هم کوین‌های خود را افزایش دهید!"
        )
        motivationMessage.value = messagesPersian.random()
    }
}
