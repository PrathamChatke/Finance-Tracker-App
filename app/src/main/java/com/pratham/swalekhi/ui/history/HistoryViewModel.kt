package com.pratham.swalekhi.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Account
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.Expense
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository
import com.pratham.swalekhi.util.Currencies
import com.pratham.swalekhi.util.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class ExpenseListItem(val expense: Expense, val category: Category?, val account: Account?)

data class DayGroup(val epochDay: Long, val items: List<ExpenseListItem>, val dayTotal: Double)

data class HistoryUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val monthLabel: String = "",
    val canGoToNextMonth: Boolean = false,
    val groups: List<DayGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val searchQuery: String = "",
    val monthTotal: Double = 0.0,
    val currencyCode: String = Currencies.default.code,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _monthOffset = MutableStateFlow(0)
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var pendingDelete: Expense? = null

    init {
        val today = LocalDate.now()
        viewModelScope.launch {
            _monthOffset.flatMapLatest { offset ->
                val yearMonth = DateUtils.yearMonthForOffset(offset, today)
                val (startDay, endDay) = DateUtils.monthRange(yearMonth)

                combine(
                    repository.observeExpensesInRange(startDay, endDay),
                    repository.observeCategories(),
                    repository.observeAccounts(),
                    _selectedCategoryId,
                    _selectedAccountId,
                    _searchQuery,
                    settingsRepository.currencyCode
                ) { values ->
                    @Suppress("UNCHECKED_CAST")
                    val expenses = values[0] as List<Expense>
                    @Suppress("UNCHECKED_CAST")
                    val categories = values[1] as List<Category>
                    @Suppress("UNCHECKED_CAST")
                    val accounts = values[2] as List<Account>
                    val selectedCategoryId = values[3] as Long?
                    val selectedAccountId = values[4] as Long?
                    val query = values[5] as String
                    val currencyCode = values[6] as String

                    val categoriesById = categories.associateBy { it.id }
                    val accountsById = accounts.associateBy { it.id }

                    var filtered = expenses.filter { !it.isIncome }
                    if (selectedCategoryId != null) filtered = filtered.filter { it.categoryId == selectedCategoryId }
                    if (selectedAccountId != null) filtered = filtered.filter { it.accountId == selectedAccountId }
                    if (query.isNotBlank()) {
                        filtered = filtered.filter { it.note.contains(query, ignoreCase = true) }
                    }

                    val groups = filtered
                        .groupBy { it.epochDay }
                        .toSortedMap(compareByDescending { it })
                        .map { (day, items) ->
                            DayGroup(
                                epochDay = day,
                                items = items.map { ExpenseListItem(it, categoriesById[it.categoryId], accountsById[it.accountId]) },
                                dayTotal = items.sumOf { it.amount }
                            )
                        }

                    HistoryUiState(
                        yearMonth = yearMonth,
                        monthLabel = DateUtils.formatMonthLabel(yearMonth),
                        canGoToNextMonth = !DateUtils.isCurrentOrFutureMonth(yearMonth, today),
                        groups = groups,
                        categories = categories,
                        accounts = accounts,
                        selectedCategoryId = selectedCategoryId,
                        selectedAccountId = selectedAccountId,
                        searchQuery = query,
                        monthTotal = filtered.sumOf { it.amount },
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

    fun onCategoryFilterSelected(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun onAccountFilterSelected(accountId: Long?) {
        _selectedAccountId.value = accountId
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    /** Removes the expense immediately but keeps it around for [undoDelete] until the snackbar times out. */
    fun onDeleteExpense(item: ExpenseListItem) {
        pendingDelete = item.expense
        viewModelScope.launch { repository.deleteExpense(item.expense) }
    }

    fun undoDelete() {
        val expense = pendingDelete ?: return
        pendingDelete = null
        viewModelScope.launch { repository.addExpense(expense.copy(id = 0)) }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HistoryViewModel(repository, settingsRepository) as T
    }
}
