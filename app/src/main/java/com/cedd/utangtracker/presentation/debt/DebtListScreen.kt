package com.cedd.utangtracker.presentation.debt

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtListScreen(
    onDebtClick: (Long) -> Unit,
    onAddDebt: () -> Unit,
    vm: DebtListViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val isPremium by vm.isPremium.collectAsStateWithLifecycle()
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DebtEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showPremiumDialog) {
        PremiumUpgradeDialog(
            featureName = "Unlimited Debts",
            featureDescription = "Free accounts are limited to 5 active debts. Upgrade to track unlimited debts.",
            onUpgrade = { vm.setPremium(true); showPremiumDialog = false },
            onDismiss = { showPremiumDialog = false }
        )
    }

    if (showDeleteConfirm && pendingDelete != null) {
        val debt = pendingDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false; pendingDelete = null },
            title = { Text("Delete Debt?") },
            text = { Text("Are you sure you want to delete this debt? You can undo immediately after.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    pendingDelete = null
                    vm.deleteDebt(debt)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = "Debt deleted",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            vm.undoDelete(debt)
                        }
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false; pendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Debts", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isPremium && state.totalActiveDebtCount >= 5) showPremiumDialog = true
                    else onAddDebt()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Add Debt")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Custom segmented tab control ────────────────────────────────────
            DebtSegmentedControl(
                selectedTab   = state.selectedTab,
                activeGreen   = MaterialTheme.colorScheme.secondary,
                activeRed     = MaterialTheme.colorScheme.tertiary,
                onTabSelected = { vm.selectTab(it) }
            )

            // ── Sort chips ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebtSortOption.entries.forEach { option ->
                    FilterChip(
                        selected = state.sortOption == option,
                        onClick  = { vm.setSort(option) },
                        label    = { Text(option.label, fontSize = 12.sp) }
                    )
                }
            }

            // ── Search bar ───────────────────────────────────────────────────
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.setQuery(it) },
                placeholder = { Text("Search by name or purpose…", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (state.query.isNotBlank()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 6.dp),
                shape = RoundedCornerShape(12.dp)
            )

            if (state.debts.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (state.selectedTab == 0) "💰" else "📋", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Walang utang dito.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tap + para mag-add ng bagong utang.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.debts, key = { it.id }) { debt ->
                        SwipeToDismissDebt(debt, onSwipeDelete = {
                            if (!debt.isLocked) {
                                pendingDelete = debt
                                showDeleteConfirm = true
                            }
                        }) {
                            val person = state.persons.find { it.id == debt.personId }
                            DebtCard(
                                debt           = debt,
                                personName     = person?.name ?: "Unknown",
                                onClick        = { onDebtClick(debt.id) },
                                onLockToggle   = if (isPremium) { { vm.toggleLock(debt) } } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Segmented Tab Control ───────────────────────────────────────────────────────

@Composable
private fun DebtSegmentedControl(
    selectedTab: Int,
    activeGreen: Color,
    activeRed: Color,
    onTabSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        Row {
            SegmentTab(
                label       = "Owed to Me",
                selected    = selectedTab == 0,
                activeColor = activeGreen,
                onClick     = { onTabSelected(0) },
                modifier    = Modifier.weight(1f)
            )
            SegmentTab(
                label       = "I Owe",
                selected    = selectedTab == 1,
                activeColor = activeRed,
                onClick     = { onTabSelected(1) },
                modifier    = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SegmentTab(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            listOf(activeColor.copy(alpha = 0.30f), activeColor.copy(alpha = 0.12f))
        )
    } else {
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgBrush)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize   = 14.sp,
            color      = if (selected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Swipe to Dismiss ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissDebt(
    debt: DebtEntity,
    onSwipeDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(Unit) {
        snapshotFlow { dismissState.currentValue }
            .drop(1) // skip restored state on recomposition — only react to real user gestures
            .filter { it == SwipeToDismissBoxValue.EndToStart }
            .collect {
                onSwipeDelete()
                dismissState.reset()
            }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = !debt.isLocked,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(end = 20.dp),
                Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        modifier = Modifier.animateContentSize()
    ) { content() }
}
