package com.pratham.swalekhi.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.RecurringRule
import com.pratham.swalekhi.util.Currencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RecurringListItem(val rule: RecurringRule, val category: Category?)

data class RecurringUiState(
    val items: List<RecurringListItem> = emptyList(),
    val currencyCode: String = Currencies.default.code
)

class RecurringViewModel(
    private val repository: ExpenseRepository,
    settingsRepository: com.pratham.swalekhi.data.SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeRecurringRules(),
                repository.observeCategories(),
                settingsRepository.currencyCode
            ) { rules, categories, currencyCode ->
                val categoriesById = categories.associateBy { it.id }
                RecurringUiState(
                    items = rules.map { RecurringListItem(it, categoriesById[it.categoryId]) },
                    currencyCode = currencyCode
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onToggleActive(rule: RecurringRule) {
        viewModelScope.launch { repository.updateRecurringRule(rule.copy(isActive = !rule.isActive)) }
    }

    fun onDelete(rule: RecurringRule) {
        viewModelScope.launch { repository.deleteRecurringRule(rule) }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val settingsRepository: com.pratham.swalekhi.data.SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RecurringViewModel(repository, settingsRepository) as T
    }
}
