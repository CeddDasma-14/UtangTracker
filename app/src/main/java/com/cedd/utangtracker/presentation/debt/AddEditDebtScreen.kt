package com.cedd.utangtracker.presentation.debt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.utangtracker.domain.model.DebtType
import com.cedd.utangtracker.presentation.components.PremiumBadge
import com.cedd.utangtracker.presentation.components.PremiumUpgradeDialog
import com.cedd.utangtracker.presentation.components.formatPeso
import java.text.SimpleDateFormat
import java.util.*

/** Returns raw digits+dot string formatted with thousands commas, e.g. "1000000" → "1,000,000" */
private fun formatWithCommas(raw: String): String {
    if (raw.isEmpty()) return raw
    val parts = raw.split(".")
    val intFormatted = parts[0].reversed().chunked(3).joinToString(",").reversed()
    return if (parts.size > 1) "$intFormatted.${parts[1]}" else intFormatted
}

private data class LoanSummary(
    val months: Int,
    val monthlyInterest: Double,
    val totalInterest: Double,
    val totalPayable: Double,
    val hasDueDate: Boolean
)

private val MONTH_OPTIONS = listOf(1, 2, 3, 4, 5, 6, 9, 12, 18, 24)

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(
            label, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            value, fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtScreen(
    onDone: () -> Unit,
    onAddPerson: () -> Unit = {},
    vm: AddEditDebtViewModel = hiltViewModel()
) {
    val persons by vm.persons.collectAsStateWithLifecycle()
    val existing by vm.existing.collectAsStateWithLifecycle()
    val isPremium by vm.isPremium.collectAsStateWithLifecycle()
    val isEdit = existing != null

    var premiumDialogFeature by remember { mutableStateOf("") }
    var premiumDialogDesc    by remember { mutableStateOf("") }
    var showPremiumDialog    by remember { mutableStateOf(false) }

    var selectedPersonId by remember { mutableStateOf(vm.prefilledPersonId) }
    var type by remember { mutableStateOf(DebtType.OWED_TO_ME) }
    var amountText by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("0") }
    var autoApplyInterest by remember { mutableStateOf(false) }
    var contractEnabled by remember { mutableStateOf(false) }
    var dueDateMillis by remember { mutableStateOf<Long?>(null) }
    var personExpanded  by remember { mutableStateOf(false) }
    var showDatePicker  by remember { mutableStateOf(false) }
    var monthsExpanded  by remember { mutableStateOf(false) }
    var selectedMonths  by remember { mutableStateOf<Int?>(null) }
    var personError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var purposeError by remember { mutableStateOf(false) }

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LaunchedEffect(existing) {
        existing?.let { d ->
            selectedPersonId = d.personId
            type = DebtType.from(d.type)
            amountText = d.amount.toString()
            purpose = d.purpose
            notes = d.notes
            interestText = d.interestRate.toString()
            autoApplyInterest = d.autoApplyInterest
            contractEnabled = d.contractEnabled
            dueDateMillis = d.dateDue
        }
    }

    // Auto-enable contract when amount reaches ₱5,000 (premium only)
    LaunchedEffect(amountText, isPremium) {
        val a = amountText.toDoubleOrNull() ?: 0.0
        if (a >= 5_000.0 && !contractEnabled && isPremium) contractEnabled = true
    }

    val loanSummary = remember(amountText, interestText, dueDateMillis, selectedMonths) {
        val a = amountText.toDoubleOrNull() ?: 0.0
        val r = interestText.toDoubleOrNull() ?: 0.0
        if (a <= 0 || r <= 0) return@remember null
        val monthly = a * (r / 100.0)
        if (dueDateMillis != null) {
            val months: Int = selectedMonths ?: run {
                // Count calendar months from today to the picked date
                val now = Calendar.getInstance()
                val due = Calendar.getInstance().apply { timeInMillis = dueDateMillis!! }
                val diff = (due.get(Calendar.YEAR) - now.get(Calendar.YEAR)) * 12 +
                           (due.get(Calendar.MONTH) - now.get(Calendar.MONTH))
                maxOf(diff, 1)
            }
            val totalInterest = monthly * months
            LoanSummary(
                months = months, monthlyInterest = monthly,
                totalInterest = totalInterest, totalPayable = a + totalInterest,
                hasDueDate = true
            )
        } else {
            LoanSummary(
                months = 0, monthlyInterest = monthly,
                totalInterest = 0.0, totalPayable = a + monthly,
                hasDueDate = false
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Debt" else "Add Debt") },
                navigationIcon = {
                    IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Person dropdown ───────────────────────────────────────────────
            val selectedPerson = persons.find { it.id == selectedPersonId }
            ExposedDropdownMenuBox(expanded = personExpanded, onExpandedChange = { personExpanded = it }) {
                OutlinedTextField(
                    value = selectedPerson?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Person *") },
                    placeholder = { Text("Select Person") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(personExpanded) },
                    isError = personError,
                    supportingText = if (personError) { { Text("Please select a person") } } else null,
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = personExpanded, onDismissRequest = { personExpanded = false }) {
                    if (persons.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No persons yet", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                            onClick = {}
                        )
                    } else {
                        persons.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person.name) },
                                onClick = { selectedPersonId = person.id; personExpanded = false; personError = false }
                            )
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PersonAdd, null,
                                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text("Add New Person", color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        },
                        onClick = { personExpanded = false; onAddPerson() }
                    )
                }
            }

            // ── Type ──────────────────────────────────────────────────────────
            Text("Type", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = type == DebtType.OWED_TO_ME,
                    onClick = { type = DebtType.OWED_TO_ME }, label = { Text("Owed to Me") })
                FilterChip(selected = type == DebtType.I_OWE,
                    onClick = { type = DebtType.I_OWE }, label = { Text("I Owe") })
            }

            // ── Amount ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = formatWithCommas(amountText),
                onValueChange = { input ->
                    val raw = input.replace(",", "")
                    if (raw.matches(Regex("\\d*\\.?\\d*"))) {
                        amountText = raw
                        amountError = false
                    }
                },
                label = { Text("Amount (₱) *") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true,
                isError = amountError,
                supportingText = if (amountError) { { Text("Enter a valid amount") } } else null
            )

            // ── Purpose ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it; purposeError = false },
                label = { Text("Purpose *") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                isError = purposeError,
                supportingText = if (purposeError) { { Text("Purpose is required") } } else null
            )

            // ── Due Date ──────────────────────────────────────────────────────
            // Quick-select: pay in N months
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pay in:", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(
                    expanded = monthsExpanded,
                    onExpandedChange = { monthsExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (selectedMonths != null)
                            "$selectedMonths month${if (selectedMonths != 1) "s" else ""}"
                        else "Select months",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthsExpanded) },
                        singleLine = true,
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = monthsExpanded,
                        onDismissRequest = { monthsExpanded = false }
                    ) {
                        MONTH_OPTIONS.forEach { months ->
                            DropdownMenuItem(
                                text = { Text("$months month${if (months != 1) "s" else ""}") },
                                onClick = {
                                    selectedMonths = months
                                    monthsExpanded = false
                
                                    val cal = Calendar.getInstance().apply {
                                        add(Calendar.MONTH, months)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    dueDateMillis = cal.timeInMillis
                                }
                            )
                        }
                    }
                }
            }

            // Or pick a specific date
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (dueDateMillis != null) "Due: ${dateFmt.format(Date(dueDateMillis!!))}"
                     else "Pick specific date (optional)")
            }
            if (dueDateMillis != null) {
                TextButton(onClick = {
                    dueDateMillis  = null
                    selectedMonths = null

                }) {
                    Text("Clear due date", fontSize = 12.sp)
                }
            }

            // ── Interest ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = interestText, onValueChange = { interestText = it },
                label = { Text("Interest Rate (% per month, 0 = none)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true
            )

            if (loanSummary != null) {
                if (loanSummary.hasDueDate) {
                    // Full loan summary card
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(
                            Modifier.padding(16.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Loan Summary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            SummaryRow("Loan Amount", formatPeso(amountText.toDoubleOrNull() ?: 0.0))
                            SummaryRow("Monthly Interest (${interestText}%)", formatPeso(loanSummary.monthlyInterest))
                            SummaryRow(
                                "Term",
                                "${loanSummary.months} month${if (loanSummary.months != 1) "s" else ""}"
                            )
                            SummaryRow("Total Interest", formatPeso(loanSummary.totalInterest))
                            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            SummaryRow("Total Payable", formatPeso(loanSummary.totalPayable), bold = true)
                        }
                    }
                } else {
                    // Simple card — no due date yet
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "Monthly Interest: ${formatPeso(loanSummary.monthlyInterest)}",
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                            )
                            Text(
                                "Set a due date to see full loan summary",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Auto-apply toggle
                Card {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        Arrangement.SpaceBetween, Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f).padding(end = 8.dp)) {
                            Text("Auto-apply on overdue", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (autoApplyInterest)
                                    "${formatPeso(loanSummary.monthlyInterest)} added to debt each month when overdue"
                                else "Tap to enable automatic monthly interest on overdue",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoApplyInterest,
                            onCheckedChange = { checked ->
                                if (checked && !isPremium) {
                                    premiumDialogFeature = "Interest Auto-Apply"
                                    premiumDialogDesc = "Automatically add monthly interest to overdue debts so you never miss a calculation."
                                    showPremiumDialog = true
                                } else autoApplyInterest = checked
                            }
                        )
                    }
                }
            }

            // ── Contract Toggle ───────────────────────────────────────────────
            val amountVal = amountText.toDoubleOrNull() ?: 0.0
            Card {
                Row(
                    Modifier.padding(12.dp).fillMaxWidth(),
                    Arrangement.SpaceBetween, Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Digital Contract", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            if (amountVal >= 5_000.0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        "Auto",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (!isPremium) PremiumBadge()
                        }
                        Text(
                            if (contractEnabled)
                                "PDF contract will be generated. Can be used for barangay mediation."
                            else "Enable to require a signed PDF contract for this debt.",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = contractEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !isPremium) {
                                premiumDialogFeature = "Digital Contracts"
                                premiumDialogDesc = "Generate a signed PDF Kasunduan sa Pagpapautang — valid evidence for barangay mediation."
                                showPremiumDialog = true
                            } else contractEnabled = checked
                        }
                    )
                }
            }

            // ── Notes ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = notes, onValueChange = { notes = it },
                label = { Text("Notes (optional)") }, modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    personError = selectedPersonId == -1L
                    amountError = amount == null || amount <= 0
                    purposeError = purpose.isBlank()
                    if (personError || amountError || purposeError) return@Button
                    val interest = interestText.toDoubleOrNull() ?: 0.0
                    vm.save(selectedPersonId, type, amount!!, purpose, dueDateMillis,
                        interest, autoApplyInterest, contractEnabled, notes, onDone)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isEdit) "Update Debt" else "Save Debt") }
        }
    }

    if (showPremiumDialog) {
        PremiumUpgradeDialog(
            featureName = premiumDialogFeature,
            featureDescription = premiumDialogDesc,
            onUpgrade = { vm.setPremium(true); showPremiumDialog = false },
            onDismiss = { showPremiumDialog = false }
        )
    }

    // ── Date Picker Dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dueDateMillis  = dpState.selectedDateMillis
                    selectedMonths = null

                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = dpState) }
    }
}
