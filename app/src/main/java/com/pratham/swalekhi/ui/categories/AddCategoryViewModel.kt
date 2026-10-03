package com.pratham.swalekhi.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.ui.components.CategoryColorPalette
import com.pratham.swalekhi.ui.components.CategoryIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddCategoryUiState(
    val isEditing: Boolean = false,
    val isIncome: Boolean = false,
    val name: String = "",
    val colorHex: String = CategoryColorPalette.first(),
    val iconKey: String = CategoryIcons.catalogue.keys.first(),
    val budgetText: String = "",
    val nameError: String? = null,
    val isSaved: Boolean = false,
    val isDefault: Boolean = false
)

class AddCategoryViewModel(
    private val repository: ExpenseRepository,
    private val categoryId: Long?,
    initialIsIncome: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCategoryUiState(isEditing = categoryId != null, isIncome = initialIsIncome))
    val uiState: StateFlow<AddCategoryUiState> = _uiState.asStateFlow()

    init {
        val id = categoryId
        if (id != null) {
            viewModelScope.launch {
                repository.getCategory(id)?.let { category ->
                    _uiState.value = _uiState.value.copy(
                        isIncome = category.isIncome,
                        name = category.name,
                        colorHex = category.colorHex,
                        iconKey = category.iconKey,
                        budgetText = category.budget?.let { formatForEdit(it) } ?: "",
                        isDefault = category.isDefault
                    )
                }
            }
        }
    }

    private fun formatForEdit(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()

    fun onNameChange(name: String) {
        _uiState.value = _uiState.value.copy(name = name, nameError = null)
    }

    fun onColorSelected(colorHex: String) {
        _uiState.value = _uiState.value.copy(colorHex = colorHex)
    }

    fun onIconSelected(iconKey: String) {
        _uiState.value = _uiState.value.copy(iconKey = iconKey)
    }

    fun onBudgetChange(text: String) {
        _uiState.value = _uiState.value.copy(budgetText = text.filter { it.isDigit() || it == '.' })
    }

    fun onSave() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(nameError = "Give it a name")
            return
        }
        viewModelScope.launch {
            val category = Category(
                id = categoryId ?: 0,
                name = state.name.trim(),
                colorHex = state.colorHex,
                iconKey = state.iconKey,
                isDefault = state.isDefault,
                isIncome = state.isIncome,
                budget = state.budgetText.toDoubleOrNull()?.takeIf { it > 0 }
            )
            if (categoryId != null) {
                repository.updateCategory(category)
            } else {
                repository.addCategory(category)
            }
            _uiState.value = _uiState.value.copy(isSaved = true)
        }
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val categoryId: Long?,
        private val initialIsIncome: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AddCategoryViewModel(repository, categoryId, initialIsIncome) as T
    }
}
