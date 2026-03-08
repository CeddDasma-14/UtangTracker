package com.cedd.utangtracker.presentation.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import com.cedd.utangtracker.data.repository.UtangRepository
import com.cedd.utangtracker.domain.model.DebtType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DebtSortOption(val label: String) {
    DATE_NEWEST("Newest"),
    DATE_OLDEST("Oldest"),
    AMOUNT_HIGH("Amount ↓"),
    AMOUNT_LOW("Amount ↑"),
    DUE_SOONEST("Due Soon"),
    STATUS("Status"),
}

data class DebtListUiState(
    val debts: List<DebtEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val selectedTab: Int = 0,
    val query: String = "",
    val sortOption: DebtSortOption = DebtSortOption.DATE_NEWEST,
    val totalActiveDebtCount: Int = 0
)

@HiltViewModel
class DebtListViewModel @Inject constructor(
    private val repo: UtangRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    private val _tab   = MutableStateFlow(0)
    private val _query = MutableStateFlow("")
    private val _sort  = MutableStateFlow(DebtSortOption.DATE_NEWEST)

    val isPremium: StateFlow<Boolean> = prefs.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPremium(enabled: Boolean) = viewModelScope.launch { prefs.setPremium(enabled) }

    val uiState: StateFlow<DebtListUiState> = combine(
        _tab,
        _query,
        _sort,
        repo.getDebtsByType(DebtType.OWED_TO_ME.value),
        repo.getDebtsByType(DebtType.I_OWE.value),
        repo.getAllPersons()
    ) { values ->
        val tab      = values[0] as Int
        val query    = values[1] as String
        val sort     = values[2] as DebtSortOption
        @Suppress("UNCHECKED_CAST")
        val owedToMe = values[3] as List<DebtEntity>
        @Suppress("UNCHECKED_CAST")
        val iOwe     = values[4] as List<DebtEntity>
        @Suppress("UNCHECKED_CAST")
        val persons  = values[5] as List<PersonEntity>

        val base = if (tab == 0) owedToMe else iOwe
        val filtered = if (query.isBlank()) base else {
            val q = query.trim().lowercase()
            base.filter { debt ->
                val personName = persons.find { it.id == debt.personId }?.name?.lowercase() ?: ""
                personName.contains(q) || debt.purpose.lowercase().contains(q)
            }
        }
        val sorted = when (sort) {
            DebtSortOption.DATE_NEWEST -> filtered.sortedByDescending { it.dateCreated }
            DebtSortOption.DATE_OLDEST -> filtered.sortedBy { it.dateCreated }
            DebtSortOption.AMOUNT_HIGH -> filtered.sortedByDescending { it.amount }
            DebtSortOption.AMOUNT_LOW  -> filtered.sortedBy { it.amount }
            DebtSortOption.DUE_SOONEST -> filtered.sortedWith(compareBy(nullsLast()) { it.dateDue })
            DebtSortOption.STATUS      -> filtered.sortedBy {
                when (it.status) { "OVERDUE" -> 0; "ACTIVE" -> 1; else -> 2 }
            }
        }
        val totalActive = (owedToMe + iOwe).count { it.status != "SETTLED" }
        DebtListUiState(debts = sorted, persons = persons,
            selectedTab = tab, query = query, sortOption = sort,
            totalActiveDebtCount = totalActive)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtListUiState())

    fun selectTab(index: Int)       { _tab.value = index }
    fun setQuery(q: String)         { _query.value = q }
    fun setSort(s: DebtSortOption)  { _sort.value = s }

    fun deleteDebt(debt: DebtEntity) = viewModelScope.launch { repo.deleteDebt(debt) }
    fun undoDelete(debt: DebtEntity) = viewModelScope.launch { repo.saveDebt(debt) }
    fun toggleLock(debt: DebtEntity) = viewModelScope.launch { repo.toggleDebtLock(debt) }
}
