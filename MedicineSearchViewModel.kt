package com.example.janaushadifinder.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.janaushadifinder.domain.model.MedicineSearch
import com.example.janaushadifinder.domain.repository.MedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MedicineSearchViewModel(
    private val repository: MedicineRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<MedicineSearch>>(emptyList())
    val searchResults: StateFlow<List<MedicineSearch>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.searchMedicines(query)
                _searchResults.value = results
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchByCategory(category: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = repository.getMedicinesByCategory(category)
                _searchResults.value = results
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
}
