package com.cedd.utangtracker.presentation.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.utangtracker.presentation.components.CurrencyText
import com.cedd.utangtracker.presentation.components.DebtCard
import java.util.Calendar

@Composable
fun DashboardScreen(
    onAddDebt: () -> Unit,
    onNavigatePersons: () -> Unit = {},
    onNavigateDebts: () -> Unit = {},
    onDebtClick: (Long) -> Unit = {},
    vm: DashboardViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    val heroGradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            Color(0xFF7B2FBE)
        )
    )

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning!"
            in 12..17 -> "Good Afternoon!"
            else      -> "Good Evening!"
        }
    }

    val netBalance = state.totalOwedToMe - state.totalIOwe

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item { DashboardHeader(greeting, state.lenderName) }

        item {
            HeroBalanceCard(
                netBalance = netBalance,
                owedToMe   = state.totalOwedToMe,
                iOwe       = state.totalIOwe,
                gradient   = heroGradient,
                modifier   = Modifier.padding(horizontal = 20.dp)
            )
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            DebtStatusChart(
                activeCount  = state.activeCount,
                overdueCount = state.overdueCount,
                settledCount = state.settledCount,
                modifier     = Modifier.padding(horizontal = 20.dp)
            )
        }

        item { Spacer(Modifier.height(20.dp)) }

        item {
            QuickActionsRow(
                onAddDebt = onAddDebt,
                onPersons = onNavigatePersons,
                onDebts   = onNavigateDebts,
                modifier  = Modifier.padding(horizontal = 20.dp)
            )
        }

        item { Spacer(Modifier.height(28.dp)) }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Recent Debts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (state.recentDebts.isNotEmpty()) {
                    Text(
                        "${state.recentDebts.size} records",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (state.recentDebts.isEmpty()) {
            item { EmptyDebtsPlaceholder() }
        } else {
            items(state.recentDebts) { debt ->
                val person = state.persons.find { it.id == debt.personId }
                DebtCard(
                    debt       = debt,
                    personName = person?.name ?: "Unknown",
                    onClick    = { onDebtClick(debt.id) },
                    modifier   = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

// ── Header ──────────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(greeting: String, lenderName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4361EE).copy(alpha = 0.10f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (lenderName.isNotBlank()) {
                    Text(
                        "Hi $lenderName,",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        greeting,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    Text(
                        greeting,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Utang Tracker",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ── Hero Balance Card ───────────────────────────────────────────────────────────

@Composable
private fun HeroBalanceCard(
    netBalance: Double,
    owedToMe: Double,
    iOwe: Double,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .padding(24.dp)
    ) {
        Column {
            Text(
                "Net Balance",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(6.dp))
            CurrencyText(
                amount     = netBalance,
                color      = Color.White,
                fontSize   = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeroStat(
                    label  = "Owed to Me",
                    amount = owedToMe,
                    color  = Color(0xFF4ADE80),
                    modifier = Modifier.weight(1f),
                    align  = Alignment.Start
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(Color.White.copy(alpha = 0.2f))
                )
                HeroStat(
                    label  = "I Owe",
                    amount = iOwe,
                    color  = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f),
                    align  = Alignment.End
                )
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier,
    align: Alignment.Horizontal = Alignment.Start
) {
    Column(modifier = modifier, horizontalAlignment = align) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        CurrencyText(amount = amount, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ── Quick Actions ───────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsRow(
    onAddDebt: () -> Unit,
    onPersons: () -> Unit,
    onDebts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            "Quick Actions",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            QuickActionButton(
                icon      = Icons.Default.Add,
                label     = "Add Debt",
                onClick   = onAddDebt,
                tintColor = Color(0xFF4361EE),
                modifier  = Modifier.weight(1f)
            )
            QuickActionButton(
                icon      = Icons.Default.People,
                label     = "Persons",
                onClick   = onPersons,
                tintColor = Color(0xFF16A34A),
                modifier  = Modifier.weight(1f)
            )
            QuickActionButton(
                icon      = Icons.Default.Description,
                label     = "Contract",
                onClick   = onDebts,
                tintColor = Color(0xFF7C3AED),
                modifier  = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tintColor: Color = Color(0xFF4361EE),
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        onClick = onClick,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            tintColor.copy(alpha = 0.18f),
                            tintColor.copy(alpha = 0.07f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(tintColor.copy(alpha = 0.25f), tintColor.copy(alpha = 0.08f))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = tintColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── Debt Status Chart ────────────────────────────────────────────────────────────

@Composable
private fun DebtStatusChart(
    activeCount: Int,
    overdueCount: Int,
    settledCount: Int,
    modifier: Modifier = Modifier
) {
    val total = activeCount + overdueCount + settledCount
    if (total == 0) return

    val green   = MaterialTheme.colorScheme.secondary
    val red     = MaterialTheme.colorScheme.tertiary
    val blue    = MaterialTheme.colorScheme.primary
    val track   = MaterialTheme.colorScheme.surfaceVariant
    val onBg    = MaterialTheme.colorScheme.onBackground.toArgb()
    val onVar   = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Donut canvas ────────────────────────────────────────────────
            Canvas(modifier = Modifier.size(110.dp)) {
                val sw     = 28f
                val radius = (size.minDimension / 2f) - sw / 2f
                val tl     = Offset(center.x - radius, center.y - radius)
                val arc    = Size(radius * 2, radius * 2)

                // background track
                drawArc(
                    color = track, startAngle = 0f, sweepAngle = 360f,
                    useCenter = false, topLeft = tl, size = arc,
                    style = Stroke(width = sw)
                )

                val gap = 6f
                var start = -90f
                listOf(
                    activeCount  to green,
                    overdueCount to red,
                    settledCount to blue
                ).filter { it.first > 0 }.forEach { (count, color) ->
                    val sweep = (count.toFloat() / total) * 360f - gap
                    drawArc(
                        color = color,
                        startAngle = start + gap / 2f, sweepAngle = sweep.coerceAtLeast(1f),
                        useCenter = false, topLeft = tl, size = arc,
                        style = Stroke(width = sw, cap = StrokeCap.Round)
                    )
                    start += (count.toFloat() / total) * 360f
                }

                // center label
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        "$total",
                        center.x,
                        center.y + 14f,
                        android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize  = 42f
                            isFakeBoldText = true
                            color = onBg
                        }
                    )
                    canvas.nativeCanvas.drawText(
                        "debts",
                        center.x,
                        center.y + 34f,
                        android.graphics.Paint().apply {
                            textAlign = android.graphics.Paint.Align.CENTER
                            textSize  = 22f
                            color = onVar
                        }
                    )
                }
            }

            // ── Legend ──────────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Debt Overview",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                ChartLegend("Active",  activeCount,  green)
                ChartLegend("Overdue", overdueCount, red)
                ChartLegend("Settled", settledCount, blue)
            }
        }
    }
}

@Composable
private fun ChartLegend(label: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "$count",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Empty State ─────────────────────────────────────────────────────────────────

@Composable
private fun EmptyDebtsPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💸", fontSize = 52.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "No debts yet!",
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap \"Add Debt\" to start tracking.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
