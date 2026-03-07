package com.cedd.utangtracker.presentation.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cedd.utangtracker.data.local.entity.PersonEntity
import com.cedd.utangtracker.data.preferences.PreferencesRepository
import com.cedd.utangtracker.data.repository.UtangRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PersonListUiState(
    val persons: List<PersonEntity> = emptyList(),
    val query: String = "",
    val totalPersonCount: Int = 0
)

@HiltViewModel
class PersonListViewModel @Inject constructor(
    private val repo: UtangRepository,
    private val prefs: PreferencesRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    val isPremium: StateFlow<Boolean> = prefs.isPremium
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setPremium(enabled: Boolean) = viewModelScope.launch { prefs.setPremium(enabled) }

    val uiState: StateFlow<PersonListUiState> = combine(
        repo.getAllPersons(),
        _query
    ) { persons, query ->
        val filtered = if (query.isBlank()) persons else {
            val q = query.trim().lowercase()
            persons.filter { it.name.lowercase().contains(q) || it.phone?.contains(q) == true }
        }
        PersonListUiState(persons = filtered, query = query, totalPersonCount = persons.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonListUiState())

    fun setQuery(q: String) { _query.value = q }
    fun deletePerson(person: PersonEntity) = viewModelScope.launch { repo.deletePerson(person) }
}
