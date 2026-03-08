package com.cedd.utangtracker.presentation.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonDetailUiState(
    val person: PersonEntity? = null,
    val debts: List<DebtEntity> = emptyList()
) {
    val totalOutstanding: Double get() = debts
        .filter { it.status != "SETTLED" }
        .sumOf { debt ->
            val target = if (debt.totalAmount > 0) debt.totalAmount else debt.amount
            (target - debt.paidAmount).coerceAtLeast(0.0)
        }

    val totalSettled: Double get() = debts
        .filter { it.status == "SETTLED" }
        .sumOf { if (it.totalAmount > 0) it.totalAmount else it.amount }

    val activeCount: Int  get() = debts.count { it.status == "ACTIVE" }
    val overdueCount: Int get() = debts.count { it.status == "OVERDUE" }
    val settledCount: Int get() = debts.count { it.status == "SETTLED" }
}

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val repo: UtangRepository,
    private val prefs: PreferencesRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val personId = savedState.get<Long>("personId") ?: -1L

    val isPremium: StateFlow<Boolean> = prefs.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPremium(enabled: Boolean) = viewModelScope.launch { prefs.setPremium(enabled) }
    fun toggleLock(debt: DebtEntity) = viewModelScope.launch { repo.toggleDebtLock(debt) }

    val uiState: StateFlow<PersonDetailUiState> = combine(
        repo.getAllPersons(),
        repo.getDebtsForPerson(personId)
    ) { persons, debts ->
        PersonDetailUiState(
            person = persons.find { it.id == personId },
            debts  = debts
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonDetailUiState())
}
