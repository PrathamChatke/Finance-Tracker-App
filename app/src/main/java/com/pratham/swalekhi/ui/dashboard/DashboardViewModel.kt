package com.pratham.swalekhi.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.CategorySpend
import com.pratham.swalekhi.data.Expense
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository
import com.pratham.swalekhi.prediction.SpendingPredictor
import com.pratham.swalekhi.util.Currencies
import com.pratham.swalekhi.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CategoryBreakdownItem(
    val category: Category?,
    val total: Double,
    val fraction: Float
)

data class DashboardUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val monthLabel: String = "",
    val canGoToNextMonth: Boolean = false,
    val projection: SpendingPredictor.Projection? = null,
    val incomeTotal: Double = 0.0,
    val expenseTotal: Double = 0.0,
    val breakdown: List<CategoryBreakdownItem> = emptyList(),
    val categoryBudgets: List<CategoryBudgetProgress> = emptyList(),
    val currencyCode: String = Currencies.default.code,
    val isLoading: Boolean = true
) {
    val netTotal: Double get() = incomeTotal - expenseTotal
}

data class CategoryBudgetProgress(
    val category: Category,
    val spent: Double,
    val budget: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _monthOffset = MutableStateFlow(0)

    private val _showBudgetDialog = MutableStateFlow(false)
    val showBudgetDialog: StateFlow<Boolean> = _showBudgetDialog.asStateFlow()

    init {
        val today = LocalDate.now()
        val (historyStart, historyEnd) = today.minusDays(120).toEpochDay() to today.minusDays(1).toEpochDay()
        val weekdayFactorsFlow = repository.observeExpensesSince(historyStart, historyEnd)
            .map { expenses ->
                val totals = expenses.groupBy { it.epochDay }.mapValues { (_, list) -> list.sumOf { it.amount } }
                SpendingPredictor.weekdayFactorsFrom(totals)
            }

        viewModelScope.launch {
            _monthOffset.flatMapLatest { offset ->
                val yearMonth = DateUtils.yearMonthForOffset(offset, today)
                val (startDay, endDay) = DateUtils.monthRange(yearMonth)
                val daysElapsed = DateUtils.daysElapsedInMonth(yearMonth, today)
                val totalDays = DateUtils.totalDaysInMonth(yearMonth)

                combine(
                    repository.observeExpensesInRange(startDay, endDay),
                    repository.observeCategoryTotalsInRange(startDay, endDay, isIncome = false),
                    repository.observeTotalInRange(startDay, endDay, isIncome = true),
                    repository.observeCategories(),
                    settingsRepository.monthlyBudget,
                    settingsRepository.currencyCode,
                    weekdayFactorsFlow
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val expenses = values[0] as List<Expense>
                    @Suppress("UNCHECKED_CAST")
                    val categoryTotals = values[1] as List<CategorySpend>
                    val incomeTotal = values[2] as Double
                    @Suppress("UNCHECKED_CAST")
                    val categories = values[3] as List<Category>
                    val budget = values[4] as Double?
                    val currencyCode = values[5] as String
                    @Suppress("UNCHECKED_CAST")
                    val weekdayFactors = values[6] as Map<DayOfWeek, Double>

                    val categoriesById = categories.associateBy { it.id }
                    val expenseOnly = expenses.filter { !it.isIncome }
                    val expenseTotal = expenseOnly.sumOf { it.amount }
                    val dailyTotals = expenseOnly.groupBy { it.epochDay }.mapValues { (_, list) -> list.sumOf { it.amount } }

                    val breakdown = categoryTotals
                        .filter { it.total > 0 }
                        .sortedByDescending { it.total }
                        .map { spend ->
                            CategoryBreakdownItem(
                                category = spend.categoryId?.let { categoriesById[it] },
                                total = spend.total,
                                fraction = if (expenseTotal > 0) (spend.total / expenseTotal).toFloat() else 0f
                            )
                        }

                    val categoryBudgets = categories
                        .filter { !it.isIncome && it.budget != null && it.budget > 0 }
                        .map { category ->
                            val spent = categoryTotals.firstOrNull { it.categoryId == category.id }?.total ?: 0.0
                            CategoryBudgetProgress(category, spent, category.budget!!)
                        }
                        .sortedByDescending { it.spent / it.budget }

                    DashboardUiState(
                        yearMonth = yearMonth,
                        monthLabel = DateUtils.formatMonthLabel(yearMonth),
                        canGoToNextMonth = !DateUtils.isCurrentOrFutureMonth(yearMonth, today),
                        projection = SpendingPredictor.project(
                            dailyTotalsThisMonth = dailyTotals,
                            daysElapsed = daysElapsed,
                            totalDaysInMonth = totalDays,
                            firstEpochDayOfMonth = startDay,
                            weekdayFactors = weekdayFactors,
                            budget = budget
                        ),
                        incomeTotal = incomeTotal,
                        expenseTotal = expenseTotal,
                        breakdown = breakdown,
                        categoryBudgets = categoryBudgets,
                        currencyCode = currencyCode,
                        isLoading = false
                    )
                }
            }.collect { _uiState.value = it }
        }
    }

    fun onPreviousMonth() {
        _monthOffset.value -= 1
    }

    fun onNextMonth() {
        if (_uiState.value.canGoToNextMonth) _monthOffset.value += 1
    }

    fun onEditBudgetClick() {
        _showBudgetDialog.value = true
    }

    fun onDismissBudgetDialog() {
        _showBudgetDialog.value = false
    }

    fun onSaveBudget(amount: Double?) {
        settingsRepository.setMonthlyBudget(amount)
        _showBudgetDialog.value = false
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(repository, settingsRepository) as T
    }
}
