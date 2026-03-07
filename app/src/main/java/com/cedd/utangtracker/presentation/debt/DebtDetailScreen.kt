package com.cedd.utangtracker.presentation.debt

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cedd.utangtracker.data.local.entity.PaymentEntity
import com.cedd.utangtracker.domain.model.DebtType
import com.cedd.utangtracker.presentation.components.CurrencyText
import com.cedd.utangtracker.presentation.components.StatusBadge
import com.cedd.utangtracker.presentation.components.formatPeso
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onContract: (Long) -> Unit = {},
    onReloan: (Long) -> Unit = {},
    vm: DebtDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val lenderName by vm.lenderName.collectAsStateWithLifecycle()
    val debt = state.data?.debt
    var showPayDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showReceiptChooser by remember { mutableStateOf(false) }

    // Camera: we need a content URI prepared before launching TakePicture
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.addDisbursementReceipt(it) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) cameraUri?.let { vm.addDisbursementReceipt(it) }
        cameraUri = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.person?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    debt?.let {
                        IconButton(onClick = { onEdit(it.id) }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFC62828))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (debt == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val remaining = debt.amount - debt.paidAmount
        val isOwedToMe = debt.type == DebtType.OWED_TO_ME.value

        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp)) {
            item {
                // Summary card
                Card(Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(4.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                            Text(if (isOwedToMe) "They owe you" else "You owe", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            StatusBadge(debt.status)
                        }
                        CurrencyText(
                            remaining,
                            color = if (isOwedToMe) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 28.sp, fontWeight = FontWeight.Bold
                        )
                        Text("Original: ${formatPeso(debt.amount)} · Paid: ${formatPeso(debt.paidAmount)}",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Text("Purpose: ${debt.purpose}")
                        debt.dateDue?.let { Text("Due: ${dateFmt.format(Date(it))}") }
                        if (debt.interestRate > 0) Text("Interest: ${debt.interestRate}% / month")
                        if (debt.notes.isNotBlank()) Text("Notes: ${debt.notes}")
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (debt.autoApplyInterest && debt.interestRate > 0) {
                    val monthlyInterest = debt.amount * (debt.interestRate / 100.0)
                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            Arrangement.SpaceBetween, Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Monthly Interest Due",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    formatPeso(monthlyInterest) + " / month",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Button(
                                onClick = { vm.applyMonthlyInterest() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Apply") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (debt.status != "SETTLED") {
                    Button(onClick = { showPayDialog = true }, Modifier.fillMaxWidth()) {
                        Text("Record Payment")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (debt.contractEnabled) {
                    OutlinedButton(onClick = { onContract(debt.id) }, Modifier.fillMaxWidth()) {
                        Text("View / Create Contract")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── SMS Reminder ──────────────────────────────────────────────
                val personPhone = state.person?.phone
                val personName  = state.person?.name ?: ""
                if (debt.status != "SETTLED" && !personPhone.isNullOrBlank()) {
                    val signature = lenderName.trim().let { full ->
                        if (full.isBlank()) ""
                        else {
                            val parts = full.split(" ").filter { it.isNotBlank() }
                            if (parts.size >= 2) "${parts[0]} ${parts.last().first().uppercaseChar()}."
                            else parts[0]
                        }
                    }
                    val smsBody = buildString {
                        append("Hi $personName! Just a reminder that you have an outstanding debt of ${formatPeso(remaining)}")
                        append(" for \"${debt.purpose}\".")
                        debt.dateDue?.let { append(" Due date: ${dateFmt.format(Date(it))}.") }
                        append(" Please settle at your earliest convenience. Thank you!")
                        if (signature.isNotBlank()) append("\n\n- $signature")
                    }
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$personPhone"))
                                .putExtra("sms_body", smsBody)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sms, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send SMS Reminder")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Share Payment Summary ─────────────────────────────────────
                val payments = state.data?.payments ?: emptyList()
                if (payments.isNotEmpty()) {
                    val summaryText = buildString {
                        append("Payment Summary — ${state.person?.name ?: ""}\n")
                        append("Purpose: ${debt.purpose}\n")
                        append("Amount: ${formatPeso(debt.amount)}\n")
                        append("Paid: ${formatPeso(debt.paidAmount)}\n")
                        append("Remaining: ${formatPeso(remaining)}\n")
                        append("Status: ${debt.status}\n\n")
                        append("Payments:\n")
                        payments.forEach { p ->
                            append("  • ${dateFmt.format(Date(p.datePaid))}: ${formatPeso(p.amount)}")
                            if (p.notes.isNotBlank()) append(" (${p.notes})")
                            append("\n")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, summaryText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Payment Summary"))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Payment Summary")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Reloan ────────────────────────────────────────────────────
                if (debt.status == "SETTLED") {
                    val reloanPersonId = debt.personId
                    OutlinedButton(
                        onClick = { onReloan(reloanPersonId) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("New Loan for Same Person")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── Disbursement receipts ─────────────────────────────────────
                DisbursementReceiptSection(
                    receiptPaths = debt.disbursementReceiptPaths
                        ?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
                    onAdd = { showReceiptChooser = true },
                    onRemove = { vm.removeDisbursementReceipt(it) }
                )
                Spacer(Modifier.height(12.dp))

                Text("Payment History", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
            }

            val payments = state.data?.payments ?: emptyList()
            if (payments.isEmpty()) {
                item { Text("No payments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(payments, key = { it.id }) { payment ->
                PaymentRow(payment, onDelete = { vm.deletePayment(payment) })
            }
            }
        }
    }

    // Receipt source chooser
    if (showReceiptChooser) {
        AlertDialog(
            onDismissRequest = { showReceiptChooser = false },
            title = { Text("Add Receipt") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            showReceiptChooser = false
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Choose from Gallery") }
                    OutlinedButton(
                        onClick = {
                            showReceiptChooser = false
                            try {
                                val tempDir = java.io.File(context.cacheDir, "camera_temp").also { it.mkdirs() }
                                val tempFile = java.io.File(tempDir, "receipt_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Camera not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Take a Photo") }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReceiptChooser = false }) { Text("Cancel") }
            }
        )
    }

    if (showPayDialog) {
        AddPaymentDialog(
            onConfirm = { amount, notes ->
                vm.addPayment(amount, notes)
                showPayDialog = false
            },
            onDismiss = { showPayDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Debt?") },
            text = { Text("This will permanently delete the debt and all payments.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteDebt(onBack) }) { Text("Delete", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

}

@Composable
private fun DisbursementReceiptSection(
    receiptPaths: List<String>,
    onAdd: () -> Unit,
    onRemove: (String) -> Unit
) {
    var fullscreenPath by remember { mutableStateOf<String?>(null) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Proof of Disbursement", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(
                        "Screenshots proving money was sent to borrower",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAdd, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Add, "Add receipt",
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (receiptPaths.isEmpty()) {
                Text(
                    "No receipts yet. Tap + to upload a screenshot of the money transfer.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                receiptPaths.forEachIndexed { index, path ->
                    val file = File(path)
                    if (!file.exists()) return@forEachIndexed
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Receipt ${index + 1}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                IconButton(onClick = { onRemove(path) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Remove",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            AsyncImage(
                                model = file,
                                contentDescription = "Receipt ${index + 1}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .background(Color(0xFFF5F5F5), MaterialTheme.shapes.small)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                    .padding(4.dp)
                                    .clickable { fullscreenPath = path }
                            )
                            Text("Tap to view full size", fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Another Receipt", fontSize = 13.sp)
                }
            }
        }
    }

    if (fullscreenPath != null) {
        val fsFile = File(fullscreenPath!!)
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        val transformState = rememberTransformableState { zoomChange, panChange, _ ->
            scale = (scale * zoomChange).coerceIn(1f, 6f)
            offset += panChange
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { fullscreenPath = null; scale = 1f; offset = Offset.Zero },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                AsyncImage(
                    model = fsFile,
                    contentDescription = "Receipt full view",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y }
                        .transformable(transformState)
                )
                IconButton(
                    onClick = { fullscreenPath = null; scale = 1f; offset = Offset.Zero },
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, "Close", tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Text(
                    "Pinch to zoom  •  tap × to close",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: PaymentEntity, onDelete: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )) {
        Row(
            Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp).fillMaxWidth(),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(dateFmt.format(Date(payment.datePaid)), fontSize = 13.sp)
                if (payment.notes.isNotBlank())
                    Text(payment.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CurrencyText(payment.amount, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { showConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, "Delete payment",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Delete payment?") },
            text = { Text("This will reverse the payment amount from the debt balance.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AddPaymentDialog(onConfirm: (Double, String) -> Unit, onDismiss: () -> Unit) {
    var amountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text("Amount (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes (optional)") }, singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull() ?: return@TextButton
                if (amount > 0) onConfirm(amount, notes)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
