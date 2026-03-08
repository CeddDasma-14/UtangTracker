package com.cedd.utangtracker.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PaymentEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import javax.inject.Inject

sealed class BackupStatus {
    object Idle : BackupStatus()
    object Working : BackupStatus()
    data class Success(val message: String) : BackupStatus()
    data class Error(val message: String) : BackupStatus()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
    private val repo: UtangRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = prefs.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isBiometricEnabled: StateFlow<Boolean> = prefs.isBiometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lenderName: StateFlow<String> = prefs.lenderName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val isPremium: StateFlow<Boolean> = prefs.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setLenderName(name: String) = viewModelScope.launch { prefs.setLenderName(name) }
    fun setPremium(enabled: Boolean) = viewModelScope.launch { prefs.setPremium(enabled) }

    private val _backupStatus = MutableStateFlow<BackupStatus>(BackupStatus.Idle)
    val backupStatus: StateFlow<BackupStatus> = _backupStatus.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) = viewModelScope.launch { prefs.setDarkMode(enabled) }
    fun toggleBiometric(enabled: Boolean) = viewModelScope.launch { prefs.setBiometricEnabled(enabled) }

    fun clearBackupStatus() { _backupStatus.value = BackupStatus.Idle }

    fun exportCsv() = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Working
        try {
            val persons  = repo.getAllPersons().first()
            val debts    = repo.getAllDebts().first()
            val dateFmt  = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val sb = StringBuilder()
            sb.appendLine("Type,Person,Purpose,Total Amount,Paid,Remaining,Status,Due Date,Interest Rate (%),Auto Interest")
            debts.forEach { d ->
                val name     = persons.find { it.id == d.personId }?.name?.csvEscape() ?: ""
                val due      = d.dateDue?.let { dateFmt.format(Date(it)) } ?: ""
                val remaining = (d.amount - d.paidAmount).coerceAtLeast(0.0)
                sb.appendLine(
                    "${d.type},${name},${d.purpose.csvEscape()}," +
                    "%.2f,%.2f,%.2f,".format(d.amount, d.paidAmount, remaining) +
                    "${d.status},$due,${d.interestRate},${d.autoApplyInterest}"
                )
            }

            val file = writeReportFile(sb.toString(), "utang_report", "csv")
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LoanTrack Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Open Report With…")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            _backupStatus.value = BackupStatus.Success("Report exported — ${debts.size} debts.")
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error("Export failed: ${e.message}")
        }
    }

    private fun String.csvEscape(): String {
        return if (contains(',') || contains('"') || contains('\n')) "\"${replace("\"", "\"\"")}\"" else this
    }

    fun exportBackup() = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Working
        try {
            val persons  = repo.getAllPersons().first()
            val debts    = repo.getAllDebts().first()
            val payments = repo.getAllPayments().first()

            val json = buildJson(persons, debts, payments)
            val file = writeBackupFile(json)
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LoanTrack Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Save Backup To…")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            _backupStatus.value = BackupStatus.Success("Backup ready — choose where to save.")
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error("Export failed: ${e.message}")
        }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Working
        try {
            val raw = context.contentResolver.openInputStream(uri)
                ?.use { it.bufferedReader().readText() }
                ?: error("Cannot read file")

            val root        = JSONObject(raw)
            val personsArr  = root.getJSONArray("persons")
            val debtsArr    = root.getJSONArray("debts")
            val paymentsArr = root.getJSONArray("payments")

            // Insert in FK order: persons → debts → payments
            for (i in 0 until personsArr.length()) {
                val o = personsArr.getJSONObject(i)
                repo.savePerson(
                    PersonEntity(
                        id        = o.getLong("id"),
                        name      = o.getString("name"),
                        photoPath = o.optString("photoPath").takeIf { it.isNotEmpty() && it != "null" },
                        phone     = o.optString("phone").takeIf { it.isNotEmpty() && it != "null" },
                        createdAt = o.getLong("createdAt")
                    )
                )
            }

            for (i in 0 until debtsArr.length()) {
                val o = debtsArr.getJSONObject(i)
                repo.saveDebt(
                    DebtEntity(
                        id                      = o.getLong("id"),
                        personId                = o.getLong("personId"),
                        type                    = o.getString("type"),
                        amount                  = o.getDouble("amount"),
                        paidAmount              = o.getDouble("paidAmount"),
                        purpose                 = o.getString("purpose"),
                        dateCreated             = o.getLong("dateCreated"),
                        dateDue                 = if (o.isNull("dateDue")) null else o.getLong("dateDue"),
                        interestRate            = o.getDouble("interestRate"),
                        autoApplyInterest       = o.optBoolean("autoApplyInterest", false),
                        contractEnabled         = o.optBoolean("contractEnabled", false),
                        status                  = o.getString("status"),
                        notes                   = o.optString("notes"),
                        disbursementReceiptPaths = o.optString("disbursementReceiptPaths")
                            .takeIf { it.isNotEmpty() && it != "null" },
                        bankCharge              = o.optDouble("bankCharge", 0.0),
                        totalAmount             = o.optDouble("totalAmount", 0.0)
                    )
                )
            }

            for (i in 0 until paymentsArr.length()) {
                val o = paymentsArr.getJSONObject(i)
                repo.insertPaymentDirect(
                    PaymentEntity(
                        id       = o.getLong("id"),
                        debtId   = o.getLong("debtId"),
                        amount   = o.getDouble("amount"),
                        datePaid = o.getLong("datePaid"),
                        notes    = o.optString("notes")
                    )
                )
            }

            _backupStatus.value = BackupStatus.Success(
                "Restored ${personsArr.length()} persons, ${debtsArr.length()} debts, ${paymentsArr.length()} payments."
            )
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error("Import failed: ${e.message}")
        }
    }

    // ── Spreadsheet CSV Import ───────────────────────────────────────────────

    fun importFromCsv(uri: Uri) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Working
        try {
            val bytes = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes() }
                ?: error("Cannot read file")

            // Detect .xlsx (ZIP magic bytes "PK") — parse it directly
            if (bytes.size >= 2 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
                val csvText = withContext(Dispatchers.IO) { parseXlsxToCsv(bytes) }
                importFromCsvText(csvText)
                return@launch
            }
            // Detect legacy .xls (OLE2 magic bytes)
            if (bytes.size >= 2 && bytes[0] == 0xD0.toByte() && bytes[1] == 0xCF.toByte()) {
                error("Legacy .xls format is not supported. Please open in Google Sheets or Excel and save as .xlsx or CSV.")
            }

            val raw = bytes.toString(Charsets.UTF_8)
            val lines = raw.lines().filter { it.isNotBlank() }
            if (lines.isEmpty()) error("File is empty")

            fun parseCsvLine(line: String): List<String> {
                val result = mutableListOf<String>()
                var inQuotes = false
                val current = StringBuilder()
                for (ch in line) {
                    when {
                        ch == '"'  -> inQuotes = !inQuotes
                        ch == ',' && !inQuotes -> { result.add(current.toString().trim()); current.clear() }
                        else -> current.append(ch)
                    }
                }
                result.add(current.toString().trim())
                return result
            }

            fun String.toMoney(): Double =
                replace(",", "").replace("₱", "").replace("%", "").trim().toDoubleOrNull() ?: 0.0

            val dateFmtSlash = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
            val dateFmtDash  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            fun parseDate(s: String): Long? = s.trim().takeIf { it.isNotBlank() }?.let {
                runCatching { (if (it.contains('-')) dateFmtDash else dateFmtSlash).parse(it)?.time }.getOrNull()
            }

            val headerLineIdx = lines.indexOfFirst { line ->
                val cols = parseCsvLine(line).map { it.trim().uppercase() }
                cols.any { it.contains("NAME") } && cols.any { it.contains("AMOUNT") }
            }
            if (headerLineIdx < 0) error("Could not find header row. Make sure the sheet has NAME and AMOUNT columns.")
            val headers = parseCsvLine(lines[headerLineIdx]).map { it.trim().uppercase() }
            val dataLines = lines.drop(headerLineIdx + 1)

            fun col(vararg names: String) = names.firstNotNullOfOrNull { n ->
                headers.indexOfFirst { it.contains(n) }.takeIf { it >= 0 }
            }

            val colDate      = col("DATE")
            val colName      = col("NAME")
            val colAmount    = col("AMOUNT")
            val colDueDate   = col("DUE DATE", "DUEDATE")
            val colRate      = col("%", "INTEREST RATE", "RATE")
            val colBankChg   = col("BANK CHARGE", "BANKCHARGE", "BANK")
            val colMonths    = col("MONTH")
            val colCollected = col("COLLECTED")
            val colBalance   = col("BALANCE")

            if (colName == null || colAmount == null) error("Missing required columns: NAME and AMOUNT")

            val existingPersons = repo.getAllPersons().first().toMutableList()
            suspend fun getOrCreatePerson(name: String): Long {
                val trimmed = name.trim()
                existingPersons.find { it.name.equals(trimmed, ignoreCase = true) }?.let { return it.id }
                val newId = repo.savePerson(PersonEntity(name = trimmed))
                existingPersons.add(PersonEntity(id = newId, name = trimmed))
                return newId
            }

            val existingBefore = existingPersons.size
            var debtCount = 0

            for (line in dataLines) {
                val cols = parseCsvLine(line)
                fun cell(idx: Int?) = if (idx != null && idx < cols.size) cols[idx] else ""

                val name = cell(colName).trim()
                if (name.isBlank()) continue

                val amount = cell(colAmount).toMoney()
                if (amount <= 0) continue

                val dateCreated  = parseDate(cell(colDate)) ?: System.currentTimeMillis()
                val dateDue      = parseDate(cell(colDueDate))
                val interestRate = cell(colRate).toMoney()
                val bankCharge   = cell(colBankChg).toMoney()
                val months       = cell(colMonths).trim().toIntOrNull() ?: 0

                val effectiveMonths = if (months > 0) months else if (dateDue != null) {
                    val c1 = java.util.Calendar.getInstance().apply { timeInMillis = dateCreated }
                    val c2 = java.util.Calendar.getInstance().apply { timeInMillis = dateDue }
                    maxOf(
                        (c2.get(java.util.Calendar.YEAR) - c1.get(java.util.Calendar.YEAR)) * 12 +
                        (c2.get(java.util.Calendar.MONTH) - c1.get(java.util.Calendar.MONTH)), 0
                    )
                } else 0
                val totalInterest = if (interestRate > 0 && effectiveMonths > 0)
                    amount * (interestRate / 100.0) * effectiveMonths else 0.0
                val totalAmount = amount + totalInterest + bankCharge
                val paidAmount = if (colBalance != null) {
                    val balance = cell(colBalance).toMoney()
                    (totalAmount - balance).coerceIn(0.0, totalAmount)
                } else {
                    cell(colCollected).toMoney().coerceIn(0.0, totalAmount)
                }
                val status = when {
                    paidAmount >= totalAmount && totalAmount > 0              -> "SETTLED"
                    dateDue != null && dateDue < System.currentTimeMillis()  -> "OVERDUE"
                    else                                                      -> "ACTIVE"
                }

                val personId = getOrCreatePerson(name)
                val debtId = repo.saveDebt(
                    DebtEntity(
                        personId     = personId,
                        type         = "OWED_TO_ME",
                        amount       = amount,
                        paidAmount   = paidAmount,
                        purpose      = "Loan",
                        dateCreated  = dateCreated,
                        dateDue      = dateDue,
                        interestRate = interestRate,
                        status       = status,
                        bankCharge   = bankCharge,
                        totalAmount  = totalAmount
                    )
                )
                if (paidAmount > 0) {
                    repo.insertPaymentDirect(
                        PaymentEntity(debtId = debtId, amount = paidAmount, datePaid = dateCreated)
                    )
                }
                debtCount++
            }

            val personCount = existingPersons.size - existingBefore
            _backupStatus.value = BackupStatus.Success(
                "Imported $debtCount debts, $personCount new persons from CSV."
            )
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error("CSV import failed: ${e.message}")
        }
    }

    // ── Google Sheets URL Import ─────────────────────────────────────────────

    fun importFromGoogleSheetsUrl(sheetUrl: String) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Working
        try {
            val csvUrl = buildCsvExportUrl(sheetUrl.trim())
                ?: error("Not a valid Google Sheets link. Make sure you copied the full URL.")

            val csvText = withContext(Dispatchers.IO) {
                var conn = java.net.URL(csvUrl).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 10_000
                conn.readTimeout = 20_000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0")

                var redirects = 0
                while (conn.responseCode in 301..303 && redirects < 5) {
                    val location = conn.getHeaderField("Location") ?: break
                    conn.disconnect()
                    conn = java.net.URL(location).openConnection() as java.net.HttpURLConnection
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 20_000
                    conn.instanceFollowRedirects = true
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                    redirects++
                }

                val code = conn.responseCode
                if (code !in 200..299) {
                    conn.disconnect()
                    error("Server returned HTTP $code. Make sure the sheet is shared as \"Anyone with the link can view\".")
                }
                conn.inputStream.bufferedReader().readText().also { conn.disconnect() }
            }

            importFromCsvText(csvText)
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error(
                if (e.message?.contains("Not a valid") == true) e.message!!
                else "Download failed: ${e.message ?: "Check that the sheet is set to Anyone with the link can view."}"
            )
        }
    }

    private fun buildCsvExportUrl(url: String): String? {
        val idRegex = Regex("""spreadsheets/d/([A-Za-z0-9_-]+)""")
        val gidRegex = Regex("""[#?&]gid=(\d+)""")
        val id = idRegex.find(url)?.groupValues?.get(1) ?: return null
        val gid = gidRegex.find(url)?.groupValues?.get(1)
        return buildString {
            append("https://docs.google.com/spreadsheets/d/$id/export?format=csv")
            if (gid != null) append("&gid=$gid")
        }
    }

    private fun parseXlsxToCsv(bytes: ByteArray): String {
        val zip = java.util.zip.ZipInputStream(bytes.inputStream())
        var sharedStringsXml: String? = null
        var sheetXml: String? = null
        var entry = zip.nextEntry
        while (entry != null) {
            when (entry.name) {
                "xl/sharedStrings.xml"      -> sharedStringsXml = zip.bufferedReader().readText()
                "xl/worksheets/sheet1.xml"  -> sheetXml = zip.bufferedReader().readText()
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
        zip.close()
        if (sheetXml == null) error("Could not find worksheet data in the Excel file.")

        val sharedStrings = mutableListOf<String>()
        sharedStringsXml?.let { xml ->
            Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { si ->
                val text = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(si.value).joinToString("") { it.groupValues[1] }
                sharedStrings.add(text.xmlUnescape())
            }
        }

        fun colToIdx(col: String): Int {
            var n = 0; col.forEach { n = n * 26 + (it - 'A' + 1) }; return n - 1
        }

        val excelEpoch = java.util.Calendar.getInstance().apply {
            set(1899, java.util.Calendar.DECEMBER, 30, 0, 0, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        fun maybeDate(raw: String, styleIdx: Int, numFmts: Map<Int, String>, cellXfs: List<Int>): String {
            val xfNumFmtId = cellXfs.getOrElse(styleIdx) { -1 }
            val fmt = numFmts[xfNumFmtId] ?: ""
            val isDate = xfNumFmtId in 14..17 || xfNumFmtId in 164..200 ||
                fmt.contains("yy", ignoreCase = true) || fmt.contains("d/m", ignoreCase = true)
            if (!isDate) return raw
            val days = raw.toDoubleOrNull() ?: return raw
            val ms = excelEpoch + (days * 86_400_000L).toLong()
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ms }
            return "${cal.get(java.util.Calendar.MONTH)+1}/${cal.get(java.util.Calendar.DAY_OF_MONTH)}/${cal.get(java.util.Calendar.YEAR)}"
        }

        val numFmts = mutableMapOf<Int, String>()
        val cellXfs  = mutableListOf<Int>()
        val zipB = java.util.zip.ZipInputStream(bytes.inputStream())
        var e2 = zipB.nextEntry
        while (e2 != null) {
            if (e2.name == "xl/styles.xml") {
                val sXml = zipB.bufferedReader().readText()
                Regex("<numFmt numFmtId=\"(\\d+)\" formatCode=\"([^\"]*)\"/>").findAll(sXml)
                    .forEach { numFmts[it.groupValues[1].toInt()] = it.groupValues[2] }
                Regex("<xf[^>]*numFmtId=\"(\\d+)\"").findAll(sXml)
                    .forEach { cellXfs.add(it.groupValues[1].toInt()) }
            }
            zipB.closeEntry(); e2 = zipB.nextEntry
        }
        zipB.close()

        val rows = mutableListOf<Map<Int, String>>()
        Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL).findAll(sheetXml).forEach { rowMatch ->
            val rowData = mutableMapOf<Int, String>()
            Regex("<c r=\"([A-Z]+)\\d+\"([^>]*)>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)
                .findAll(rowMatch.groupValues[1]).forEach { cellMatch ->
                    val colIdx   = colToIdx(cellMatch.groupValues[1])
                    val attrs    = cellMatch.groupValues[2]
                    val content  = cellMatch.groupValues[3]
                    val rawVal   = Regex("<v>(.*?)</v>").find(content)?.groupValues?.get(1) ?: ""
                    val styleIdx = Regex("s=\"(\\d+)\"").find(attrs)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                    val cellVal = when {
                        attrs.contains("t=\"s\"")  -> sharedStrings.getOrElse(rawVal.toIntOrNull() ?: -1) { rawVal }
                        attrs.contains("t=\"str\"") || attrs.contains("t=\"inlineStr\"") ->
                            Regex("<t[^>]*>(.*?)</t>").find(content)?.groupValues?.get(1)?.xmlUnescape() ?: rawVal
                        else -> maybeDate(rawVal, styleIdx, numFmts, cellXfs)
                    }
                    if (cellVal.isNotBlank()) rowData[colIdx] = cellVal
                }
            if (rowData.isNotEmpty()) rows.add(rowData)
        }
        if (rows.isEmpty()) error("Spreadsheet appears to be empty.")
        val maxCol = rows.maxOf { it.keys.maxOrNull() ?: 0 }
        return rows.joinToString("\n") { row ->
            (0..maxCol).joinToString(",") { col ->
                val v = row[col] ?: ""
                if (v.contains(',') || v.contains('"') || v.contains('\n'))
                    "\"${v.replace("\"", "\"\"")}\"" else v
            }
        }
    }

    private fun String.xmlUnescape() = replace("&amp;", "&").replace("&lt;", "<")
        .replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")

    private suspend fun importFromCsvText(raw: String) {
        val lines = raw.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) error("Sheet is empty")

        fun parseCsvLine(line: String): List<String> {
            val result = mutableListOf<String>()
            var inQuotes = false
            val current = StringBuilder()
            for (ch in line) {
                when {
                    ch == '"'  -> inQuotes = !inQuotes
                    ch == ',' && !inQuotes -> { result.add(current.toString().trim()); current.clear() }
                    else -> current.append(ch)
                }
            }
            result.add(current.toString().trim())
            return result
        }

        fun String.toMoney(): Double =
            replace(",", "").replace("₱", "").replace("%", "").trim().toDoubleOrNull() ?: 0.0

        val dateFmtSlash = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
        val dateFmtDash  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        fun parseDate(s: String): Long? = s.trim().takeIf { it.isNotBlank() }?.let {
            runCatching { (if (it.contains('-')) dateFmtDash else dateFmtSlash).parse(it)?.time }.getOrNull()
        }

        val headerLineIdx = lines.indexOfFirst { line ->
            val cols = parseCsvLine(line).map { it.trim().uppercase() }
            cols.any { it.contains("NAME") } && cols.any { it.contains("AMOUNT") }
        }
        if (headerLineIdx < 0) error("Could not find header row. Make sure the sheet has NAME and AMOUNT columns.")
        val headers = parseCsvLine(lines[headerLineIdx]).map { it.trim().uppercase() }
        val dataLines = lines.drop(headerLineIdx + 1)

        fun col(vararg names: String) = names.firstNotNullOfOrNull { n ->
            headers.indexOfFirst { it.contains(n) }.takeIf { it >= 0 }
        }

        val colDate      = col("DATE")
        val colName      = col("NAME")
        val colAmount    = col("AMOUNT")
        val colDueDate   = col("DUE DATE", "DUEDATE")
        val colRate      = col("%", "INTEREST RATE", "RATE")
        val colBankChg   = col("BANK CHARGE", "BANKCHARGE", "BANK")
        val colMonths    = col("MONTH")
        val colCollected = col("COLLECTED")
        val colBalance   = col("BALANCE")

        if (colName == null || colAmount == null) error("Missing required columns: NAME and AMOUNT")

        val existingPersons = repo.getAllPersons().first().toMutableList()
        suspend fun getOrCreatePerson(name: String): Long {
            val trimmed = name.trim()
            existingPersons.find { it.name.equals(trimmed, ignoreCase = true) }?.let { return it.id }
            val newId = repo.savePerson(PersonEntity(name = trimmed))
            existingPersons.add(PersonEntity(id = newId, name = trimmed))
            return newId
        }

        val existingBefore = existingPersons.size
        var debtCount = 0

        for (line in dataLines) {
            val cols = parseCsvLine(line)
            fun cell(idx: Int?) = if (idx != null && idx < cols.size) cols[idx] else ""

            val name = cell(colName).trim()
            if (name.isBlank()) continue

            val amount = cell(colAmount).toMoney()
            if (amount <= 0) continue

            val dateCreated  = parseDate(cell(colDate)) ?: System.currentTimeMillis()
            val dateDue      = parseDate(cell(colDueDate))
            val interestRate = cell(colRate).toMoney()
            val bankCharge   = cell(colBankChg).toMoney()
            val months       = cell(colMonths).trim().toIntOrNull() ?: 0

            val effectiveMonths = if (months > 0) months else if (dateDue != null) {
                val c1 = java.util.Calendar.getInstance().apply { timeInMillis = dateCreated }
                val c2 = java.util.Calendar.getInstance().apply { timeInMillis = dateDue }
                maxOf(
                    (c2.get(java.util.Calendar.YEAR) - c1.get(java.util.Calendar.YEAR)) * 12 +
                    (c2.get(java.util.Calendar.MONTH) - c1.get(java.util.Calendar.MONTH)), 0
                )
            } else 0
            val totalInterest = if (interestRate > 0 && effectiveMonths > 0)
                amount * (interestRate / 100.0) * effectiveMonths else 0.0
            val totalAmount = amount + totalInterest + bankCharge
            val paidAmount = if (colBalance != null) {
                val balance = cell(colBalance).toMoney()
                (totalAmount - balance).coerceIn(0.0, totalAmount)
            } else {
                cell(colCollected).toMoney().coerceIn(0.0, totalAmount)
            }
            val status = when {
                paidAmount >= totalAmount && totalAmount > 0             -> "SETTLED"
                dateDue != null && dateDue < System.currentTimeMillis() -> "OVERDUE"
                else                                                     -> "ACTIVE"
            }

            val personId = getOrCreatePerson(name)
            val debtId = repo.saveDebt(
                DebtEntity(
                    personId     = personId,
                    type         = "OWED_TO_ME",
                    amount       = amount,
                    paidAmount   = paidAmount,
                    purpose      = "Loan",
                    dateCreated  = dateCreated,
                    dateDue      = dateDue,
                    interestRate = interestRate,
                    status       = status,
                    bankCharge   = bankCharge,
                    totalAmount  = totalAmount
                )
            )
            if (paidAmount > 0) {
                repo.insertPaymentDirect(
                    PaymentEntity(debtId = debtId, amount = paidAmount, datePaid = dateCreated)
                )
            }
            debtCount++
        }

        val personCount = existingPersons.size - existingBefore
        _backupStatus.value = BackupStatus.Success(
            "Imported $debtCount debts, $personCount new persons."
        )
    }

    // ── Spreadsheet JSON Import ───────────────────────────────────────────────

    fun importSpreadsheet(uri: Uri) = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Working
        try {
            val raw = context.contentResolver.openInputStream(uri)
                ?.use { it.bufferedReader().readText() }
                ?: error("Cannot read file")

            val root    = JSONObject(raw)
            val records = root.getJSONArray("records")
            val dateFmt = SimpleDateFormat("M/d/yyyy", Locale.getDefault())
            val isoFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            fun parseDate(s: String): Long? = runCatching {
                (if (s.contains('-')) isoFmt else dateFmt).parse(s)?.time
            }.getOrNull()

            val existingPersons = repo.getAllPersons().first().toMutableList()

            suspend fun getOrCreatePerson(name: String): Long {
                val trimmed = name.trim()
                existingPersons.find { it.name.equals(trimmed, ignoreCase = true) }
                    ?.let { return it.id }
                val newId = repo.savePerson(PersonEntity(name = trimmed))
                existingPersons.add(PersonEntity(id = newId, name = trimmed))
                return newId
            }

            var debtCount = 0
            val existingBefore = existingPersons.size

            for (i in 0 until records.length()) {
                val r = records.getJSONObject(i)

                val personName   = r.getString("name")
                val amount       = r.getDouble("amount")
                val interestRate = r.optDouble("interestRate", 0.0)
                val bankCharge   = r.optDouble("bankCharge", 0.0)
                val collected    = r.optDouble("collected", 0.0)
                val purpose      = r.optString("purpose", "Loan")
                val dateCreated  = r.optString("date", "").let { parseDate(it) } ?: System.currentTimeMillis()
                val dateDue      = r.optString("dueDate", "").takeIf { it.isNotBlank() }?.let { parseDate(it) }
                val months       = r.optInt("months", 0)

                val effectiveMonths = if (months > 0) months else if (dateDue != null) {
                    val c1 = java.util.Calendar.getInstance().apply { timeInMillis = dateCreated }
                    val c2 = java.util.Calendar.getInstance().apply { timeInMillis = dateDue }
                    maxOf(
                        (c2.get(java.util.Calendar.YEAR) - c1.get(java.util.Calendar.YEAR)) * 12 +
                        (c2.get(java.util.Calendar.MONTH) - c1.get(java.util.Calendar.MONTH)), 0
                    )
                } else 0
                val totalInterest = if (interestRate > 0 && effectiveMonths > 0)
                    amount * (interestRate / 100.0) * effectiveMonths else 0.0
                val totalAmount   = amount + totalInterest + bankCharge

                val paidAmount = collected.coerceIn(0.0, totalAmount)
                val status = when {
                    paidAmount >= totalAmount                               -> "SETTLED"
                    dateDue != null && dateDue < System.currentTimeMillis() -> "OVERDUE"
                    else                                                    -> "ACTIVE"
                }

                val personId = getOrCreatePerson(personName)
                val debtId = repo.saveDebt(
                    DebtEntity(
                        personId     = personId,
                        type         = "OWED_TO_ME",
                        amount       = amount,
                        paidAmount   = paidAmount,
                        purpose      = purpose,
                        dateCreated  = dateCreated,
                        dateDue      = dateDue,
                        interestRate = interestRate,
                        status       = status,
                        bankCharge   = bankCharge,
                        totalAmount  = totalAmount
                    )
                )

                if (paidAmount > 0) {
                    repo.insertPaymentDirect(
                        PaymentEntity(debtId = debtId, amount = paidAmount, datePaid = dateCreated)
                    )
                }
                debtCount++
            }

            val personCount = existingPersons.size - existingBefore
            _backupStatus.value = BackupStatus.Success(
                "Imported $debtCount debts, $personCount new persons."
            )
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error("Spreadsheet import failed: ${e.message}")
        }
    }

    fun exportSpreadsheetTemplate() = viewModelScope.launch {
        _backupStatus.value = BackupStatus.Working
        try {
            val template = JSONObject().apply {
                put("_instructions", "Fill in the records array. Required: name, amount. Optional: date (M/d/yyyy), dueDate (M/d/yyyy), interestRate (%), bankCharge, months, collected, purpose.")
                put("records", JSONArray().apply {
                    put(JSONObject().apply {
                        put("date", "12/26/2025")
                        put("name", "Keiffer")
                        put("amount", 7000.0)
                        put("dueDate", "3/26/2026")
                        put("interestRate", 10.0)
                        put("bankCharge", 0.0)
                        put("months", 3)
                        put("collected", 1500.0)
                        put("purpose", "Loan")
                    })
                    put(JSONObject().apply {
                        put("date", "1/17/2026")
                        put("name", "Jessica Quijano")
                        put("amount", 30000.0)
                        put("dueDate", "7/17/2026")
                        put("interestRate", 10.0)
                        put("bankCharge", 10.0)
                        put("months", 6)
                        put("collected", 12000.0)
                        put("purpose", "Loan")
                    })
                })
            }.toString(2)

            val file = writeReportFile(template, "spreadsheet_template", "json")
            val uri  = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LoanTrack Spreadsheet Template")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, "Save Template To…")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            _backupStatus.value = BackupStatus.Success("Template exported — fill it in and import.")
        } catch (e: Exception) {
            _backupStatus.value = BackupStatus.Error("Template export failed: ${e.message}")
        }
    }

    // ── JSON helpers ─────────────────────────────────────────────────────────

    private fun buildJson(
        persons: List<PersonEntity>,
        debts: List<DebtEntity>,
        payments: List<PaymentEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("persons", JSONArray().also { arr ->
            persons.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("photoPath", p.photoPath ?: JSONObject.NULL)
                    put("phone", p.phone ?: JSONObject.NULL)
                    put("createdAt", p.createdAt)
                })
            }
        })

        root.put("debts", JSONArray().also { arr ->
            debts.forEach { d ->
                arr.put(JSONObject().apply {
                    put("id", d.id)
                    put("personId", d.personId)
                    put("type", d.type)
                    put("amount", d.amount)
                    put("paidAmount", d.paidAmount)
                    put("purpose", d.purpose)
                    put("dateCreated", d.dateCreated)
                    put("dateDue", d.dateDue ?: JSONObject.NULL)
                    put("interestRate", d.interestRate)
                    put("autoApplyInterest", d.autoApplyInterest)
                    put("contractEnabled", d.contractEnabled)
                    put("status", d.status)
                    put("notes", d.notes)
                    put("disbursementReceiptPaths", d.disbursementReceiptPaths ?: JSONObject.NULL)
                    put("bankCharge", d.bankCharge)
                    put("totalAmount", d.totalAmount)
                })
            }
        })

        root.put("payments", JSONArray().also { arr ->
            payments.forEach { p ->
                arr.put(JSONObject().apply {
                    put("id", p.id)
                    put("debtId", p.debtId)
                    put("amount", p.amount)
                    put("datePaid", p.datePaid)
                    put("notes", p.notes)
                })
            }
        })

        return root.toString(2)
    }

    private fun writeBackupFile(json: String): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "utang_backup_$timestamp.json").also { it.writeText(json) }
    }

    private fun writeReportFile(content: String, prefix: String, ext: String): File {
        val dir = File(context.filesDir, "backups").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(dir, "${prefix}_$timestamp.$ext").also { it.writeText(content) }
    }
}
