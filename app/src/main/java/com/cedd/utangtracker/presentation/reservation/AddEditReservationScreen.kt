package com.cedd.utangtracker.presentation.reservation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.utangtracker.domain.model.DebtType
import java.text.SimpleDateFormat
import java.util.*

private fun formatWithCommas(raw: String): String {
    if (raw.isEmpty()) return raw
    val parts = raw.split(".")
    val intPart = parts[0].toBigIntegerOrNull()?.let {
        "%,d".format(it.toLong())
    } ?: parts[0]
    return if (parts.size > 1) "$intPart.${parts[1]}" else intPart
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReservationScreen(
    onDone: () -> Unit,
    vm: AddEditReservationViewModel = hiltViewModel()
) {
    val persons by vm.persons.collectAsStateWithLifecycle()
    val existing by vm.existing.collectAsStateWithLifecycle()
    val isEdit = existing != null

    var selectedPersonId by remember { mutableStateOf(vm.prefilledPersonId) }
    var type by remember { mutableStateOf(DebtType.OWED_TO_ME) }
    var amountText by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var plannedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPersonDropdown by remember { mutableStateOf(false) }

    val dateFmt = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    // Pre-fill when editing
    LaunchedEffect(existing) {
        existing?.let { r ->
            selectedPersonId = r.personId
            type = if (r.type == DebtType.OWED_TO_ME.value) DebtType.OWED_TO_ME else DebtType.I_OWE
            amountText = r.amount.toBigDecimal().stripTrailingZeros().toPlainString()
            purpose = r.purpose
            notes = r.notes
            plannedDateMillis = r.plannedDate
        }
    }

    val selectedPerson = persons.find { it.id == selectedPersonId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Reservation" else "New Reservation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Person Picker ─────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = showPersonDropdown,
                onExpandedChange = { showPersonDropdown = it }
            ) {
                OutlinedTextField(
                    value = selectedPerson?.name ?: "Select Person",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Person") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPersonDropdown) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = showPersonDropdown,
                    onDismissRequest = { showPersonDropdown = false }
                ) {
                    persons.forEach { person ->
                        DropdownMenuItem(
                            text = { Text(person.name) },
                            onClick = { selectedPersonId = person.id; showPersonDropdown = false }
                        )
                    }
                }
            }

            // ── Type Toggle ───────────────────────────────────────────────────
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DebtType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(if (t == DebtType.OWED_TO_ME) "They will borrow from me" else "I will borrow from them") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Amount ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = formatWithCommas(amountText),
                onValueChange = { raw ->
                    val cleaned = raw.replace(",", "").filter { it.isDigit() || it == '.' }
                    if (cleaned.count { it == '.' } <= 1) amountText = cleaned
                },
                label = { Text("Amount (₱)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₱") }
            )

            // ── Purpose ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Purpose / Reason for Borrowing") },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Planned Date ──────────────────────────────────────────────────
            OutlinedTextField(
                value = plannedDateMillis?.let { dateFmt.format(Date(it)) } ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Planned Date (when they want to borrow)") },
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, "Pick date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Notes ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // ── Save Button ───────────────────────────────────────────────────
            val amount = amountText.toDoubleOrNull()
            val canSave = selectedPersonId != -1L && amount != null && amount > 0
                && purpose.isNotBlank() && plannedDateMillis != null

            Button(
                onClick = {
                    vm.save(selectedPersonId, type, amount!!, purpose, plannedDateMillis!!, notes, onDone)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isEdit) "Update Reservation" else "Save Reservation")
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Date Picker Dialog ────────────────────────────────────────────────────
    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = plannedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    plannedDateMillis = dpState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) { DatePicker(state = dpState) }
    }
}
