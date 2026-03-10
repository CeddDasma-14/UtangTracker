package com.cedd.utangtracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val totalOwedToMe: Double = 0.0,
    val totalIOwe: Double = 0.0,
    val recentDebts: List<DebtEntity> = emptyList(),
    val allDebts: List<DebtEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val activeCount: Int = 0,
    val overdueCount: Int = 0,
    val settledCount: Int = 0,
    val lenderName: String = "",
    val totalPrincipal: Double = 0.0,
    val totalInterestEarned: Double = 0.0,
    val grandTotal: Double = 0.0,
    val totalBalance: Double = 0.0,
    val totalCollected: Double = 0.0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repo: UtangRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getTotalOwedToMe(),
        repo.getTotalIOwe(),
        repo.getAllDebts(),
        repo.getAllPersons(),
        prefs.lenderName
    ) { owedToMe, iOwe, debts, persons, name ->
        val lentDebts      = debts.filter { it.type == "OWED_TO_ME" }
        val totalPrincipal = lentDebts.sumOf { it.amount }
        val grandTotal     = lentDebts.sumOf { if (it.totalAmount > 0) it.totalAmount else it.amount }
        val totalCollected = lentDebts.sumOf { it.paidAmount }
        val totalInterest  = grandTotal - totalPrincipal - lentDebts.sumOf { it.bankCharge }
        val totalBalance   = lentDebts.sumOf { debt ->
            if (debt.ledgerEnabled && debt.ledgerCurrentBalance > 0)
                debt.ledgerCurrentBalance
            else
                ((if (debt.totalAmount > 0) debt.totalAmount else debt.amount) - debt.paidAmount).coerceAtLeast(0.0)
        }
        DashboardUiState(
            totalOwedToMe = owedToMe ?: 0.0,
            totalIOwe = iOwe ?: 0.0,
            recentDebts = debts.take(5),
            allDebts = debts,
            persons = persons,
            activeCount  = debts.count { it.status == "ACTIVE" },
            overdueCount = debts.count { it.status == "OVERDUE" },
            settledCount = debts.count { it.status == "SETTLED" },
            lenderName = name,
            totalPrincipal = totalPrincipal,
            totalInterestEarned = totalInterest.coerceAtLeast(0.0),
            grandTotal = grandTotal,
            totalBalance = totalBalance.coerceAtLeast(0.0),
            totalCollected = totalCollected
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
