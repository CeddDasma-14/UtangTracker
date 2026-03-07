package com.cedd.utangtracker.presentation.debt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.repository.UtangRepository
import com.cedd.utangtracker.domain.model.DebtType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditDebtViewModel @Inject constructor(
    private val repo: UtangRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val editId = savedState.get<Long>("debtId")?.takeIf { it != -1L }
    val prefilledPersonId: Long = savedState.get<Long>("personId")?.takeIf { it != -1L } ?: -1L

    val persons: StateFlow<List<PersonEntity>> = repo.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _existing = MutableStateFlow<DebtEntity?>(null)
    val existing: StateFlow<DebtEntity?> = _existing

    init {
        editId?.let { id ->
            viewModelScope.launch {
                repo.getDebtWithPayments(id).firstOrNull()?.let { _existing.value = it.debt }
            }
        }
    }

    fun save(
        personId: Long, type: DebtType, amount: Double, purpose: String,
        dateDue: Long?, interestRate: Double, autoApplyInterest: Boolean,
        contractEnabled: Boolean, notes: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        val debt = _existing.value?.copy(
            personId = personId, type = type.value, amount = amount, purpose = purpose,
            dateDue = dateDue, interestRate = interestRate, autoApplyInterest = autoApplyInterest,
            contractEnabled = contractEnabled, notes = notes
        ) ?: DebtEntity(
            personId = personId, type = type.value, amount = amount, purpose = purpose,
            dateDue = dateDue, interestRate = interestRate, autoApplyInterest = autoApplyInterest,
            contractEnabled = contractEnabled, notes = notes
        )
        if (editId != null) repo.updateDebt(debt) else repo.saveDebt(debt)
        onDone()
    }
}
