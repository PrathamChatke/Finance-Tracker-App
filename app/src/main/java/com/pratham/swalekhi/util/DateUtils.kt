package com.pratham.swalekhi.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

object DateUtils {
    private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")
    private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val shortMonthLabelFormatter = DateTimeFormatter.ofPattern("MMM yyyy")

    fun epochDayToLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    /** [monthOffset] 0 = current month, -1 = last month, etc. Never allows a future month. */
    fun yearMonthForOffset(monthOffset: Int, today: LocalDate = LocalDate.now()): YearMonth =
        YearMonth.from(today).plusMonths(monthOffset.toLong())

    fun monthRange(yearMonth: YearMonth): Pair<Long, Long> {
        val start = yearMonth.atDay(1).toEpochDay()
        val end = yearMonth.atEndOfMonth().toEpochDay()
        return start to end
    }

    fun currentMonthRange(today: LocalDate = LocalDate.now()): Pair<Long, Long> =
        monthRange(YearMonth.from(today))

    /** Days elapsed *in this specific month*: all of them if it's a past month, up to today if current. */
    fun daysElapsedInMonth(yearMonth: YearMonth, today: LocalDate = LocalDate.now()): Int {
        val current = YearMonth.from(today)
        return when {
            yearMonth < current -> yearMonth.lengthOfMonth()
            yearMonth == current -> today.dayOfMonth
            else -> 0
        }
    }

    fun daysElapsedInMonth(today: LocalDate = LocalDate.now()): Int = today.dayOfMonth

    fun totalDaysInMonth(yearMonth: YearMonth): Int = yearMonth.lengthOfMonth()

    fun totalDaysInMonth(today: LocalDate = LocalDate.now()): Int =
        YearMonth.from(today).lengthOfMonth()

    fun isCurrentOrFutureMonth(yearMonth: YearMonth, today: LocalDate = LocalDate.now()): Boolean =
        yearMonth >= YearMonth.from(today)

    fun formatDayLabel(epochDay: Long): String =
        LocalDate.ofEpochDay(epochDay).format(dayLabelFormatter)

    fun formatMonthLabel(today: LocalDate = LocalDate.now()): String =
        today.format(monthLabelFormatter)

    fun formatMonthLabel(yearMonth: YearMonth): String = yearMonth.format(monthLabelFormatter)

    fun formatShortMonthLabel(yearMonth: YearMonth): String = yearMonth.format(shortMonthLabelFormatter)

    fun formatDayLabelRelative(epochDay: Long, today: LocalDate = LocalDate.now()): String {
        val date = LocalDate.ofEpochDay(epochDay)
        return when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(dayLabelFormatter)
        }
    }

    fun dayOfWeek(epochDay: Long): DayOfWeek = LocalDate.ofEpochDay(epochDay).dayOfWeek
}
