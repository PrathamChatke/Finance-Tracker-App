package com.pratham.swalekhi.data

import android.content.Context
import com.pratham.swalekhi.util.Currencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * SharedPreferences-backed store for every app-wide setting: the optional monthly budget,
 * display currency, app-lock toggle, daily reminder time, and onboarding/notification bookkeeping.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("swalekhi_settings", Context.MODE_PRIVATE)

    private val _monthlyBudget = MutableStateFlow(readBudget())
    val monthlyBudget: StateFlow<Double?> = _monthlyBudget

    private val _currencyCode = MutableStateFlow(readCurrencyCode())
    val currencyCode: StateFlow<String> = _currencyCode

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled

    private val _dailyReminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_REMINDER_ENABLED, false))
    val dailyReminderEnabled: StateFlow<Boolean> = _dailyReminderEnabled

    private val _reminderHour = MutableStateFlow(prefs.getInt(KEY_REMINDER_HOUR, 20))
    val reminderHour: StateFlow<Int> = _reminderHour

    private val _reminderMinute = MutableStateFlow(prefs.getInt(KEY_REMINDER_MINUTE, 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute

    val hasSeenOnboarding: Boolean get() = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)

    private fun readBudget(): Double? {
        if (!prefs.contains(KEY_BUDGET)) return null
        val raw = prefs.getFloat(KEY_BUDGET, 0f)
        return if (raw <= 0f) null else raw.toDouble()
    }

    private fun readCurrencyCode(): String =
        prefs.getString(KEY_CURRENCY, null) ?: Currencies.default.code

    fun setMonthlyBudget(amount: Double?) {
        prefs.edit().apply {
            if (amount == null || amount <= 0.0) {
                remove(KEY_BUDGET)
            } else {
                putFloat(KEY_BUDGET, amount.toFloat())
            }
        }.apply()
        _monthlyBudget.value = if (amount != null && amount > 0.0) amount else null
    }

    fun setCurrencyCode(code: String) {
        prefs.edit().putString(KEY_CURRENCY, code).apply()
        _currencyCode.value = code
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLockEnabled.value = enabled
    }

    fun setDailyReminder(enabled: Boolean, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_REMINDER_ENABLED, enabled)
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        _dailyReminderEnabled.value = enabled
        _reminderHour.value = hour
        _reminderMinute.value = minute
    }

    fun markOnboardingSeen() {
        prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, true).apply()
    }

    /** Highest budget-threshold percentage (80/90/100) already notified for this month, or 0. */
    fun lastNotifiedThreshold(monthKey: String): Int =
        prefs.getInt(KEY_THRESHOLD_PREFIX + monthKey, 0)

    fun setLastNotifiedThreshold(monthKey: String, threshold: Int) {
        prefs.edit().putInt(KEY_THRESHOLD_PREFIX + monthKey, threshold).apply()
    }

    companion object {
        private const val KEY_BUDGET = "monthly_budget"
        private const val KEY_CURRENCY = "currency_code"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_ONBOARDING_SEEN = "onboarding_seen"
        private const val KEY_THRESHOLD_PREFIX = "notified_threshold_"
    }
}
