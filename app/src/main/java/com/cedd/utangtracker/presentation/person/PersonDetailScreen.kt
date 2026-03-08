package com.cedd.utangtracker.presentation.person

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.presentation.components.DebtCard
import com.cedd.utangtracker.presentation.components.PremiumUpgradeDialog
import kotlin.math.abs

private val avatarPalette = listOf(
    Color(0xFF6C63FF), Color(0xFF4ADE80), Color(0xFF4361EE), Color(0xFFFFA726),
    Color(0xFF26C6DA), Color(0xFFEC407A), Color(0xFF26A69A), Color(0xFFAB47BC),
)
private fun avatarColorFor(name: String): Color =
    avatarPalette[abs(name.hashCode()) % avatarPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    onBack: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onEditPerson: (Long) -> Unit,
    vm: PersonDetailViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val person = state.person

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(person?.name ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    person?.let {
                        IconButton(onClick = { onEditPerson(it.id) }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (person == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Person header ─────────────────────────────────────────────
            item {
                val color = avatarColorFor(person.name)
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.12f))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                person.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = color, fontWeight = FontWeight.Bold, fontSize = 26.sp
                            )
                        }
                        Column {
                            Text(person.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            person.phone?.let {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Phone, null,
                                        Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(it, fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            person.notes?.let {
                                Text(it, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // ── Summary row ───────────────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryChip(
                        label = "Outstanding",
                        value = "₱${"%.0f".format(state.totalOutstanding)}",
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = "Settled",
                        value = "₱${"%.0f".format(state.totalSettled)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryChip(
                        label = "Overdue",
                        value = "${state.overdueCount}",
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Debt list ─────────────────────────────────────────────────
            if (state.debts.isEmpty()) {
                item {
                    Box(
                        Modifier.fillMaxWidth().padding(top = 32.dp),
                        Alignment.Center
                    ) {
                        Text(
                            "Walang utang para kay ${person.name}.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "Debts (${state.debts.size})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(state.debts, key = { it.id }) { debt ->
                    DebtCard(
                        debt       = debt,
                        personName = person.name,
                        onClick    = { onDebtClick(debt.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
