package com.pratham.swalekhi.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository
import com.pratham.swalekhi.util.CsvBackup
import com.pratham.swalekhi.util.Currencies
import com.pratham.swalekhi.util.ShareUtil
import com.pratham.swalekhi.work.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.YearMonth

data class SettingsUiState(
    val budget: Double? = null,
    val currencyCode: String = Currencies.default.code,
    val appLockEnabled: Boolean = false,
    val dailyReminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val statusMessage: String? = null
)

class SettingsViewModel(
    private val repository: ExpenseRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.monthlyBudget,
                settingsRepository.currencyCode,
                settingsRepository.appLockEnabled,
                settingsRepository.dailyReminderEnabled,
                settingsRepository.reminderHour,
                settingsRepository.reminderMinute
            ) { values ->
                SettingsUiState(
                    budget = values[0] as Double?,
                    currencyCode = values[1] as String,
                    appLockEnabled = values[2] as Boolean,
                    dailyReminderEnabled = values[3] as Boolean,
                    reminderHour = values[4] as Int,
                    reminderMinute = values[5] as Int,
                    statusMessage = _uiState.value.statusMessage
                )
            }.collect { _uiState.value = it }
        }
    }

    fun onSaveBudget(amount: Double?) {
        settingsRepository.setMonthlyBudget(amount)
    }

    fun onCurrencySelected(code: String) {
        settingsRepository.setCurrencyCode(code)
    }

    fun onAppLockToggle(enabled: Boolean, canAuthenticate: Boolean) {
        if (enabled && !canAuthenticate) {
            _uiState.value = _uiState.value.copy(statusMessage = "Set up a screen lock or fingerprint on your phone first.")
            return
        }
        settingsRepository.setAppLockEnabled(enabled)
    }

    fun onReminderToggle(context: Context, enabled: Boolean) {
        val state = _uiState.value
        settingsRepository.setDailyReminder(enabled, state.reminderHour, state.reminderMinute)
        if (enabled) {
            WorkScheduler.scheduleDailyReminder(context, state.reminderHour, state.reminderMinute)
        } else {
            WorkScheduler.cancelDailyReminder(context)
        }
    }

    fun onReminderTimeChange(context: Context, hour: Int, minute: Int) {
        settingsRepository.setDailyReminder(true, hour, minute)
        WorkScheduler.scheduleDailyReminder(context, hour, minute)
    }

    fun onExport(context: Context, uri: Uri) {
        viewModelScope.launch {
            val count = CsvBackup.export(context, uri, repository)
            _uiState.value = _uiState.value.copy(statusMessage = "Exported $count transactions.")
        }
    }

    fun onImport(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = CsvBackup.import(context, uri, repository)
            _uiState.value = _uiState.value.copy(
                statusMessage = "Imported ${result.importedCount} transactions" +
                    if (result.skippedCount > 0) " (${result.skippedCount} skipped)." else "."
            )
        }
    }

    fun onShareReport(context: Context) {
        viewModelScope.launch {
            val file = CsvBackup.exportMonthToCacheFile(context, repository, YearMonth.now())
            ShareUtil.shareCsvFile(context, file)
        }
    }

    fun onDismissMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    class Factory(
        private val repository: ExpenseRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(repository, settingsRepository) as T
    }
}
