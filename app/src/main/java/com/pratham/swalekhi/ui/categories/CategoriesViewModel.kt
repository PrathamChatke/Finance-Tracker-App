package com.pratham.swalekhi.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val showIncome: Boolean = false,
    val categories: List<Category> = emptyList(),
    val deleteBlockedMessage: String? = null
)

class CategoriesViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private var allCategories: List<Category> = emptyList()

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                allCategories = categories
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val showIncome = _uiState.value.showIncome
        _uiState.value = _uiState.value.copy(categories = allCategories.filter { it.isIncome == showIncome })
    }

    fun onTabSelected(showIncome: Boolean) {
        _uiState.value = _uiState.value.copy(showIncome = showIncome)
        applyFilter()
    }

    fun onDeleteCategory(category: Category) {
        viewModelScope.launch {
            val deleted = repository.deleteCategoryIfUnused(category)
            if (!deleted) {
                val reason = if (category.isDefault) {
                    "\"${category.name}\" is a built-in category and can't be deleted."
                } else {
                    "\"${category.name}\" still has expenses. Move or delete them first."
                }
                _uiState.value = _uiState.value.copy(deleteBlockedMessage = reason)
            }
        }
    }

    fun onDismissMessage() {
        _uiState.value = _uiState.value.copy(deleteBlockedMessage = null)
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CategoriesViewModel(repository) as T
    }
}
