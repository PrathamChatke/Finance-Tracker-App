package com.pratham.swalekhi.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository
import com.pratham.swalekhi.util.Currencies
import com.pratham.swalekhi.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private const val MONTHS_OF_HISTORY = 6

data class MonthTotal(val yearMonth: YearMonth, val total: Double)

data class InsightsUiState(
    val monthlyTotals: List<MonthTotal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedCategoryTotals: List<MonthTotal> = emptyList(),
    val currencyCode: String = Currencies.default.code,
    val isLoading: Boolean = true
)

class InsightsViewModel(
    private val repository: ExpenseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        val today = LocalDate.now()
        val months = (0 until MONTHS_OF_HISTORY).map { YearMonth.from(today).minusMonths(it.toLong()) }.reversed()
        val rangeStart = months.first().atDay(1).toEpochDay()
        val rangeEnd = months.last().atEndOfMonth().toEpochDay()

        viewModelScope.launch {
            combine(
                repository.observeExpensesSince(rangeStart, rangeEnd),
                repository.observeCategoriesByType(false),
                _selectedCategoryId,
                settingsRepository.currencyCode
            ) { expenses, categories, selectedCategoryId, currencyCode ->
                val totalsByMonth = months.associateWith { 0.0 }.toMutableMap()
                val totalsByCategoryAndMonth = HashMap<Long?, MutableMap<YearMonth, Double>>()

                for (expense in expenses) {
                    val month = YearMonth.from(LocalDate.ofEpochDay(expense.epochDay))
                    totalsByMonth[month] = (totalsByMonth[month] ?: 0.0) + expense.amount
                    val categoryMap = totalsByCategoryAndMonth.getOrPut(expense.categoryId) { mutableMapOf() }
                    categoryMap[month] = (categoryMap[month] ?: 0.0) + expense.amount
                }

                val effectiveCategoryId = selectedCategoryId ?: categories.firstOrNull()?.id
                val categoryMonthMap = totalsByCategoryAndMonth[effectiveCategoryId].orEmpty()

                InsightsUiState(
                    monthlyTotals = months.map { MonthTotal(it, totalsByMonth[it] ?: 0.0) },
                    categories = categories,
                    selectedCategoryId = effectiveCategoryId,
                    selectedCategoryTotals = months.map { MonthTotal(it, categoryMonthMap[it] ?: 0.0) },
                    currencyCode = currencyCode,
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onCategorySelected(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InsightsViewModel(repository, settingsRepository) as T
    }
}
