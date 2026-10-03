package com.pratham.swalekhi.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Account
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.ui.components.CategoryColorPalette
import com.pratham.swalekhi.ui.components.CategoryIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddAccountUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val colorHex: String = CategoryColorPalette.first(),
    val iconKey: String = "savings",
    val nameError: String? = null,
    val isSaved: Boolean = false,
    val isDefault: Boolean = false
)

class AddAccountViewModel(
    private val repository: ExpenseRepository,
    private val accountId: Long?
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState(isEditing = accountId != null))
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    init {
        val id = accountId
        if (id != null) {
            viewModelScope.launch {
                repository.getAccount(id)?.let { account ->
                    _uiState.value = _uiState.value.copy(
                        name = account.name,
                        colorHex = account.colorHex,
                        iconKey = account.iconKey,
                        isDefault = account.isDefault
                    )
                }
            }
        }
    }

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun onColorSelected(colorHex: String) {
        _uiState.value = _uiState.value.copy(colorHex = colorHex)
    }

    fun onIconSelected(iconKey: String) {
        _uiState.value = _uiState.value.copy(iconKey = iconKey)
    }

    fun onSave() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = "Give it a name")
            return
        }
        viewModelScope.launch {
            val account = Account(
                id = accountId ?: 0,
                name = state.name.trim(),
                colorHex = state.colorHex,
                iconKey = state.iconKey,
                isDefault = state.isDefault
            )
            if (accountId != null) repository.updateAccount(account) else repository.addAccount(account)
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val accountId: Long?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddAccountViewModel(repository, accountId) as T
    }
}
