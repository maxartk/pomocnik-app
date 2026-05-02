package cz.kovmak.pomocnik.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cz.kovmak.pomocnik.PomocnikApp
import cz.kovmak.pomocnik.data.database.WorkEntry
import cz.kovmak.pomocnik.data.repository.WorkRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as PomocnikApp).database
    private val repository = WorkRepository(
        database.workEntryDao(),
        cz.kovmak.pomocnik.data.network.OpenRouterApi.create("")
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val entries: StateFlow<List<WorkEntry>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getRecentEntries(50)
            } else {
                repository.searchEntries(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allEntries: StateFlow<List<WorkEntry>> = repository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val entryCount = repository.getAllEntries()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteEntry(entry: WorkEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun deleteAllEntries() {
        viewModelScope.launch {
            repository.deleteAllEntries()
        }
    }
}
