package com.cedd.utangtracker.presentation.contract

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.cedd.utangtracker.pdf.ContractStrings
import com.cedd.utangtracker.presentation.components.CurrencyText
import com.cedd.utangtracker.presentation.components.formatPeso
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractScreen(
    onBack: () -> Unit,
    vm: ContractViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val comakers by vm.comakers.collectAsStateWithLifecycle()
    val contract = state.contract
    val debt = state.debt

    val idPhotoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { vm.saveIdPhoto(it) }
    }
    // Setup form state
    var lenderName by remember { mutableStateOf("") }
    var borrowerName by remember { mutableStateOf(state.person?.name ?: "") }
    var collateral by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("en") }
    var showSetup by remember { mutableStateOf(contract == null) }
    var signingRole by remember { mutableStateOf<String?>(null) }  // "lender" | "borrower" | "witness"

    LaunchedEffect(state.person) {
        if (borrowerName.isBlank()) borrowerName = state.person?.name ?: ""
    }
    LaunchedEffect(contract) {
        showSetup = contract == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasunduan sa Utang") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (contract?.isSigned == 1) {
                        IconButton(onClick = { vm.exportAndShare() }) {
                            Icon(Icons.Default.Share, "Share PDF")
                        }
                    }
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
            // ── Setup form ────────────────────────────────────────────────────
            if (showSetup && debt != null) {
                Text("Contract Setup", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                OutlinedTextField(lenderName, { lenderName = it }, label = { Text("Lender Name *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(borrowerName, { borrowerName = it }, label = { Text("Borrower Name *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    collateral, { collateral = it },
                    label = { Text("Collateral (optional)") },
                    placeholder = { Text("e.g. Samsung Galaxy S21, ATM card, laptop") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 3
                )
                // Language selector
                Text("Contract Language", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("en" to "English", "tl" to "Tagalog", "bis" to "Bisaya").forEach { (code, label) ->
                        FilterChip(
                            selected = language == code,
                            onClick = { language = code },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Button(
                    onClick = {
                        if (lenderName.isNotBlank() && borrowerName.isNotBlank())
                            vm.createContract(lenderName.trim(), borrowerName.trim(), null, collateral.trim(), language)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create Contract") }
                return@Column
            }

            if (contract == null || debt == null) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                return@Column
            }

            // ── Contract preview (full document) ─────────────────────────────
            val s = remember(contract.language) { ContractStrings.forLang(contract.language) }
            val cal = remember(contract.dateCreated) {
                java.util.Calendar.getInstance().apply { timeInMillis = contract.dateCreated }
            }
            val contractMonth = remember(contract.dateCreated) {
                SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date(contract.dateCreated))
            }
            val amtWords = remember(contract.amount) { contractAmountToWords(contract.amount) }
            val hasCollateral = !contract.collateral.isNullOrBlank()
            val dueStr = contract.dateDue?.let { dateFmt.format(Date(it)) } ?: "N/A"

            Card(
                Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Title
                    Text(s.title, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                    Text(s.subtitle, fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp))

                    // Opening paragraph
                    Text(
                        s.openingFn(
                            cal.get(java.util.Calendar.DAY_OF_MONTH),
                            contractMonth,
                            cal.get(java.util.Calendar.YEAR),
                            contract.lenderName,
                            contract.borrowerName
                        ),
                        fontSize = 12.sp, lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(4.dp))

                    // Section 1
                    Text("1. ${s.sec1}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    DocBullet(s.amountBullet(formatPeso(contract.amount), amtWords))
                    DocBullet(s.purposeBullet(contract.purpose))
                    DocBullet(if (contract.interestRate > 0) s.interestBullet(contract.interestRate) else s.interestNone)
                    DocBullet(s.dueBullet(dueStr))
                    Spacer(Modifier.height(4.dp))

                    // Section 2
                    Text("2. ${s.sec2}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    DocBullet(s.repay1); DocBullet(s.repay2); DocBullet(s.repay3); DocBullet(s.repay4)
                    Spacer(Modifier.height(4.dp))

                    // Section 3: Collateral (optional)
                    if (hasCollateral) {
                        Text("3. ${s.sec3Collateral}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        DocBullet(s.col1(contract.collateral!!))
                        DocBullet(s.col2); DocBullet(s.col3); DocBullet(s.col4); DocBullet(s.col5)
                        Spacer(Modifier.height(4.dp))
                    }

                    // Section 3 or 4: Digital Signatures
                    Text("${if (hasCollateral) "4" else "3"}. ${s.sec4Signatures}",
                        fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(s.sigIntro, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(6.dp))

                    // Signature status with images
                    SignatureStatus(s.sigLender, contract.lenderSignaturePath)
                    SignatureStatus(s.sigBorrower, contract.borrowerSignaturePath)
                    contract.witnessName?.let { SignatureStatus("${s.sigWitness} $it", contract.witnessSignaturePath) }

                    // Borrower signed timestamp
                    contract.borrowerSignedAt?.let { ts ->
                        val tsFmt = SimpleDateFormat("MMMM d, yyyy  h:mm a", Locale.ENGLISH)
                        Text("${s.borrowerSignedLabel} ${tsFmt.format(Date(ts))}",
                            fontSize = 11.sp, color = Color(0xFF2E7D32))
                    }

                    if (contract.isSigned == 1) {
                        Spacer(Modifier.height(4.dp))
                        Text("CONTRACT LOCKED — Both parties have signed.",
                            color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    // Footer
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(4.dp))
                    Text("Contract No: ${contract.contractNumber}  ·  ${dateFmt.format(Date(contract.dateCreated))}",
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.footerDisclaimer,
                        fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // ── Comakers ──────────────────────────────────────────────────────
            ComakerSection(comakers = comakers, onAdd = { n, m, a -> vm.addComaker(n, m, a) }, onDelete = { vm.deleteComaker(it) })

            // ── Remote Signing ────────────────────────────────────────────────
            if (contract.isSigned == 0) {
                RemoteLinkSection(
                    state = state,
                    onGenerate = { vm.generateLink() },
                    onRegenerate = { vm.regenerateLink() },
                    onAddIdPhoto = { idPhotoLauncher.launch("image/*") }
                )
            }

            // ── Signing section ───────────────────────────────────────────────
            if (contract.isSigned == 0) {
                Text("Signatures", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                if (signingRole == null) {
                    if (contract.lenderSignaturePath == null) {
                        OutlinedButton(onClick = { signingRole = "lender" }, Modifier.fillMaxWidth()) {
                            Text("Sign as Lender")
                        }
                    }
                    if (contract.borrowerSignaturePath == null) {
                        OutlinedButton(onClick = { signingRole = "borrower" }, Modifier.fillMaxWidth()) {
                            Text("Sign as Borrower")
                        }
                    }
                    contract.witnessName?.let {
                        if (contract.witnessSignaturePath == null) {
                            OutlinedButton(onClick = { signingRole = "witness" }, Modifier.fillMaxWidth()) {
                                Text("Sign as Witness")
                            }
                        }
                    }
                } else {
                    // Show signature pad for current role
                    Text("Sign as ${signingRole!!.replaceFirstChar { it.uppercase() }}",
                        fontWeight = FontWeight.Medium)
                    SignaturePad(
                        modifier = Modifier.fillMaxWidth(),
                        onSave = { paths, w, h ->
                            vm.saveSignature(signingRole!!, paths, w, h)
                            signingRole = null
                        },
                        onClear = {}
                    )
                    TextButton(onClick = { signingRole = null }) { Text("Cancel") }
                }
            }

            // ── Export buttons ────────────────────────────────────────────────
            if (contract.isSigned == 1) {
                Button(onClick = { vm.exportAndShare() }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export Loan Agreement")
                }
            }
            if (contract.remoteBorrowerStatus == "completed") {
                OutlinedButton(onClick = { vm.exportAcknowledgment() }, Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export Borrower's Acknowledgment")
                }
            }

        }
    }
}

@Composable
private fun ComakerSection(
    comakers: List<com.cedd.utangtracker.data.local.entity.ComakerEntity>,
    onAdd: (String, String, String) -> Unit,
    onDelete: (com.cedd.utangtracker.data.local.entity.ComakerEntity) -> Unit
) {
    var showForm by remember { mutableStateOf(false) }
    var name    by remember { mutableStateOf("") }
    var mobile  by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Alternative Contacts (Comakers)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                IconButton(onClick = { showForm = !showForm }, modifier = Modifier.size(28.dp)) {
                    Icon(if (showForm) Icons.Default.Close else Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                }
            }

            if (comakers.isEmpty() && !showForm) {
                Text("No comakers yet. Tap + to add an alternative contact (e.g. relative, guarantor).",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            comakers.forEach { comaker ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(comaker.fullName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(comaker.mobileNumber, fontSize = 12.sp)
                            Text(comaker.address, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDelete(comaker) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (showForm) {
                OutlinedTextField(name, { name = it }, label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(mobile, { mobile = it }, label = { Text("Mobile Number *") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Address *") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 3)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showForm = false; name = ""; mobile = ""; address = "" },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && mobile.isNotBlank() && address.isNotBlank()) {
                                onAdd(name.trim(), mobile.trim(), address.trim())
                                name = ""; mobile = ""; address = ""
                                showForm = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Add Comaker") }
                }
            }
        }
    }
}

@Composable
private fun RemoteLinkSection(
    state: ContractUiState,
    onGenerate: () -> Unit,
    onRegenerate: () -> Unit,
    onAddIdPhoto: () -> Unit
) {
    val context = LocalContext.current
    val contract = state.contract ?: return
    val status = contract.remoteBorrowerStatus   // "none" | "pending" | "completed"
    val link = state.shareableLink

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Link, null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("Remote Signing", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }

            when (status) {
                "none" -> {
                    Text("Borrower is far away? Generate a secure link they can open in their browser to sign remotely.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.linkError != null) {
                        Text(state.linkError, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = onGenerate,
                        enabled = !state.isGeneratingLink,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isGeneratingLink) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Generate Secure Link")
                    }
                }

                "pending" -> {
                    // Status dot + expiry
                    val expiryText = contract.secureLinkExpiresAt?.let { exp ->
                        val remaining = exp - System.currentTimeMillis()
                        if (remaining <= 0) "Expired" else {
                            val h = remaining / 3_600_000
                            val d = h / 24
                            if (d > 0) "Expires in ${d}d ${h % 24}h" else "Expires in ${h}h"
                        }
                    } ?: ""
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = Color(0xFFF57F17), shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(8.dp)) {}
                        Text("Pending — waiting for borrower", fontSize = 12.sp,
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.weight(1f))
                        Text(expiryText, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (link != null) {
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Contract Link", link))
                                    Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy Link")
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, link)
                                        putExtra(Intent.EXTRA_SUBJECT, "Please sign this debt contract")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share link via")
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share Link")
                            }
                        }
                    }
                }

                "completed" -> {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(color = Color(0xFF2E7D32), shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(8.dp)) {}
                        Text("Borrower has signed remotely", fontSize = 12.sp,
                            fontWeight = FontWeight.Medium, color = Color(0xFF2E7D32))
                    }
                    if (contract.remoteBorrowerFullName != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Name: ${contract.remoteBorrowerFullName}", fontSize = 12.sp)
                            contract.remoteBorrowerAddress?.let {
                                Text("Address: $it", fontSize = 12.sp)
                            }
                            contract.remoteBorrowerIdType?.let { idType ->
                                val idNum = contract.remoteBorrowerIdNumber ?: ""
                                Text("ID: $idType — $idNum", fontSize = 12.sp)
                            }

                            // ID photo thumbnail + add/replace button
                            val idPhotoFile = contract.remoteBorrowerIdImagePath?.let { java.io.File(it) }
                            var showFullscreen by remember { mutableStateOf(false) }

                            if (idPhotoFile != null && idPhotoFile.exists()) {
                                AsyncImage(
                                    model = idPhotoFile,
                                    contentDescription = "Borrower ID photo — tap to view",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .background(Color(0xFFF5F5F5), MaterialTheme.shapes.small)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                                        .padding(4.dp)
                                        .clickable { showFullscreen = true }
                                )
                                Text(
                                    "Tap photo to view full size",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (showFullscreen) {
                                    var scale by remember { mutableFloatStateOf(1f) }
                                    var offset by remember { mutableStateOf(Offset.Zero) }
                                    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                                        scale = (scale * zoomChange).coerceIn(1f, 6f)
                                        offset += panChange
                                    }
                                    androidx.compose.ui.window.Dialog(
                                        onDismissRequest = { showFullscreen = false },
                                        properties = androidx.compose.ui.window.DialogProperties(
                                            usePlatformDefaultWidth = false
                                        )
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxSize()
                                                .background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AsyncImage(
                                                model = idPhotoFile,
                                                contentDescription = "Borrower ID full view",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer {
                                                        scaleX = scale
                                                        scaleY = scale
                                                        translationX = offset.x
                                                        translationY = offset.y
                                                    }
                                                    .transformable(transformState)
                                            )
                                            IconButton(
                                                onClick = { showFullscreen = false },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp)
                                            ) {
                                                Icon(Icons.Default.Close, "Close",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(28.dp))
                                            }
                                            Text(
                                                "Pinch to zoom  •  tap × to close",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp,
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = onAddIdPhoto,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (idPhotoFile != null && idPhotoFile.exists()) "Replace ID Photo"
                                    else "Add ID Photo",
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    // Allow re-sending
                    OutlinedButton(
                        onClick = onRegenerate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Link, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Generate New Link")
                    }
                }
            }
        }
    }
}

@Composable
private fun DocBullet(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
    }
}

private fun contractAmountToWords(amount: Double): String {
    val whole = amount.toLong()
    val centavos = ((amount - whole) * 100).toLong()
    return if (centavos > 0)
        "${contractNumberToWords(whole)} Pesos and ${contractNumberToWords(centavos)} Centavos"
    else
        "${contractNumberToWords(whole)} Pesos"
}

private fun contractNumberToWords(n: Long): String {
    if (n == 0L) return "Zero"
    val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen")
    val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty",
        "Sixty", "Seventy", "Eighty", "Ninety")
    fun below1000(x: Long): String = when {
        x < 20  -> ones[x.toInt()]
        x < 100 -> tens[(x / 10).toInt()] + if (x % 10 > 0) " ${ones[(x % 10).toInt()]}" else ""
        else    -> "${ones[(x / 100).toInt()]} Hundred" +
                   if (x % 100 > 0) " ${below1000(x % 100)}" else ""
    }
    return when {
        n >= 1_000_000_000 -> "${below1000(n / 1_000_000_000)} Billion" +
                              if (n % 1_000_000_000 > 0) " ${contractNumberToWords(n % 1_000_000_000)}" else ""
        n >= 1_000_000     -> "${below1000(n / 1_000_000)} Million" +
                              if (n % 1_000_000 > 0) " ${contractNumberToWords(n % 1_000_000)}" else ""
        n >= 1_000         -> "${below1000(n / 1_000)} Thousand" +
                              if (n % 1_000 > 0) " ${contractNumberToWords(n % 1_000)}" else ""
        else               -> below1000(n)
    }
}

@Composable
private fun SignatureStatus(role: String, path: String?) {
    val signed = path != null
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (signed) "✓" else "○",
                color = if (signed) Color(0xFF2E7D32) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Text("$role signature${if (signed) " — signed" else " — pending"}", fontSize = 12.sp)
        }
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = "$role signature",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(72.dp)
                    .background(Color.White, MaterialTheme.shapes.small)
                    .padding(4.dp)
            )
        }
    }
}
