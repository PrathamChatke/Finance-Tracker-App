package com.pratham.swalekhi.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Account
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.RecurrenceFrequency
import com.pratham.swalekhi.data.RecurringRule
import com.pratham.swalekhi.util.Currencies
import com.pratham.swalekhi.work.RecurrenceCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddRecurringUiState(
    val isEditing: Boolean = false,
    val isIncome: Boolean = false,
    val amountText: String = "",
    val note: String = "",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val dayOfPeriod: Int = LocalDate.now().dayOfMonth,
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val currencyCode: String = Currencies.default.code,
    val amountError: String? = null,
    val isSaved: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddRecurringViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: com.pratham.swalekhi.data.SettingsRepository,
    private val ruleId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddRecurringUiState(isEditing = ruleId != null))
    val uiState: StateFlow<AddRecurringUiState> = _uiState.asStateFlow()

    private val _isIncome = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _isIncome.flatMapLatest { repository.observeCategoriesByType(it) }.collect { categories ->
                val state = _uiState.value
                val stillValid = categories.any { it.id == state.selectedCategoryId }
                _uiState.value = state.copy(
                    categories = categories,
                    selectedCategoryId = if (stillValid) state.selectedCategoryId else categories.firstOrNull()?.id
                )
            }
        }
        viewModelScope.launch {
            repository.observeAccounts().collect { accounts ->
                val state = _uiState.value
                _uiState.value = state.copy(
                    accounts = accounts,
                    selectedAccountId = state.selectedAccountId ?: accounts.firstOrNull { it.isDefault }?.id
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.currencyCode.collect { code -> _uiState.value = _uiState.value.copy(currencyCode = code) }
        }
        val id = ruleId
        if (id != null) {
            viewModelScope.launch {
                repository.getRecurringRule(id)?.let { rule ->
                    _isIncome.value = rule.isIncome
                    _uiState.value = _uiState.value.copy(
                        isIncome = rule.isIncome,
                        amountText = formatForEdit(rule.amount),
                        note = rule.note,
                        frequency = rule.frequency,
                        dayOfPeriod = rule.dayOfPeriod,
                        selectedCategoryId = rule.categoryId,
                        selectedAccountId = rule.accountId
                    )
                }
            }
        }
    }

    private fun formatForEdit(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()

    fun onTypeChange(isIncome: Boolean) {
        _isIncome.value = isIncome
        _uiState.value = _uiState.value.copy(isIncome = isIncome, selectedCategoryId = null)
    }

    fun onAmountChange(text: String) {
        _uiState.value = _uiState.value.copy(amountText = text.filter { it.isDigit() || it == '.' }, amountError = null)
    }

    fun onNoteChange(text: String) {
        _uiState.value = _uiState.value.copy(note = text)
    }

    fun onFrequencyChange(frequency: RecurrenceFrequency) {
        val defaultDay = if (frequency == RecurrenceFrequency.WEEKLY) LocalDate.now().dayOfWeek.value else LocalDate.now().dayOfMonth
        _uiState.value = _uiState.value.copy(frequency = frequency, dayOfPeriod = defaultDay)
    }

    fun onDayOfPeriodChange(day: Int) {
        _uiState.value = _uiState.value.copy(dayOfPeriod = day)
    }

    fun onCategorySelected(id: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = id)
    }

    fun onAccountSelected(id: Long) {
        _uiState.value = _uiState.value.copy(selectedAccountId = id)
    }

    fun onSave() {
        val state = _uiState.value
        val amount = state.amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.value = state.copy(amountError = "Enter a valid amount")
            return
        }
        viewModelScope.launch {
            val nextDue = RecurrenceCalculator.nextDueDate(state.frequency, state.dayOfPeriod, LocalDate.now())
            val rule = RecurringRule(
                id = ruleId ?: 0,
                amount = amount,
                categoryId = state.selectedCategoryId,
                accountId = state.selectedAccountId,
                note = state.note.trim(),
                isIncome = state.isIncome,
                frequency = state.frequency,
                dayOfPeriod = state.dayOfPeriod,
                nextDueEpochDay = nextDue.toEpochDay(),
                isActive = true
            )
            if (ruleId != null) repository.updateRecurringRule(rule) else repository.addRecurringRule(rule)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val settingsRepository: com.pratham.swalekhi.data.SettingsRepository,
        private val ruleId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddRecurringViewModel(repository, settingsRepository, ruleId) as T
    }
}
