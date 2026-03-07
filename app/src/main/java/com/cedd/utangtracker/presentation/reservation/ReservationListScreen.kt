package com.cedd.utangtracker.presentation.reservation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cedd.utangtracker.data.local.entity.ReservationEntity
import com.cedd.utangtracker.domain.model.DebtType
import com.cedd.utangtracker.presentation.components.formatPeso
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReservationListScreen(
    onAddReservation: () -> Unit,
    onEditReservation: (Long) -> Unit,
    onDebtCreated: (Long) -> Unit,
    vm: ReservationListViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    val filterOptions = listOf("ALL", "PENDING", "APPROVED", "REJECTED", "CONVERTED")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Reservations", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReservation,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Reservation")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // ── Filter chips ─────────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { option ->
                    FilterChip(
                        selected = state.filterStatus == option,
                        onClick = { vm.setFilter(option) },
                        label = {
                            Text(
                                when (option) {
                                    "ALL" -> "All"
                                    "PENDING" -> "Pending"
                                    "APPROVED" -> "Approved"
                                    "REJECTED" -> "Rejected"
                                    "CONVERTED" -> "Converted"
                                    else -> option
                                }
                            )
                        }
                    )
                }
            }

            if (state.reservations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No reservations yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Tap + to record a future loan request",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.reservations, key = { it.id }) { reservation ->
                        val personName = state.persons.find { it.id == reservation.personId }?.name ?: "Unknown"
                        ReservationCard(
                            reservation = reservation,
                            personName = personName,
                            onEdit = { onEditReservation(reservation.id) },
                            onDelete = { vm.deleteReservation(reservation) },
                            onApprove = { vm.updateStatus(reservation, "APPROVED") },
                            onReject = { vm.updateStatus(reservation, "REJECTED") },
                            onConvert = { vm.convertToDebt(reservation) { debtId -> onDebtCreated(debtId) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: ReservationEntity,
    personName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onConvert: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val statusColor = when (reservation.status) {
        "APPROVED"  -> Color(0xFF2E7D32)
        "REJECTED"  -> Color(0xFFC62828)
        "CONVERTED" -> Color(0xFF1565C0)
        else        -> Color(0xFFF57F17) // PENDING = amber
    }

    val typeLabel = if (reservation.type == DebtType.OWED_TO_ME.value)
        "$personName will borrow from you"
    else
        "You will borrow from $personName"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(personName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(typeLabel, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        reservation.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Amount + purpose
            Text(formatPeso(reservation.amount), fontWeight = FontWeight.SemiBold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary)
            Text(reservation.purpose, fontSize = 14.sp)

            // Planned date
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(
                    "Planned: ${dateFmt.format(Date(reservation.plannedDate))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (reservation.notes.isNotBlank()) {
                Text(reservation.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (reservation.status) {
                    "PENDING" -> {
                        OutlinedButton(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f)
                        ) { Text("Approve", fontSize = 12.sp) }

                        OutlinedButton(
                            onClick = onReject,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Reject", fontSize = 12.sp) }

                        TextButton(onClick = onEdit) { Text("Edit", fontSize = 12.sp) }
                    }
                    "APPROVED" -> {
                        Button(
                            onClick = onConvert,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Convert to Debt", fontSize = 12.sp)
                        }
                        TextButton(onClick = onEdit) { Text("Edit", fontSize = 12.sp) }
                    }
                    "REJECTED", "CONVERTED" -> {
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, null, Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                        }
                        if (reservation.status == "REJECTED") {
                            TextButton(onClick = onApprove) { Text("Re-approve", fontSize = 12.sp) }
                        }
                    }
                }

                if (reservation.status == "PENDING" || reservation.status == "APPROVED") {
                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Reservation?") },
            text = { Text("This reservation will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
