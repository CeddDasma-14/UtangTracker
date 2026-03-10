package com.cedd.utangtracker.presentation.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.cedd.utangtracker.data.local.entity.LedgerEntryEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private val MONTH_NAMES = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)
private val MONTH_SHORT = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
private fun formatDate(millis: Long): String = dateFormatter.format(millis)

private fun fmt(amount: Double): String {
    val n = NumberFormat.getNumberInstance(Locale.US)
    n.minimumFractionDigits = 2; n.maximumFractionDigits = 2
    return "₱${n.format(amount)}"
}

// ── Colors ────────────────────────────────────────────────────────────────────
private val GreenPaid    = Color(0xFFE8F5E9)
private val GreenText    = Color(0xFF2E7D32)
private val RedMissed    = Color(0xFFFFEBEE)
private val RedText      = Color(0xFFC62828)
private val AmberPartial = Color(0xFFFFFDE7)
private val AmberText    = Color(0xFFF57F17)
private val BlueAccent   = Color(0xFF1565C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanLedgerScreen(
    onBack: () -> Unit,
    vm: LoanLedgerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val debt by vm.debt.collectAsState()
    val entries by vm.entries.collectAsState()
    val addState by vm.addState.collectAsState()
    val showSettings by vm.showSettings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Loan Ledger", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        IconButton(onClick = {
                            val text = vm.buildShareText()
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Ledger"))
                        }) {
                            Icon(Icons.Default.Share, "Share ledger")
                        }
                    }
                    IconButton(onClick = { vm.toggleSettings() }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (debt?.ledgerEnabled == true) {
                FloatingActionButton(
                    onClick = { vm.openAddEntry() },
                    containerColor = BlueAccent
                ) {
                    Icon(Icons.Default.Add, "Add Month", tint = Color.White)
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ── Settings panel ────────────────────────────────────────────────
            AnimatedVisibility(visible = showSettings, enter = expandVertically(), exit = shrinkVertically()) {
                debt?.let { d ->
                    SettingsPanel(
                        ledgerEnabled = d.ledgerEnabled,
                        carryOver = d.ledgerCarryOver,
                        carryOverMonthly = d.ledgerCarryOverMonthly,
                        cycleMonths = d.ledgerCycleMonths,
                        initialBalance = d.ledgerInitialBalance,
                        hasEntries = entries.isNotEmpty(),
                        onLedgerEnabled = { vm.setLedgerEnabled(it) },
                        onCarryOver = {},
                        onCarryOverMonthly = {},
                        onCycleMonths = {},
                        onInitialBalance = {},
                        onDismiss = { vm.dismissSettings() }
                    )
                }
            }

            if (debt?.ledgerEnabled != true) {
                DisabledPlaceholder()
            } else {
                debt?.let { d ->
                    val startBal  = vm.startingBalance(d)
                    val bill      = vm.monthlyBill(d)
                    val interest  = vm.fixedInterest(d)
                    val effectiveOpeningBal = if (d.ledgerInitialBalance > 0.0) d.ledgerInitialBalance else startBal

                    LazyColumn(Modifier.fillMaxSize()) {
                        // ── Summary card ──────────────────────────────────────
                        item {
                            SummaryCard(
                                purpose      = d.purpose,
                                principal    = d.amount,
                                rate         = d.interestRate,
                                cycleMonths  = d.ledgerCycleMonths,
                                totalAmount  = startBal,
                                monthlyBill  = bill,
                                fixedInt     = interest,
                                carryOver    = d.ledgerCarryOver,
                                initialBalance = d.ledgerInitialBalance,
                                currentBal   = entries.lastOrNull()?.closingBalance ?: effectiveOpeningBal,
                                entryCount   = entries.size,
                                missedCount  = entries.count { it.isMissedPayment }
                            )
                        }

                        if (entries.isEmpty()) {
                            item {
                                LedgerSetupCard(
                                    debt = d,
                                    autoStartBal = startBal,
                                    onSave = { cycleMonths, initialBal ->
                                        vm.setCycleMonths(cycleMonths)
                                        vm.setInitialBalance(initialBal)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Ledger ready! Tap + to record the first month.")
                                        }
                                    }
                                )
                            }
                        } else {
                            items(entries, key = { it.id }) { entry ->
                                LedgerEntryCard(
                                    entry = entry,
                                    monthlyBill = bill,
                                    onDelete = { vm.deleteEntry(entry) }
                                )
                            }
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }
        }
    }

    addState?.let { state ->
        AddEntryDialog(
            state = state,
            debt = debt,
            monthlyBill = debt?.let { vm.monthlyBill(it) } ?: 0.0,
            onStateChange = { vm.updateAddState(it) },
            onConfirm = { vm.saveEntry() },
            onDismiss = { vm.dismissAddEntry() }
        )
    }
}

// ── Summary Card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    purpose: String,
    principal: Double,
    rate: Double,
    cycleMonths: Int,
    totalAmount: Double,
    monthlyBill: Double,
    fixedInt: Double,
    carryOver: Double,
    initialBalance: Double,
    currentBal: Double,
    entryCount: Int,
    missedCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Brush.linearGradient(listOf(Color(0xFF1A237E), Color(0xFF1565C0))), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Borrower / purpose label
            Text(purpose, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))

            // Current balance — the most important number
            if (entryCount > 0) {
                val balColor = if (missedCount > 0) Color(0xFFFF8A80) else Color(0xFFA5D6A7)
                Text(fmt(currentBal), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = balColor)
                Text(
                    if (missedCount > 0) "$missedCount missed · $entryCount months tracked"
                    else "$entryCount months tracked · all paid",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            }

            // Loan details row — matches Excel header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SumItem("Principal", fmt(principal))
                SumItem("Total (${rate.toInt()}%×${cycleMonths}mo)", fmt(totalAmount), center = true)
                SumItem("Monthly Bill", fmt(monthlyBill), right = true)
            }
        }
    }
}

