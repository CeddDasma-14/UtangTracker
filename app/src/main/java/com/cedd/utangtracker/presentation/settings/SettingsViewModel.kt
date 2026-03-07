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
import kotlinx.coroutines.launch
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
                            .takeIf { it.isNotEmpty() && it != "null" }
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
