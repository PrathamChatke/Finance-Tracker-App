package com.pratham.swalekhi.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Account
import com.pratham.swalekhi.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val deleteBlockedMessage: String? = null
)

class AccountsViewModel(private val repository: ExpenseRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeAccounts().collect { accounts ->
                _uiState.value = _uiState.value.copy(accounts = accounts)
            }
        }
    }

    fun onDeleteAccount(account: Account) {
        viewModelScope.launch {
            val deleted = repository.deleteAccountIfUnused(account)
            if (!deleted) {
                val reason = if (account.isDefault) {
                    "\"${account.name}\" is a built-in account and can't be deleted."
                } else {
                    "\"${account.name}\" still has transactions. Move or delete them first."
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
            AccountsViewModel(repository) as T
    }
}
