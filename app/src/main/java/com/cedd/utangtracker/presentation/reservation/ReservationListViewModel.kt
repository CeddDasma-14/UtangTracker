package com.cedd.utangtracker.presentation.reservation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.DebtEntity
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.local.entity.ReservationEntity
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReservationListUiState(
    val reservations: List<ReservationEntity> = emptyList(),
    val persons: List<PersonEntity> = emptyList(),
    val filterStatus: String = "ALL"
)

@HiltViewModel
class ReservationListViewModel @Inject constructor(
    private val repo: UtangRepository
) : ViewModel() {

    private val _filterStatus = MutableStateFlow("ALL")

    val uiState: StateFlow<ReservationListUiState> = combine(
        repo.getAllReservations(),
        repo.getAllPersons(),
        _filterStatus
    ) { reservations, persons, filter ->
        val filtered = if (filter == "ALL") reservations
        else reservations.filter { it.status == filter }
        ReservationListUiState(reservations = filtered, persons = persons, filterStatus = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReservationListUiState())

    fun setFilter(status: String) { _filterStatus.value = status }

    fun deleteReservation(r: ReservationEntity) = viewModelScope.launch {
        repo.deleteReservation(r)
    }

    fun updateStatus(r: ReservationEntity, newStatus: String) = viewModelScope.launch {
        repo.updateReservation(r.copy(status = newStatus))
    }

    /** Converts an approved reservation into a real DebtEntity and marks it CONVERTED. */
    fun convertToDebt(r: ReservationEntity, onDone: (Long) -> Unit) = viewModelScope.launch {
        val debtId = repo.saveDebt(
            DebtEntity(
                personId = r.personId,
                type = r.type,
                amount = r.amount,
                purpose = r.purpose,
                dateDue = r.plannedDate,
                notes = r.notes
            )
        )
        repo.updateReservation(r.copy(status = "CONVERTED"))
        onDone(debtId)
    }
}
