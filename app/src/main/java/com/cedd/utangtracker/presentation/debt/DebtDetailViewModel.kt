package com.cedd.utangtracker.presentation.debt

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.LedgerEntryEntity
import com.cedd.utangtracker.data.local.entity.PaymentEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.local.relation.DebtWithPayments
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import java.io.File
import javax.inject.Inject

data class DebtDetailUiState(
    val data: DebtWithPayments? = null,
    val person: PersonEntity? = null
)

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    private val repo: UtangRepository,
    private val prefs: PreferencesRepository,
    @ApplicationContext private val context: Context,
    savedState: SavedStateHandle
) : ViewModel() {

    private val debtId = savedState.get<Long>("debtId") ?: -1L

    val lenderName: StateFlow<String> = prefs.lenderName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val isPremium: StateFlow<Boolean> = prefs.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPremium(enabled: Boolean) = viewModelScope.launch { prefs.setPremium(enabled) }

    val ledgerEntries: StateFlow<List<LedgerEntryEntity>> = repo.getLedgerEntries(debtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<DebtDetailUiState> = combine(
        repo.getDebtWithPayments(debtId),
        repo.getAllPersons()
    ) { dwp, persons ->
        DebtDetailUiState(data = dwp, person = persons.find { it.id == dwp?.debt?.personId })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtDetailUiState())

    fun addPayment(amount: Double, notes: String, datePaidMillis: Long = System.currentTimeMillis()) = viewModelScope.launch {
        val debt = uiState.value.data?.debt ?: return@launch
        repo.addPayment(PaymentEntity(debtId = debtId, amount = amount, datePaid = datePaidMillis, notes = notes), debt)
    }

    fun deletePayment(payment: PaymentEntity) = viewModelScope.launch {
        val debt = uiState.value.data?.debt ?: return@launch
        repo.deletePayment(payment, debt)
    }

    fun deleteDebt(onDone: () -> Unit) = viewModelScope.launch {
        uiState.value.data?.debt?.let { repo.deleteDebt(it) }
        onDone()
    }

    // ── Disbursement receipts ─────────────────────────────────────────────────

    fun addDisbursementReceipt(uri: Uri) = viewModelScope.launch {
        val debt = uiState.value.data?.debt ?: return@launch
        val receiptDir = File(context.filesDir, "receipts").also { it.mkdirs() }
        val destFile = File(receiptDir, "debt${debt.id}_receipt_${System.currentTimeMillis()}.jpg")
        try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            } ?: return@launch
            destFile.outputStream().use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
        } catch (_: Exception) { return@launch }

        val existing = debt.disbursementReceiptPaths
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        repo.updateDebt(debt.copy(
            disbursementReceiptPaths = (existing + destFile.absolutePath).joinToString(",")
        ))
    }

    fun removeDisbursementReceipt(path: String) = viewModelScope.launch {
        val debt = uiState.value.data?.debt ?: return@launch
        val remaining = debt.disbursementReceiptPaths
            ?.split(",")?.filter { it.isNotBlank() && it != path } ?: emptyList()
        repo.updateDebt(debt.copy(
            disbursementReceiptPaths = remaining.joinToString(",").ifBlank { null }
        ))
        try { File(path).delete() } catch (_: Exception) {}
    }

    fun toggleLock() = viewModelScope.launch {
        uiState.value.data?.debt?.let { repo.toggleDebtLock(it) }
    }

    /** Adds one month's interest to the debt principal. */
    fun applyMonthlyInterest() = viewModelScope.launch {
        val debt = uiState.value.data?.debt ?: return@launch
        if (debt.interestRate <= 0) return@launch
        val monthlyAmount = debt.amount * (debt.interestRate / 100.0)
        repo.updateDebt(debt.copy(amount = debt.amount + monthlyAmount))
    }
}