@Composable
private fun SumItem(label: String, value: String, center: Boolean = false, right: Boolean = false) {
    Column(horizontalAlignment = when {
        right -> Alignment.End
        center -> Alignment.CenterHorizontally
        else -> Alignment.Start
    }) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

// ── Entry Card ────────────────────────────────────────────────────────────────

@Composable
private fun LedgerEntryCard(
    entry: LedgerEntryEntity,
    monthlyBill: Double,
    onDelete: () -> Unit
) {
    val isMissed  = entry.isMissedPayment
    val isPartial = !isMissed && entry.paymentAmount < monthlyBill - 0.01
    val bg = when {
        isMissed  -> RedMissed
        isPartial -> AmberPartial
        else      -> GreenPaid
    }
    val accentColor = when {
        isMissed  -> RedText
        isPartial -> AmberText
        else      -> GreenText
    }
    val shortfall = if (isPartial) monthlyBill - entry.paymentAmount else 0.0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: month + status line
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    MONTH_SHORT.getOrElse(entry.month - 1) { "?" } + " ${entry.year}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                // Excel-style status line
                val statusLine = when {
                    isMissed -> buildString {
                        append("NO PAYMENT RECEIVED")
                        if (entry.interestAdded > 0) append(" · +${fmt(entry.interestAdded)}")
                        if (entry.carryOverAdded > 0) append(" · +${fmt(entry.carryOverAdded)} prev.bal")
                    }
                    isPartial -> "${fmt(entry.paymentAmount)} received · shortfall ${fmt(shortfall)}"
                    else -> "${fmt(entry.paymentAmount)} received"
                }
                Text(statusLine, style = MaterialTheme.typography.bodySmall, color = accentColor, fontWeight = FontWeight.Medium)
                if (entry.paymentDate != null) {
                    Text("Paid: ${formatDate(entry.paymentDate)}", style = MaterialTheme.typography.labelSmall, color = BlueAccent)
                }
                if (entry.notes.isNotBlank()) {
                    Text(entry.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            // Right: running balance (prominent) + delete
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(fmt(entry.closingBalance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
                Text("balance", style = MaterialTheme.typography.labelSmall, color = accentColor.copy(alpha = 0.7f))
                IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ── Disabled placeholder ──────────────────────────────────────────────────────

@Composable
private fun DisabledPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Text("Ledger not enabled", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("Tap ⚙ above to enable and configure the ledger.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        }
    }
}

// ── Add Entry Dialog ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(
    state: LoanLedgerViewModel.AddEntryState,
    debt: com.cedd.utangtracker.data.local.entity.DebtEntity?,
    monthlyBill: Double,
    onStateChange: (LoanLedgerViewModel.AddEntryState.() -> LoanLedgerViewModel.AddEntryState) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Add Month Entry")
                val monthLabel = MONTH_SHORT.getOrElse(state.month - 1) { "?" }
                Text("$monthLabel ${state.year} · Bill: ${fmt(monthlyBill)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Month / Year
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var monthExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = it }, modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = MONTH_SHORT.getOrElse(state.month - 1) { "?" },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Month") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                            MONTH_SHORT.forEachIndexed { idx, name ->
                                DropdownMenuItem(text = { Text(name) }, onClick = {
                                    onStateChange { copy(month = idx + 1) }
                                    monthExpanded = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = state.year.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { onStateChange { copy(year = it) } } },
                        label = { Text("Year") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Payment amount
                OutlinedTextField(
                    value = state.paymentInput,
                    onValueChange = { onStateChange { copy(paymentInput = it) } },
                    label = { Text("Payment received (0 = missed)") },
                    prefix = { Text("₱") },
                    supportingText = { Text("Expected: ${fmt(monthlyBill)}") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Payment date
                OutlinedTextField(
                    value = state.paymentDateMillis?.let { formatDate(it) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment Date (optional)") },
                    placeholder = { Text("Tap to set date") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, "Pick date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.paymentDateMillis != null) {
                    TextButton(onClick = { onStateChange { copy(paymentDateMillis = null) } }, modifier = Modifier.align(Alignment.End)) {
                        Text("Clear date", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Previous balance — always optional
                OutlinedTextField(
                    value = state.carryOverOverride,
                    onValueChange = { onStateChange { copy(carryOverOverride = it) } },
                    label = { Text("Previous Balance (optional)") },
                    prefix = { Text("₱") },
                    placeholder = { Text("0 = skip") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Add any unpaid balance carried over from the lender") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { onStateChange { copy(notes = it) } },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Preview ────────────────────────────────────────────────────
                state.preview?.let { preview ->
                    HorizontalDivider()
                    val payment = state.paymentInput.toDoubleOrNull() ?: 0.0
                    val opening = preview.closingBalance - preview.interestAdded - preview.carryOverAdded + payment
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Preview", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            PreviewRow("Opening balance", fmt(opening))
                            if (preview.isMissedPayment) {
                                PreviewRow("+Interest (missed)", fmt(preview.interestAdded), RedText)
                            } else {
                                if (payment > 0) PreviewRow("-Payment", fmt(payment), GreenText)
                                if (preview.shortfall > 0) PreviewRow("Shortfall vs. bill", fmt(preview.shortfall), AmberText)
                            }
                            if (preview.carryOverAdded > 0) PreviewRow("+Previous Balance", fmt(preview.carryOverAdded), BlueAccent)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            PreviewRow("Closing Balance", fmt(preview.closingBalance), bold = true)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    if (showDatePicker) {
        LedgerDatePickerDialog(
            initialMillis = state.paymentDateMillis,
            onConfirm = { millis ->
                onStateChange { copy(paymentDateMillis = millis) }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun PreviewRow(label: String, value: String, color: Color = Color.Unspecified, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = color)
    }
}

// ── Ledger Setup Card (shown when ledger enabled but no entries yet) ───────────

@Composable
private fun LedgerSetupCard(
    debt: com.cedd.utangtracker.data.local.entity.DebtEntity,
    autoStartBal: Double,
    onSave: (cycleMonths: Int, initialBal: Double) -> Unit
) {
    var cycleText by remember { mutableStateOf(debt.ledgerCycleMonths.toString()) }

    // Always auto-calculate from paidAmount — never pre-fill from saved override
    // so lender is always guided by the correct computed value
    val autoBal = (autoStartBal - debt.paidAmount).coerceAtLeast(0.0)
    var balText by remember {
        mutableStateOf(
            if (autoBal > 0.0 && autoBal < autoStartBal) "%.2f".format(autoBal) else ""
        )
    }

    val cycleMonths = cycleText.toIntOrNull()?.coerceAtLeast(1) ?: debt.ledgerCycleMonths
    val enteredBal = balText.toDoubleOrNull()
    val effectiveBal = enteredBal ?: autoStartBal

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = BlueAccent.copy(alpha = 0.1f)) {
                    Icon(Icons.Default.Settings, null, tint = BlueAccent, modifier = Modifier.padding(8.dp))
                }
                Column {
                    Text("Set Up Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Confirm the opening balance before tracking", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Loan Term
            OutlinedTextField(
                value = cycleText,
                onValueChange = { cycleText = it },
                label = { Text("Loan Term (months)") },
                placeholder = { Text("e.g. 3 = quarterly") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    val b = cycleText.toIntOrNull()?.coerceAtLeast(1) ?: debt.ledgerCycleMonths
                    Text("Monthly bill: ${fmt(LoanLedgerViewModel.calcMonthlyBill(debt.amount, debt.interestRate, b))}")
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Opening balance — app pre-fills from paidAmount, lender can correct
            OutlinedTextField(
                value = balText,
                onValueChange = { balText = it },
                label = { Text("Opening Balance") },
                prefix = { Text("₱") },
                placeholder = { Text(fmt(autoStartBal)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = {
                    when {
                        balText.isEmpty() ->
                            Text("Leave blank to use full loan amount: ${fmt(autoStartBal)}")
                        autoBal > 0.0 && autoBal < autoStartBal && enteredBal == autoBal ->
                            Text("Auto-calculated from ₱${fmt(autoStartBal)} − payments recorded (${fmt(debt.paidAmount)})")
                        else ->
                            Text("Adjust if needed — this is the balance the first month starts from")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Preview row
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("First month opens at", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(fmt(effectiveBal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BlueAccent)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Monthly bill", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text(fmt(LoanLedgerViewModel.calcMonthlyBill(debt.amount, debt.interestRate, cycleMonths)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Start button
            Button(
                onClick = { onSave(cycleMonths, enteredBal ?: 0.0) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BlueAccent)
            ) {
                Text("Start Tracking", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── Settings Panel ────────────────────────────────────────────────────────────

@Composable
private fun SettingsPanel(
    ledgerEnabled: Boolean,
    carryOver: Double,
    carryOverMonthly: Boolean,
    cycleMonths: Int,
    initialBalance: Double,
    hasEntries: Boolean,
    onLedgerEnabled: (Boolean) -> Unit,
    onCarryOver: (Double) -> Unit,
    onCarryOverMonthly: (Boolean) -> Unit,
    onCycleMonths: (Int) -> Unit,
    onInitialBalance: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, tonalElevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ledger Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onDismiss) { Text("Done") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Enable Ledger", style = MaterialTheme.typography.bodyMedium)
                    Text("Track monthly payment history", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Switch(checked = ledgerEnabled, onCheckedChange = onLedgerEnabled)
            }
        }
    }
}

// ── Date Picker Dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LedgerDatePickerDialog(
    initialMillis: Long?,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis ?: System.currentTimeMillis())
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp, modifier = Modifier.padding(horizontal = 16.dp)) {
            Column {
                DatePicker(state = state, showModeToggle = true)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { state.selectedDateMillis?.let { onConfirm(it) } ?: onDismiss() }) { Text("Confirm") }
                }
            }
        }
    }
}
