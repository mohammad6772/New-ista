package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.DailyExpense
import com.example.data.models.SittingSession
import com.example.ui.viewmodel.IstaViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(viewModel: IstaViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Health, 1 = Expenses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Switcher Tab Layout
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("گزارش سلامت", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                icon = { Icon(Icons.Default.QueryStats, "سلامت") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("مدیریت هزینه‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                icon = { Icon(Icons.Default.Payments, "هزینه‌ها") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            HealthStatsView(viewModel)
        } else {
            ExpensesStatsView(viewModel)
        }
    }
}

@Composable
fun HealthStatsView(viewModel: IstaViewModel) {
    val sessions by viewModel.sessions.collectAsState()
    val gardenState by viewModel.gardenState.collectAsState()

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val todaySessions = remember(sessions) {
        sessions.filter { it.dateString == todayStr }
    }

    val totalSittingToday = remember(todaySessions) {
        todaySessions.sumOf { it.sittingDurationSeconds }
    }

    val totalStandingToday = remember(todaySessions) {
        todaySessions.sumOf { it.standingDurationSeconds }
    }

    val totalBreaksToday = remember(todaySessions) {
        todaySessions.count { it.standingSuccess }
    }

    val longestSittingSession = remember(sessions) {
        if (sessions.isEmpty()) 0L else sessions.maxOf { it.sittingDurationSeconds }
    }

    val averageSittingDuration = remember(sessions) {
        if (sessions.isEmpty()) 0.0 else sessions.map { it.sittingDurationSeconds }.average()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "خلاصه فعالیت امروز",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // Triple Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard(
                modifier = Modifier.weight(1.0f),
                title = "نشستن امروز",
                value = formatHoursMinutes(totalSittingToday),
                icon = Icons.Default.Timer,
                tint = MaterialTheme.colorScheme.primary
            )
            StatItemCard(
                modifier = Modifier.weight(1.0f),
                title = "ایستادن امروز",
                value = formatHoursMinutes(totalStandingToday),
                icon = Icons.Default.DirectionsWalk,
                tint = MaterialTheme.colorScheme.error
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItemCard(
                modifier = Modifier.weight(1.0f),
                title = "زنجیره فعلی",
                value = "${gardenState.streak} روز",
                icon = Icons.Default.LocalFireDepartment,
                tint = Color(0xFFFF9800)
            )
            StatItemCard(
                modifier = Modifier.weight(1.0f),
                title = "برخاستن سالم",
                value = "$totalBreaksToday بار",
                icon = Icons.Default.DoneAll,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "آمارهای مرجع تاریخی",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LongStatRow(
                    title = "طولانی‌ترین الگوی نشستن به ثبت رسیده",
                    value = formatHoursMinutes(longestSittingSession),
                    icon = Icons.Default.Warning
                )
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                LongStatRow(
                    title = "میانگین طول کل نشستن‌ها",
                    value = formatHoursMinutes(averageSittingDuration.toLong()),
                    icon = Icons.Default.Equalizer
                )
                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                LongStatRow(
                    title = "مجموع دفعات ایستادن ثبت شده",
                    value = "${gardenState.successfulBreaksCount} مرتبه",
                    icon = Icons.Default.Star
                )
            }
        }
    }
}

@Composable
fun ExpensesStatsView(viewModel: IstaViewModel) {
    val expenses by viewModel.expenses.collectAsState()

    var amountInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("سلامت") } // سلامت, غذا, حمل‌ونقل, باغچه, متفرقه

    val categories = listOf("سلامت", "غذا", "حمل‌ونقل", "باغچه", "متفرقه")
    val categoryColors = listOf(
        Color(0xFF4CAF50), // سلامت - green
        Color(0xFFFF9800), // غذا - orange
        Color(0xFF2196F3), // حمل و نقل - blue
        Color(0xFF9C27B0), // باغچه - purple
        Color(0xFF9E9E9E)  // متفرقه - gray
    )

    val totalExpenses = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    val categoryProportions = remember(expenses) {
        val sums = categories.associateWith { cat ->
            expenses.filter { it.category == cat }.sumOf { it.amount }
        }
        sums
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ثبت مخارج روزانه",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )

        // Expense Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("مبلغ هزینه (تومان)", textAlign = TextAlign.Right) },
                    modifier = Modifier.fillMaxWidth().testTag("expense_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = descriptionInput,
                    onValueChange = { descriptionInput = it },
                    label = { Text("بابت چه چیزی؟ (توضیحات)", textAlign = TextAlign.Right) },
                    modifier = Modifier.fillMaxWidth().testTag("expense_description_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category selector
                Column {
                    Text("دسته‌بندی موضوعی:", style = MaterialTheme.typography.labelSmall)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount != null && descriptionInput.isNotBlank()) {
                            viewModel.addExpense(amount, selectedCategory, descriptionInput)
                            amountInput = ""
                            descriptionInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_expense_button"),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Add, "ثبت هزینه")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("افزودن به لیست مخارج", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Charts Section
        if (expenses.isNotEmpty()) {
            Text(
                text = "نمودار سهم مخارج",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pie chart Canvas drawing
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var currentStartAngle = 0f
                            categories.forEachIndexed { idx, cat ->
                                val sum = categoryProportions[cat] ?: 0.0
                                if (sum > 0 && totalExpenses > 0) {
                                    val sweep = ((sum / totalExpenses) * 360f).toFloat()
                                    drawArc(
                                        color = categoryColors[idx],
                                        startAngle = currentStartAngle,
                                        sweepAngle = sweep,
                                        useCenter = true,
                                        size = Size(size.width, size.height)
                                    )
                                    currentStartAngle += sweep
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Legend
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.forEachIndexed { idx, cat ->
                            val sum = categoryProportions[cat] ?: 0.0
                            if (sum > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            modifier = Modifier.size(12.dp),
                                            shape = RoundedCornerShape(2.dp),
                                            color = categoryColors[idx]
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(cat, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text(
                                        text = "${NumberFormat.getInstance().format(sum)} تومان (${String.format(Locale.US, "%.1f", (sum / totalExpenses) * 100)}%)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // List of Expenses
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "لیست مخارج روزانه",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "جمع کل: ${NumberFormat.getInstance().format(totalExpenses)} تومان",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (expenses.isEmpty()) {
            Text(
                text = "هنوز هیچ هزینه‌ای ثبت نشده است.",
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                expenses.forEach { expense ->
                    val colorIdx = categories.indexOf(expense.category).coerceAtLeast(0)
                    val color = categoryColors[colorIdx]

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.size(8.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = color
                                ) {}
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(expense.description, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    Text(expense.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${NumberFormat.getInstance().format(expense.amount)} تومان",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(onClick = { viewModel.removeExpense(expense.id) }) {
                                    Icon(Icons.Default.Delete, "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItemCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, title, tint = tint, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
fun LongStatRow(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}

private fun formatHoursMinutes(seconds: Long): String {
    if (seconds <= 0) return "۰ دقیقه"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) {
        "$h ساعت و $m دقیقه"
    } else {
        "$m دقیقه"
    }
}
