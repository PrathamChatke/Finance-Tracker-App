package com.pratham.swalekhi.util

import java.text.NumberFormat
import java.util.Locale

/** A currency the user can pick for display. Amounts are never converted — this only changes how they're shown. */
data class CurrencyOption(val code: String, val symbol: String, val displayName: String)

object Currencies {
    val all = listOf(
        CurrencyOption("INR", "₹", "Indian Rupee"),
        CurrencyOption("USD", "$", "US Dollar"),
        CurrencyOption("EUR", "€", "Euro"),
        CurrencyOption("GBP", "£", "British Pound"),
        CurrencyOption("JPY", "¥", "Japanese Yen"),
        CurrencyOption("AUD", "A$", "Australian Dollar"),
        CurrencyOption("CAD", "C$", "Canadian Dollar"),
        CurrencyOption("SGD", "S$", "Singapore Dollar"),
        CurrencyOption("AED", "د.إ", "UAE Dirham"),
        CurrencyOption("CNY", "¥", "Chinese Yuan"),
        CurrencyOption("CHF", "CHF", "Swiss Franc"),
        CurrencyOption("ZAR", "R", "South African Rand"),
        CurrencyOption("NZD", "NZ$", "New Zealand Dollar"),
        CurrencyOption("SEK", "kr", "Swedish Krona"),
        CurrencyOption("THB", "฿", "Thai Baht")
    )

    val default = all.first { it.code == "INR" }

    fun byCode(code: String?): CurrencyOption = all.firstOrNull { it.code == code } ?: default
}

/**
 * Formats amounts with a user-chosen currency symbol. Grouping/decimal separators follow the
 * device locale (so "1,00,000" vs "100,000" style still feels native), but the symbol itself is
 * whatever currency the user picked in Settings — independent of device locale/region.
 */
object CurrencyFormatter {

    fun format(amount: Double, currencyCode: String = Currencies.default.code): String {
        val option = Currencies.byCode(currencyCode)
        val number = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }.format(amount)
        return "${option.symbol}$number"
    }

    fun formatSigned(amount: Double, currencyCode: String = Currencies.default.code): String {
        val sign = if (amount > 0) "+" else if (amount < 0) "-" else ""
        return sign + format(kotlin.math.abs(amount), currencyCode)
    }
}
