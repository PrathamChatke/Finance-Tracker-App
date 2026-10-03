package com.pratham.swalekhi.ui.addexpense

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Account
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.Expense
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository
import com.pratham.swalekhi.util.Currencies
import com.pratham.swalekhi.util.ReceiptStorage
import com.pratham.swalekhi.work.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddExpenseUiState(
    val isEditing: Boolean = false,
    val isIncome: Boolean = false,
    val amountText: String = "",
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val photoPath: String? = null,
    val amountError: String? = null,
    val currencyCode: String = Currencies.default.code,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val expenseId: Long?
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AddExpenseUiState(isEditing = expenseId != null))
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _isIncome = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            _isIncome.flatMapLatest { isIncome -> repository.observeCategoriesByType(isIncome) }
                .collect { categories ->
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
                    selectedAccountId = state.selectedAccountId ?: accounts.firstOrNull { it.isDefault }?.id ?: accounts.firstOrNull()?.id
                )
            }
        }
        viewModelScope.launch {
            settingsRepository.currencyCode.collect { code ->
                _uiState.value = _uiState.value.copy(currencyCode = code)
            }
        }
        val id = expenseId
        if (id != null) {
            viewModelScope.launch {
                repository.getExpense(id)?.let { expense ->
                    _isIncome.value = expense.isIncome
                    _uiState.value = _uiState.value.copy(
                        isIncome = expense.isIncome,
                        amountText = formatAmountForEdit(expense.amount),
                        note = expense.note,
                        date = LocalDate.ofEpochDay(expense.epochDay),
                        selectedCategoryId = expense.categoryId,
                        selectedAccountId = expense.accountId,
                        photoPath = expense.photoPath
                    )
                }
            }
        }
    }

    private fun formatAmountForEdit(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()

    fun onTypeChange(isIncome: Boolean) {
        _isIncome.value = isIncome
        _uiState.value = _uiState.value.copy(isIncome = isIncome, selectedCategoryId = null)
    }

    fun onAmountChange(text: String) {
        val filtered = text.filter { it.isDigit() || it == '.' }
        _uiState.value = _uiState.value.copy(amountText = filtered, amountError = null)
    }

    fun onNoteChange(text: String) {
        _uiState.value = _uiState.value.copy(note = text)
    }

    fun onDateChange(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun onCategorySelected(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onAccountSelected(accountId: Long) {
        _uiState.value = _uiState.value.copy(selectedAccountId = accountId)
    }

    fun onPhotoPicked(uri: Uri) {
        viewModelScope.launch {
            val path = ReceiptStorage.copyToAppStorage(getApplication(), uri)
            if (path != null) {
                ReceiptStorage.delete(_uiState.value.photoPath)
                _uiState.value = _uiState.value.copy(photoPath = path)
            }
        }
    }

    fun onRemovePhoto() {
        ReceiptStorage.delete(_uiState.value.photoPath)
        _uiState.value = _uiState.value.copy(photoPath = null)
    }

    fun onSave() {
        val state = _uiState.value
        val amount = state.amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _uiState.value = state.copy(amountError = "Enter a valid amount")
            return
        }
        viewModelScope.launch {
            val expense = Expense(
                id = expenseId ?: 0,
                amount = amount,
                categoryId = state.selectedCategoryId,
                accountId = state.selectedAccountId,
                note = state.note.trim(),
                epochDay = state.date.toEpochDay(),
                isIncome = state.isIncome,
                photoPath = state.photoPath
            )
            if (expenseId != null) {
                repository.updateExpense(expense)
            } else {
                repository.addExpense(expense)
            }
            if (!state.isIncome) {
                NotificationHelper.checkBudgetThresholds(getApplication(), repository, settingsRepository, state.currencyCode)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    fun onDelete() {
        val id = expenseId ?: return
        viewModelScope.launch {
            repository.getExpense(id)?.let {
                ReceiptStorage.delete(it.photoPath)
                repository.deleteExpense(it)
            }
            _uiState.value = _uiState.value.copy(isDeleted = true)
        }
    }

    class Factory(
        private val application: Application,
        private val repository: ExpenseRepository,
        private val settingsRepository: SettingsRepository,
        private val expenseId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddExpenseViewModel(application, repository, settingsRepository, expenseId) as T
    }
}
