package com.pratham.swalekhi.work

import com.pratham.swalekhi.data.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.LocalDate

/** Pure date math for [com.pratham.swalekhi.data.RecurringRule] — shared by the "create rule" UI
 * (to compute the first due date) and [RecurringWorker] (to advance a rule after it fires). */
object RecurrenceCalculator {

    /** Next occurrence on/after [from] for the given cadence. */
    fun nextDueDate(frequency: RecurrenceFrequency, dayOfPeriod: Int, from: LocalDate): LocalDate = when (frequency) {
        RecurrenceFrequency.WEEKLY -> {
            val targetDow = DayOfWeek.of(dayOfPeriod.coerceIn(1, 7))
            var candidate = from
            while (candidate.dayOfWeek != targetDow) candidate = candidate.plusDays(1)
            candidate
        }
        RecurrenceFrequency.MONTHLY -> {
            val day = dayOfPeriod.coerceIn(1, 31).coerceAtMost(from.lengthOfMonth())
            var candidate = from.withDayOfMonth(day)
            if (candidate.isBefore(from)) {
                val nextMonth = from.plusMonths(1)
                candidate = nextMonth.withDayOfMonth(dayOfPeriod.coerceIn(1, 31).coerceAtMost(nextMonth.lengthOfMonth()))
            }
            candidate
        }
    }

    /** The occurrence strictly after [current] — used to advance a rule once it has fired. */
    fun advance(frequency: RecurrenceFrequency, dayOfPeriod: Int, current: LocalDate): LocalDate =
        nextDueDate(frequency, dayOfPeriod, current.plusDays(1))
}
