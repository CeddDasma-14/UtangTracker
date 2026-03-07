package com.cedd.utangtracker.presentation.reservation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.local.entity.ReservationEntity
import com.cedd.utangtracker.data.repository.UtangRepository
import com.cedd.utangtracker.domain.model.DebtType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditReservationViewModel @Inject constructor(
    private val repo: UtangRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val editId = savedState.get<Long>("reservationId")?.takeIf { it != -1L }
    val prefilledPersonId: Long = savedState.get<Long>("personId")?.takeIf { it != -1L } ?: -1L

    val persons: StateFlow<List<PersonEntity>> = repo.getAllPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _existing = MutableStateFlow<ReservationEntity?>(null)
    val existing: StateFlow<ReservationEntity?> = _existing

    init {
        editId?.let { id ->
            viewModelScope.launch {
                repo.getReservationById(id)?.let { _existing.value = it }
            }
        }
    }

    fun save(
        personId: Long,
        type: DebtType,
        amount: Double,
        purpose: String,
        plannedDate: Long,
        notes: String,
        onDone: () -> Unit
    ) = viewModelScope.launch {
        val r = _existing.value?.copy(
            personId = personId, type = type.value, amount = amount,
            purpose = purpose, plannedDate = plannedDate, notes = notes
        ) ?: ReservationEntity(
            personId = personId, type = type.value, amount = amount,
            purpose = purpose, plannedDate = plannedDate, notes = notes
        )
        if (editId != null) repo.updateReservation(r) else repo.saveReservation(r)
        onDone()
    }
}
