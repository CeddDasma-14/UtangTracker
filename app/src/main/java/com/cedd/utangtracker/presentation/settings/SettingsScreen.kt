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
import androidx.compose.material.icons.filled.TableChart
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

    var showImportConfirm by remember { mutableStateOf(false) }
    var pendingImportUri  by remember { mutableStateOf<android.net.Uri?>(null) }

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
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text("Biometric Lock", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text(
                                "Require fingerprint or face unlock on open",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { vm.toggleBiometric(it) }
                    )
                }
            }

            // ── Backup & Restore ─────────────────────────────────────────────
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Backup & Restore", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                            onClick = { vm.exportBackup() },
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
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
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

            // ── Reports ──────────────────────────────────────────────────────
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reports", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Text(
                        "Export all debts as a CSV file. Open it in Google Sheets or Excel to analyze and share.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    Button(
                        onClick = { vm.exportCsv() },
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
                            "Utang Tracker \u00b7 v1.0.0",
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
}
