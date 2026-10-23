package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.AppDatabase
import com.example.data.repository.AppRepository
import com.example.service.TimerManager
import com.example.ui.screens.*
import com.example.ui.theme.IstaTheme
import com.example.ui.viewmodel.IstaViewModel
import com.example.ui.viewmodel.ViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        var instance: MainActivity? = null
            private set
    }

    private lateinit var viewModel: IstaViewModel

    // Permission launcher for post API 33 notifications and activity sensors
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions state flags optionally
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()

        // Request startup permissions safely
        requestStartupPermissions()

        // Build database and repository layers cleanly
        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val database = AppDatabase.getDatabase(this, applicationScope)
        val repository = AppRepository(database.appDao())
        
        // Initialize static DB link on TimerManager
        TimerManager.initRepository(this)

        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[IstaViewModel::class.java]

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()

            IstaTheme(themeName = settings.selectedTheme) {
                var currentScreenIndex by remember { mutableIntStateOf(0) } // 0 = Home, 1 = Stats, 2 = Settings, 3 = About

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.navigationBarsPadding().testTag("bottom_navigation_bar"),
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = currentScreenIndex == 0,
                                onClick = { currentScreenIndex = 0 },
                                icon = { Icon(Icons.Default.AirlineSeatReclineNormal, "خانه") },
                                label = { Text("ایستادگی", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("tab_home")
                            )
                            NavigationBarItem(
                                selected = currentScreenIndex == 1,
                                onClick = { currentScreenIndex = 1 },
                                icon = { Icon(Icons.Default.QueryStats, "آمار") },
                                label = { Text("آمار و مخارج", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("tab_stats")
                            )
                            NavigationBarItem(
                                selected = currentScreenIndex == 2,
                                onClick = { currentScreenIndex = 2 },
                                icon = { Icon(Icons.Default.Settings, "تنظیمات") },
                                label = { Text("تنظیمات", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("tab_settings")
                            )
                            NavigationBarItem(
                                selected = currentScreenIndex == 3,
                                onClick = { currentScreenIndex = 3 },
                                icon = { Icon(Icons.Default.Info, "درباره ما") },
                                label = { Text("ایستا", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.testTag("tab_about")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreenIndex) {
                            0 -> HomeScreen(viewModel)
                            1 -> StatisticsScreen(viewModel)
                            2 -> SettingsScreen(viewModel)
                            3 -> AboutScreen()
                        }
                    }
                }
            }
        }
    }

    private fun requestStartupPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsNeeded.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
