package com.cedd.utangtracker.presentation.person

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditPersonViewModel @Inject constructor(
    private val repo: UtangRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val editId = savedState.get<Long>("personId")?.takeIf { it != -1L }

    private val _existing = MutableStateFlow<PersonEntity?>(null)
    val existing: StateFlow<PersonEntity?> = _existing

    init {
        editId?.let { id ->
            viewModelScope.launch { _existing.value = repo.getPersonById(id) }
        }
    }

    fun save(name: String, phone: String?, notes: String?, onDone: () -> Unit) = viewModelScope.launch {
        val person = _existing.value?.copy(name = name, phone = phone?.ifBlank { null }, notes = notes?.ifBlank { null })
            ?: PersonEntity(name = name, phone = phone?.ifBlank { null }, notes = notes?.ifBlank { null })
        if (editId != null) repo.updatePerson(person) else repo.savePerson(person)
        onDone()
    }
}
