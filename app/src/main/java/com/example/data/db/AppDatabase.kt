package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SettingsEntity::class,
        SittingSession::class,
        DailyExpense::class,
        GardenState::class,
        GardenDecoration::class,
        DailyMission::class,
        Achievement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ista_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.appDao())
                }
            }
        }

        suspend fun populateDatabase(dao: AppDao) {
            // 1. Def Settings
            dao.insertOrUpdateSettings(SettingsEntity())

            // 2. Def Garden State
            dao.insertOrUpdateGardenState(GardenState())

            // 3. Decorations Catalog
            val defaultDecorations = listOf(
                GardenDecoration("flowers", "گل‌های باغچه", "Colorful Flowers", 10, false, 0, "local_florist"),
                GardenDecoration("grass", "سبزه و چمنزار", "Lush Grass", 5, false, 0, "grass"),
                GardenDecoration("bench", "نیمکت چوبی", "Wooden Bench", 30, false, 0, "weekend"),
                GardenDecoration("pond", "برکه کوچک آب", "Small Pond", 80, false, 0, "water"),
                GardenDecoration("birds", "آواز پرندگان", "Garden Birds", 40, false, 0, "flutter_dash"),
                GardenDecoration("butterflies", "پروانه‌های رقصان", "Butterflies", 25, false, 0, "emoji_nature"),
                GardenDecoration("fountain", "فواره آبنما", "Water Fountain", 150, false, 0, "filter_hdr"),
                GardenDecoration("statue", "تندیس باغ", "Garden Statue", 200, false, 0, "palette"),
                GardenDecoration("lights", "چراغ‌های باغچه", "Garden Lights", 50, false, 0, "lightbulb"),
                GardenDecoration("stone_path", "مسیر سنگفرش", "Stone Paths", 35, false, 0, "view_module")
            )
            dao.insertDecorations(defaultDecorations)

            // 4. Daily Missions
            val defaultMissions = listOf(
                DailyMission("stand_5", "ایستادن ۵ بار در طول روز", "Stand up 5 times today", 5, 0, false, 25, 50),
                DailyMission("no_long_sit", "جلوگیری از نشستن طولانی به مدت ۲ ساعت", "Avoid sitting for 2 hours", 2, 0, false, 15, 30),
                DailyMission("streak_3", "کامل کردن یادآورها برای ۳ روز متوالی", "Complete reminders for 3 consecutive days", 3, 0, false, 50, 100)
            )
            dao.insertMissions(defaultMissions)

            // 5. Achievements
            val defaultAchievements = listOf(
                Achievement("first_stand", "اولین گام سلامتی", "First Stand Break", "کامل کردن اولین ایستادگی موفقیت‌آمیز", "Complete your first successful standing break.", false),
                Achievement("one_week_streak", "یک هفته پایداری", "One Week Streak", "کامل کردن زنجیره ایستادن برای ۷ روز متوالی", "Maintain a standing streak for 7 consecutive days.", false),
                Achievement("100_breaks", "مدافع سلامت (۱۰۰ ایستادن)", "Health Defender (100 Breaks)", "ثبت ۱۰۰ ایستادن موفقیت‌آمیز در سلامت", "Reach 100 successful standing breaks.", false),
                Achievement("healthy_worker", "کارمند دوستدار تندرستی", "Healthy Professional", "اجتناب از نشستن‌های مداوم طولانی در یک روز برای ساعت‌ها", "Avoid long sitting intervals in a single workday.", false),
                Achievement("forest_builder", "بنیان‌گذار جنگل سبز", "Forest Builder", "رسیدن به سطح باغچه شخصی جنگلی زیبا و انبوه", "Evolve your garden level to a personal forest.", false)
            )
            dao.insertAchievements(defaultAchievements)
        }
    }
}
