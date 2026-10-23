package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.IstaViewModel

@Composable
fun SettingsScreen(viewModel: IstaViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()

    val durationPresets = listOf(20, 25, 30, 40, 45, 60)
    var customInput by remember { mutableStateOf("") }

    val soundOptions = listOf("Default", "Bell", "Soft Chime", "Nature", "Digital Beep")
    val themeOptions = listOf("System Default", "Light", "Dark", "Green Nature", "Blue Ocean", "Purple Night")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "تنظیمات هوشمند ایستا",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // SITTING DURATION CONFIGURATION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HourglassEmpty, "مدت ردیابی", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مدت زمان نشستن مجاز پیش از یادآوری ایستادن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                // Presets grid of buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("انتخاب زمان پیش‌فرض (دقیقه):", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durationPresets.take(3).forEach { duration ->
                            val isSelected = !settings.isCustomDuration && settings.sittingDurationMinutes == duration
                            Button(
                                onClick = { viewModel.updateSittingDurationPreset(duration) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("preset_${duration}_button")
                            ) {
                                Text("$duration دقیقه", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        durationPresets.drop(3).forEach { duration ->
                            val isSelected = !settings.isCustomDuration && settings.sittingDurationMinutes == duration
                            Button(
                                onClick = { viewModel.updateSittingDurationPreset(duration) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("preset_${duration}_button")
                            ) {
                                Text("$duration دقیقه", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // Custom limit input field
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("یا ثبت زمان دلخواه سفارشی:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customInput,
                            onValueChange = { customInput = it },
                            label = { Text("دقیقه") },
                            modifier = Modifier.weight(1f).testTag("custom_duration_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Button(
                            onClick = {
                                val mins = customInput.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    viewModel.updateCustomSittingDuration(mins)
                                    customInput = ""
                                    Toast.makeText(context, "زمان نشستن با موفقیت بر روی $mins دقیقه تنظیم شد.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "لطفا عدد معتبر وارد کنید.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(56.dp).testTag("save_custom_duration_button")
                        ) {
                            Text("اعمال")
                        }
                    }

                    if (settings.isCustomDuration) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "زمان سفارشی فعال: ${settings.customDurationMinutes} دقیقه",
                                modifier = Modifier.padding(8.dp),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }

        // REMINDER SOUND CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, "صدا", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تنظیم صدای هشدار یادآور ایستادن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    soundOptions.take(3).forEach { sound ->
                        val isSelected = settings.reminderSound == sound
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateReminderSound(sound) },
                            label = { Text(sound) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    soundOptions.drop(3).forEach { sound ->
                        val isSelected = settings.reminderSound == sound
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateReminderSound(sound) },
                            label = { Text(sound) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // VIBRATION CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, "لرزش", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لرزش دستگاه هنگام زنگ هشدار اصلی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Switch(
                    checked = settings.isVibrationEnabled,
                    onCheckedChange = { viewModel.updateVibration(it) },
                    modifier = Modifier.testTag("vibration_toggle")
                )
            }
        }

        // VISUAL ATMOSPHERE / THEME SELECTOR CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, "تم", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تم رنگی و فضای بصری اپلیکیشن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeOptions.chunked(3).forEach { rowThemes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowThemes.forEach { themeName ->
                                val isSelected = settings.selectedTheme == themeName
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.updateTheme(themeName) }
                                        .testTag("theme_chip_${themeName.replace(" ", "_")}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Text(
                                        text = translateThemeNamePersian(themeName),
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun translateThemeNamePersian(enTheme: String): String {
    return when (enTheme) {
        "System Default" -> "پیش‌فرض سیستم"
        "Light" -> "روشن کلاسیک"
        "Dark" -> "تیره شب"
        "Green Nature" -> "سبز طبیعت"
        "Blue Ocean" -> "آبی اقیانوس"
        "Purple Night" -> "بنفش کهکشان"
        else -> enTheme
    }
}
