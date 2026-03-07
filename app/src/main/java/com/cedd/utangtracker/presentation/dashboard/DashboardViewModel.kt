package com.cedd.utangtracker.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class DashboardUiState(
    val totalOwedToMe: Double = 0.0,
    val totalIOwe: Double = 0.0,
    val recentDebts: List<DebtEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val activeCount: Int = 0,
    val overdueCount: Int = 0,
    val settledCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repo: UtangRepository) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repo.getTotalOwedToMe(),
        repo.getTotalIOwe(),
        repo.getAllDebts(),
        repo.getAllPersons()
    ) { owedToMe, iOwe, debts, persons ->
        DashboardUiState(
            totalOwedToMe = owedToMe ?: 0.0,
            totalIOwe = iOwe ?: 0.0,
            recentDebts = debts.take(5),
            persons = persons,
            activeCount  = debts.count { it.status == "ACTIVE" },
            overdueCount = debts.count { it.status == "OVERDUE" },
            settledCount = debts.count { it.status == "SETTLED" }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
