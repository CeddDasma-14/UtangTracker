package com.cedd.utangtracker.presentation.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.TableChart
import com.cedd.utangtracker.presentation.components.PremiumBadge
import com.cedd.utangtracker.presentation.components.PremiumUpgradeDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
    val isDarkMode         by vm.isDarkMode.collectAsStateWithLifecycle()
    val isBiometricEnabled by vm.isBiometricEnabled.collectAsStateWithLifecycle()
    val backupStatus       by vm.backupStatus.collectAsStateWithLifecycle()
    val lenderName         by vm.lenderName.collectAsStateWithLifecycle()
    val isPremium          by vm.isPremium.collectAsStateWithLifecycle()

    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri  by remember { mutableStateOf<android.net.Uri?>(null) }
    var showSpreadsheetImportConfirm by remember { mutableStateOf(false) }
    var pendingSpreadsheetUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showCsvImportConfirm by remember { mutableStateOf(false) }
    var pendingCsvUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var sheetsUrl by remember { mutableStateOf("") }
    var showSheetsImportConfirm by remember { mutableStateOf(false) }
    var premiumDialogFeature by remember { mutableStateOf("") }
    var premiumDialogDesc    by remember { mutableStateOf("") }
    var showPremiumDialog    by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar for Success / Error, then reset
    LaunchedEffect(backupStatus) {
        when (val s = backupStatus) {
            is BackupStatus.Success -> {
                snackbarHostState.showSnackbar(s.message)
                vm.clearBackupStatus()
            }
            is BackupStatus.Error -> {
                snackbarHostState.showSnackbar(s.message)
                vm.clearBackupStatus()
            }
            else -> Unit
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
            showImportConfirm = true
        }
    }

    val spreadsheetImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingSpreadsheetUri = uri
            showSpreadsheetImportConfirm = true
        }
    }

    val csvImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingCsvUri = uri
            showCsvImportConfirm = true
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Premium ──────────────────────────────────────────────────────
            if (isPremium) {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "\u2B50 Premium Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "All premium features are unlocked.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        TextButton(onClick = { vm.setPremium(false) }) {
                            Text("Revoke", fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                        }
                    }
                }
            } else {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "\u2B50 Upgrade to Premium",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "Unlock: Unlimited debts & persons, Digital Contracts, Backup & Restore, CSV Export, Biometric Lock, SMS Reminders, Interest Auto-Apply, Share Payment Summary.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            lineHeight = 17.sp
                        )
                        Button(
                            onClick = {
                                premiumDialogFeature = ""
                                premiumDialogDesc    = ""
                                showPremiumDialog    = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) { Text("Unlock Premium") }
                    }
                }
            }

            // ── Dark Mode ────────────────────────────────────────────────────
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text(
                            "Switch to dark theme", fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { vm.toggleDarkMode(it) })
                }
            }

            // ── Lender Name ──────────────────────────────────────────────────
            val keyboard = LocalSoftwareKeyboardController.current
            var nameInput by remember(lenderName) { mutableStateOf(lenderName) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your Name", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Used as your signature at the end of SMS reminders.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = { Text("e.g. Juan D.", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            vm.setLenderName(nameInput.trim())
                            keyboard?.hide()
                        })
                    )
                    if (nameInput.trim() != lenderName) {
                        Button(
                            onClick = { vm.setLenderName(nameInput.trim()); keyboard?.hide() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) { Text("Save Name") }
                    }
                }
            }

            // ── Biometric Lock ───────────────────────────────────────────────
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    Arrangement.SpaceBetween,
                    Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isPremium) Icons.Default.Fingerprint else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isPremium) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Biometric Lock", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                                if (!isPremium) PremiumBadge()
                            }
                            Text(
                                "Require fingerprint or face unlock on open",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { checked ->
                            if (checked && !isPremium) {
                                premiumDialogFeature = "Biometric Lock"
                                premiumDialogDesc = "Protect your debt data with fingerprint or face unlock every time you open the app."
                                showPremiumDialog = true
                            } else {
                                vm.toggleBiometric(checked)
                            }
                        }
                    )
                }
            }

            // ── Backup & Restore ─────────────────────────────────────────────
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Backup & Restore", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        if (!isPremium) PremiumBadge()
                    }
                    Text(
                        "Export all persons, debts, and payments to a JSON file. " +
                        "Save it to Google Drive or send via email for safekeeping.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    val isWorking = backupStatus is BackupStatus.Working

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Export
                        Button(
                            onClick = {
                                if (!isPremium) {
                                    premiumDialogFeature = "Backup & Restore"
                                    premiumDialogDesc = "Export all your debts, persons, and payments to a JSON file. Restore them anytime."
                                    showPremiumDialog = true
                                } else vm.exportBackup()
                            },
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isWorking) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, null, Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }

                        // Import
                        OutlinedButton(
                            onClick = {
                                if (!isPremium) {
                                    premiumDialogFeature = "Backup & Restore"
                                    premiumDialogDesc = "Export all your debts, persons, and payments to a JSON file. Restore them anytime."
                                    showPremiumDialog = true
                                } else importLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }

                    Text(
                        "Note: Restore merges backup data with existing records. " +
                        "Receipt images and contract PDFs are not included in the backup.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // ── Spreadsheet Import ───────────────────────────────────────────
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Spreadsheet Import", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        if (!isPremium) PremiumBadge()
                    }
                    Text(
                        "Import your existing loans directly from Excel or Google Sheets. " +
                        "Just export your spreadsheet as a CSV file and import it here.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    val isWorking = backupStatus is BackupStatus.Working

                    // Google Sheets URL paste
                    OutlinedTextField(
                        value = sheetsUrl,
                        onValueChange = { sheetsUrl = it },
                        placeholder = { Text("Paste Google Sheets link here…", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (sheetsUrl.isNotBlank()) {
                                if (!isPremium) {
                                    premiumDialogFeature = "Spreadsheet Import"
                                    premiumDialogDesc = "Import loans directly from Google Sheets, CSV, or Excel files."
                                    showPremiumDialog = true
                                } else showSheetsImportConfirm = true
                            }
                        })
                    )
                    Button(
                        onClick = {
                            if (!isPremium) {
                                premiumDialogFeature = "Spreadsheet Import"
                                premiumDialogDesc = "Import loans directly from Google Sheets, CSV, or Excel files."
                                showPremiumDialog = true
                            } else if (sheetsUrl.isNotBlank()) showSheetsImportConfirm = true
                        },
                        enabled = !isWorking && sheetsUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.CloudDownload, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Import from Google Sheets Link")
                        if (!isPremium) {
                            Spacer(Modifier.width(6.dp))
                            PremiumBadge()
                        }
                    }
                    Text(
                        "Share your Google Sheets → tap Share → Copy link → paste it above.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider()

                    // Primary: CSV import
                    Button(
                        onClick = {
                            if (!isPremium) {
                                premiumDialogFeature = "Spreadsheet Import"
                                premiumDialogDesc = "Import loans directly from Google Sheets, CSV, or Excel files."
                                showPremiumDialog = true
                            } else csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                        },
                        enabled = !isWorking,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isWorking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Import CSV or Excel (.xlsx)")
                        if (!isPremium) {
                            Spacer(Modifier.width(6.dp))
                            PremiumBadge()
                        }
                    }
                    Text(
                        "How: In Excel → File → Save As → CSV. In Google Sheets → File → Download → CSV. " +
                        "Required columns: NAME, AMOUNT. Optional: DATE, DUE DATE, %, BANK CHARGE, MONTH, COLLECTED MONEY.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    HorizontalDivider()

                    // Secondary: JSON template
                    Text("Or use a JSON template", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!isPremium) {
                                    premiumDialogFeature = "Spreadsheet Import"
                                    premiumDialogDesc = "Import loans directly from Google Sheets, CSV, or Excel files."
                                    showPremiumDialog = true
                                } else vm.exportSpreadsheetTemplate()
                            },
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileDownload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Get Template")
                            if (!isPremium) {
                                Spacer(Modifier.width(6.dp))
                                PremiumBadge()
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (!isPremium) {
                                    premiumDialogFeature = "Spreadsheet Import"
                                    premiumDialogDesc = "Import loans directly from Google Sheets, CSV, or Excel files."
                                    showPremiumDialog = true
                                } else spreadsheetImportLauncher.launch(arrayOf("application/json", "*/*"))
                            },
                            enabled = !isWorking,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Import JSON")
                            if (!isPremium) {
                                Spacer(Modifier.width(6.dp))
                                PremiumBadge()
                            }
                        }
                    }
                }
            }

            // ── Reports ──────────────────────────────────────────────────────
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Reports", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        if (!isPremium) PremiumBadge()
                    }
                    Text(
                        "Export all debts as a CSV file. Open it in Google Sheets or Excel to analyze and share.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = {
                            if (!isPremium) {
                                premiumDialogFeature = "CSV Export"
                                premiumDialogDesc = "Export all your debts to a spreadsheet. Open in Google Sheets or Excel for analysis."
                                showPremiumDialog = true
                            } else vm.exportCsv()
                        },
                        enabled = backupStatus !is BackupStatus.Working,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.TableChart, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Export CSV Report")
                    }
                }
            }

            // ── About / Developer Signature ───────────────────────────────────
            val sigGradient = Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                )
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(sigGradient)
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "A  P R O J E C T  B Y",
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Cedd",
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(0.35f),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "LoanTrack \u00b7 v1.0.0",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "\u00a9 2026 \u2014 All rights reserved",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
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

    // Confirm before restoring
    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; pendingImportUri = null },
            title = { Text("Restore Backup?") },
            text  = {
                Text(
                    "Existing records with matching IDs will be overwritten by the backup data. " +
                    "New records added after the backup will remain untouched. Continue?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri?.let { vm.importBackup(it) }
                    showImportConfirm = false
                    pendingImportUri = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false; pendingImportUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm CSV import
    if (showCsvImportConfirm) {
        AlertDialog(
            onDismissRequest = { showCsvImportConfirm = false; pendingCsvUri = null },
            title = { Text("Import from CSV?") },
            text  = {
                Text(
                    "Each row in the CSV will be added as a new debt. " +
                    "Persons are created automatically by name. Continue?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingCsvUri?.let { vm.importFromCsv(it) }
                    showCsvImportConfirm = false
                    pendingCsvUri = null
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showCsvImportConfirm = false; pendingCsvUri = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm Google Sheets URL import
    if (showSheetsImportConfirm) {
        AlertDialog(
            onDismissRequest = { showSheetsImportConfirm = false },
            title = { Text("Import from Google Sheets?") },
            text  = {
                Text(
                    "The app will download your sheet as CSV and import each row as a new debt. " +
                    "Make sure the sheet is shared publicly (\"Anyone with the link can view\"). Continue?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importFromGoogleSheetsUrl(sheetsUrl.trim())
                    showSheetsImportConfirm = false
                    sheetsUrl = ""
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showSheetsImportConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirm spreadsheet JSON import
    if (showSpreadsheetImportConfirm) {
        AlertDialog(
            onDismissRequest = { showSpreadsheetImportConfirm = false; pendingSpreadsheetUri = null },
            title = { Text("Import Spreadsheet?") },
            text  = {
                Text(
                    "Records in the JSON file will be added as new debts. " +
                    "Persons will be created automatically if they don't exist yet. Continue?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingSpreadsheetUri?.let { vm.importSpreadsheet(it) }
                    showSpreadsheetImportConfirm = false
                    pendingSpreadsheetUri = null
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSpreadsheetImportConfirm = false
                    pendingSpreadsheetUri = null
                }) { Text("Cancel") }
            }
        )
    }
}
