package com.cedd.utangtracker.presentation.ledger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.LedgerEntryEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.repository.UtangRepository
import com.cedd.utangtracker.util.LedgerCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LoanLedgerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: UtangRepository
) : ViewModel() {

    private val debtId: Long = checkNotNull(savedStateHandle["debtId"])

    private val _debt = MutableStateFlow<DebtEntity?>(null)
    val debt: StateFlow<DebtEntity?> = _debt.asStateFlow()

    private val _person = MutableStateFlow<PersonEntity?>(null)
    val person: StateFlow<PersonEntity?> = _person.asStateFlow()

    val entries: StateFlow<List<LedgerEntryEntity>> = repo.getLedgerEntries(debtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    data class AddEntryState(
        val year: Int = Calendar.getInstance().get(Calendar.YEAR),
        val month: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
        val paymentInput: String = "",
        val paymentDateMillis: Long? = null,
        val carryOverOverride: String = "",   // manual carry-over for this entry
        val notes: String = "",
        val preview: LedgerCalculator.LedgerResult? = null
    )

    private val _addState = MutableStateFlow<AddEntryState?>(null)
    val addState: StateFlow<AddEntryState?> = _addState.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllDebts().collect { list ->
                val d = list.find { it.id == debtId }
                _debt.value = d
                if (d != null && _person.value == null) {
                    _person.value = repo.getPersonById(d.personId)
                }
            }
        }
    }

    // ── Computed helpers ──────────────────────────────────────────────────────
    fun startingBalance(debt: DebtEntity) =
        LedgerCalculator.startingBalance(debt.amount, debt.interestRate, debt.ledgerCycleMonths)

    fun monthlyBill(debt: DebtEntity) =
        LedgerCalculator.monthlyBill(debt.amount, debt.interestRate, debt.ledgerCycleMonths)

    fun fixedInterest(debt: DebtEntity) =
        LedgerCalculator.fixedInterest(debt.amount, debt.interestRate)

    companion object {
        fun calcMonthlyBill(principal: Double, interestRate: Double, cycleMonths: Int) =
            LedgerCalculator.monthlyBill(principal, interestRate, cycleMonths)
    }

    // ── Config toggles ────────────────────────────────────────────────────────
    fun setLedgerEnabled(enabled: Boolean) = viewModelScope.launch {
        _debt.value?.let { repo.updateDebt(it.copy(ledgerEnabled = enabled)) }
    }
    fun setCarryOver(amount: Double) = viewModelScope.launch {
        _debt.value?.let { repo.updateDebt(it.copy(ledgerCarryOver = amount)) }
    }
    fun setCarryOverMonthly(monthly: Boolean) = viewModelScope.launch {
        _debt.value?.let { repo.updateDebt(it.copy(ledgerCarryOverMonthly = monthly)) }
    }
    fun setCycleMonths(months: Int) = viewModelScope.launch {
        _debt.value?.let { repo.updateDebt(it.copy(ledgerCycleMonths = months.coerceAtLeast(1))) }
    }
    fun setInitialBalance(balance: Double) = viewModelScope.launch {
        _debt.value?.let { repo.updateDebt(it.copy(ledgerInitialBalance = balance.coerceAtLeast(0.0))) }
    }
    fun toggleSettings() { _showSettings.value = !_showSettings.value }
    fun dismissSettings() { _showSettings.value = false }

    // ── Add entry dialog ──────────────────────────────────────────────────────
    fun openAddEntry() {
        val debt = _debt.value ?: return
        val currentEntries = entries.value
        val (nextYear, nextMonth) = LedgerCalculator.nextMonth(currentEntries, debt.dateCreated)
        val state = AddEntryState(year = nextYear, month = nextMonth)
        _addState.value = state
        refreshPreview(state)
    }

    fun updateAddState(block: AddEntryState.() -> AddEntryState) {
        val updated = _addState.value?.block() ?: return
        _addState.value = updated
        refreshPreview(updated)
    }

    private fun refreshPreview(state: AddEntryState) {
        val debt = _debt.value ?: return
        val currentEntries = entries.value

        // First entry: use custom initial balance if set, else auto startingBalance
        val openingBalance = if (currentEntries.isEmpty())
            if (debt.ledgerInitialBalance > 0.0) debt.ledgerInitialBalance
            else LedgerCalculator.startingBalance(debt.amount, debt.interestRate, debt.ledgerCycleMonths)
        else
            currentEntries.last().closingBalance

        val payment = state.paymentInput.toDoubleOrNull() ?: 0.0
        val effectiveCarryOver = resolveCarryOver(state, debt)
        val bill = LedgerCalculator.monthlyBill(debt.amount, debt.interestRate, debt.ledgerCycleMonths)

        _addState.value = state.copy(
            preview = LedgerCalculator.compute(
                openingBalance = openingBalance,
                principal = debt.amount,
                interestRate = debt.interestRate,
                carryOver = effectiveCarryOver,
                monthlyBill = bill,
                paymentAmount = payment
            )
        )
    }

    private fun resolveCarryOver(state: AddEntryState, debt: DebtEntity): Double {
        val manual = state.carryOverOverride.toDoubleOrNull()
        return when {
            manual != null && manual > 0 -> manual
            debt.ledgerCarryOverMonthly && debt.ledgerCarryOver > 0 -> debt.ledgerCarryOver
            else -> 0.0
        }
    }

    fun dismissAddEntry() { _addState.value = null }

    fun saveEntry() = viewModelScope.launch {
        val state = _addState.value ?: return@launch
        val debt = _debt.value ?: return@launch
        val preview = state.preview ?: return@launch
        val currentEntries = entries.value

        val openingBalance = if (currentEntries.isEmpty())
            if (debt.ledgerInitialBalance > 0.0) debt.ledgerInitialBalance
            else LedgerCalculator.startingBalance(debt.amount, debt.interestRate, debt.ledgerCycleMonths)
        else
            currentEntries.last().closingBalance

        repo.insertLedgerEntry(
            LedgerEntryEntity(
                debtId = debtId,
                year = state.year,
                month = state.month,
                openingBalance = openingBalance,
                interestAdded = preview.interestAdded,
                carryOverAdded = preview.carryOverAdded,
                paymentAmount = state.paymentInput.toDoubleOrNull() ?: 0.0,
                closingBalance = preview.closingBalance,
                isMissedPayment = preview.isMissedPayment,
                paymentDate = state.paymentDateMillis,
                notes = state.notes
            )
        )
        // Sync ledgerCurrentBalance so dashboard/list/detail screens reflect ledger balance
        _debt.value?.let { repo.updateDebt(it.copy(ledgerCurrentBalance = preview.closingBalance)) }
        _addState.value = null
    }

    fun deleteEntry(entry: LedgerEntryEntity) = viewModelScope.launch {
        val balanceAfterDelete = entries.value.filter { it.id != entry.id }.lastOrNull()?.closingBalance ?: 0.0
        repo.deleteLedgerEntry(entry)
        _debt.value?.let { repo.updateDebt(it.copy(ledgerCurrentBalance = balanceAfterDelete)) }
    }

    fun clearAllEntries() = viewModelScope.launch {
        repo.clearLedger(debtId)
        _debt.value?.let { repo.updateDebt(it.copy(ledgerCurrentBalance = 0.0)) }
    }

    // ── Share / Export ────────────────────────────────────────────────────────
    fun buildShareText(): String {
        val debt = _debt.value ?: return ""
        val entries = entries.value
        val personName = _person.value?.name ?: "Unknown"
        val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val bill = LedgerCalculator.monthlyBill(debt.amount, debt.interestRate, debt.ledgerCycleMonths)
        val startBal = LedgerCalculator.startingBalance(debt.amount, debt.interestRate, debt.ledgerCycleMonths)
        val dateFmt = SimpleDateFormat("MMMM d, yyyy", Locale.US)
        val currBal = if (debt.ledgerCurrentBalance > 0) debt.ledgerCurrentBalance
                      else (debt.totalAmount.takeIf { it > 0 } ?: debt.amount) - debt.paidAmount

        return buildString {
            appendLine("LOAN LEDGER STATEMENT")
            appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━")
            appendLine("Borrower : $personName")
            appendLine("Purpose  : ${debt.purpose}")
            appendLine("Generated: ${dateFmt.format(Date())}")
            appendLine()
            appendLine("Principal   : ₱${"%.2f".format(debt.amount)}")
            appendLine("Rate        : ${debt.interestRate}% / month")
            appendLine("Term        : ${debt.ledgerCycleMonths} month${if (debt.ledgerCycleMonths != 1) "s" else ""}")
            appendLine("Total Loan  : ₱${"%.2f".format(startBal)}")
            appendLine("Monthly Bill: ₱${"%.2f".format(bill)}")
            appendLine()
            appendLine("MONTHLY RECORD")
            appendLine("──────────────────────────────────────────")
            if (entries.isEmpty()) {
                appendLine("  (no entries yet)")
            } else {
                for (e in entries) {
                    val mo = monthNames.getOrElse(e.month - 1) { "?" }
                    val payCol = when {
                        e.isMissedPayment -> "NO PAYMENT"
                        else -> "₱${"%.2f".format(e.paymentAmount)}"
                    }
                    val intCol = if (e.interestAdded > 0) "+₱${"%.2f".format(e.interestAdded)} interest" else "-"
                    val shortfall = bill - e.paymentAmount
                    val shortfallCol = if (!e.isMissedPayment && shortfall > 0.01) " (shortfall ₱${"%.2f".format(shortfall)})" else ""
                    appendLine("${mo} ${e.year}  |  $payCol$shortfallCol  |  $intCol  |  ₱${"%.2f".format(e.closingBalance)}")
                }
            }
            appendLine("──────────────────────────────────────────")
            appendLine()
            val missed = entries.count { it.isMissedPayment }
            appendLine("Current Balance : ₱${"%.2f".format(currBal)}")
            appendLine("Months Tracked  : ${entries.size}")
            if (missed > 0) appendLine("Missed Payments : $missed")
            appendLine()
            appendLine("Generated by LoanTrack")
        }
    }
}
