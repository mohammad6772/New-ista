package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.TimerState
import com.example.ui.viewmodel.IstaViewModel
import java.util.Locale

@Composable
fun HomeScreen(viewModel: IstaViewModel) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsState()
    val sittingSecs by viewModel.sittingSeconds.collectAsState()
    val sittingRemainingSecs by viewModel.sittingRemainingSeconds.collectAsState()
    val standingSecs by viewModel.standingSeconds.collectAsState()
    val motivationMessage by viewModel.motivationMessage.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val totalDurationSetting = if (settings.isCustomDuration) {
        settings.customDurationMinutes * 60
    } else {
        settings.sittingDurationMinutes * 60
    }

    val progress = remember(sittingRemainingSecs, totalDurationSetting) {
        if (totalDurationSetting > 0) {
            sittingRemainingSecs.toFloat() / totalDurationSetting.toFloat()
        } else {
            0f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "کاهش نشستن طولانی با ایستا",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 24.sp
                ),
                textAlign = TextAlign.Center
            )
            Text(
                text = "تندرستی را با حرکت برگردانید",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.secondary
                ),
                textAlign = TextAlign.Center
            )
        }

        // Circular Timer Display
        Box(
            modifier = Modifier
                .size(240.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val colorScheme = MaterialTheme.colorScheme
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawArc(
                    color = colorScheme.primary.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                )

                // Foreground Progress Arc
                if (timerState == TimerState.SITTING) {
                    val isTimeUp = sittingRemainingSecs == 0
                    drawArc(
                        color = if (isTimeUp) colorScheme.error else colorScheme.primary,
                        startAngle = -90f,
                        sweepAngle = if (isTimeUp) 360f else progress * 360f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                } else if (timerState == TimerState.STANDING) {
                    drawArc(
                        color = colorScheme.error, // Reddish color for standing break countdown
                        startAngle = -90f,
                        sweepAngle = (standingSecs.toFloat() / 600f) * 360f,
                        useCenter = false,
                        style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Live Timer Text Inside Ring
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                when (timerState) {
                    TimerState.IDLE -> {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "آماده کار",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "آماده شروع",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TimerState.SITTING -> {
                        val isTimeUp = sittingRemainingSecs == 0
                        Text(
                            text = if (isTimeUp) "بلند شوید!" else formatTime(sittingRemainingSecs),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isTimeUp) 34.sp else 44.sp
                            ),
                            color = if (isTimeUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isTimeUp) "زمان نشستن تمام شد!" else "زمان تا ایستادن",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isTimeUp) FontWeight.Bold else FontWeight.Normal),
                            color = if (isTimeUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                    }
                    TimerState.STANDING -> {
                        Text(
                            text = formatTime(standingSecs),
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "مدت زمان ایستادگی",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Active details info cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                onClick = {},
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AirlineSeatReclineNormal, "تایمر نشستن", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("نشستن", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatTime(sittingSecs),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Card(
                onClick = {},
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.DirectionsWalk, "تایمر ایستادن", tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("ایستادن", style = MaterialTheme.typography.labelSmall)
                    Text(
                        text = formatTime(standingSecs),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (timerState) {
                TimerState.IDLE -> {
                    Button(
                        onClick = { viewModel.startSitting(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("start_sitting_button"),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "شروع ردیابی")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "شروع ردیابی نشستن",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                TimerState.SITTING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isTimeUp = sittingRemainingSecs == 0
                        if (isTimeUp) {
                            Button(
                                onClick = { viewModel.snoozeSitting(context) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("snooze_sitting_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(Icons.Default.Snooze, contentDescription = "به تاخیر انداختن")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("۵ دقیقه تاخیر (امکان بلند شدن ندارم)", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.triggerStandManually(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("stand_manually_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTimeUp) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "بلند شدم")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("من ایستادم", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = { viewModel.stopSitting(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("stop_sitting_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "توقف ردیابی", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توقف کامل", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                TimerState.STANDING -> {
                    Button(
                        onClick = { viewModel.stopStandingManually(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("stop_standing_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "توقف ایستادن")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "پایان ایستادن و نشستن مجدد",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // Sensor Indicator Badge
        if (timerState == TimerState.SITTING) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "سنسور فعال است",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ردیابی گام هوشمند فعال است (۵ قدم بایستید)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Motivational Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "پیام انگیزشی",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = motivationMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Convert seconds to mm:ss format for display
private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
