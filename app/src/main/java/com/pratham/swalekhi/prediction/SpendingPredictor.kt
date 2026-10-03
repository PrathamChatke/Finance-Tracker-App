package com.pratham.swalekhi.prediction

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Projects how much this month will cost based on how much has been spent so far.
 *
 * The model blends two signals so a single big day doesn't wildly swing the forecast, while
 * still reacting to a genuine change of pace:
 *  - a **whole-month run rate** (spend-so-far / days-elapsed), and
 *  - a **recent-days run rate** (last up-to-7 days), weighted more heavily.
 *
 * The blended daily rate is then spread across the remaining days of the month using per-weekday
 * multipliers learned from your past spending (e.g. if Saturdays historically run 40% above your
 * average day, the remaining Saturdays get scaled up accordingly) — falling back to a flat 1.0x
 * multiplier for every day until there's enough history to learn from.
 */
object SpendingPredictor {

    private const val RECENT_WINDOW_DAYS = 7
    private const val RECENT_WEIGHT = 0.6
    private const val MONTH_WEIGHT = 1.0 - RECENT_WEIGHT

    data class Projection(
        val spentSoFar: Double,
        val dailyAverage: Double,
        val projectedTotal: Double,
        val daysElapsed: Int,
        val daysRemaining: Int,
        val totalDaysInMonth: Int,
        val budget: Double?,
    ) {
        /** Null when no budget is set. Positive = projected to go over by this much. */
        val projectedOverBudget: Double?
            get() = budget?.let { projectedTotal - it }

        /** How much can still be spent per remaining day without breaching the budget. */
        val safeToSpendPerDay: Double?
            get() = budget?.let { b ->
                val remaining = b - spentSoFar
                if (daysRemaining <= 0) remaining else remaining / daysRemaining
            }

        val budgetProgress: Float?
            get() = budget?.let { if (it <= 0) 0f else (spentSoFar / it).toFloat().coerceIn(0f, 2f) }
    }

    /**
     * @param dailyTotalsThisMonth epochDay -> amount spent that day, for every day so far this month
     *   (days with no spend can simply be omitted).
     * @param weekdayFactors optional learned multiplier per weekday, relative to an overall average
     *   of 1.0. Pass an empty map to fall back to a flat linear projection.
     */
    fun project(
        dailyTotalsThisMonth: Map<Long, Double>,
        daysElapsed: Int,
        totalDaysInMonth: Int,
        firstEpochDayOfMonth: Long,
        weekdayFactors: Map<DayOfWeek, Double> = emptyMap(),
        budget: Double? = null
    ): Projection {
        val spentSoFar = dailyTotalsThisMonth.values.sum()
        val safeDaysElapsed = daysElapsed.coerceAtLeast(1)
        val monthRate = spentSoFar / safeDaysElapsed

        val recentDays = minOf(RECENT_WINDOW_DAYS, daysElapsed).coerceAtLeast(1)
        val recentStart = firstEpochDayOfMonth + daysElapsed - recentDays
        val recentTotal = (recentStart until firstEpochDayOfMonth + daysElapsed)
            .sumOf { dailyTotalsThisMonth[it] ?: 0.0 }
        val recentRate = recentTotal / recentDays

        val blendedRate = if (daysElapsed >= RECENT_WINDOW_DAYS) {
            recentRate * RECENT_WEIGHT + monthRate * MONTH_WEIGHT
        } else {
            monthRate
        }

        val remainingDays = (totalDaysInMonth - daysElapsed).coerceAtLeast(0)
        var projectedRemaining = 0.0
        for (offset in 1..remainingDays) {
            val epochDay = firstEpochDayOfMonth + daysElapsed + offset - 1
            val weekday = LocalDate.ofEpochDay(epochDay).dayOfWeek
            val factor = weekdayFactors[weekday] ?: 1.0
            projectedRemaining += blendedRate * factor
        }

        return Projection(
            spentSoFar = spentSoFar,
            dailyAverage = monthRate,
            projectedTotal = spentSoFar + projectedRemaining,
            daysElapsed = daysElapsed,
            daysRemaining = remainingDays,
            totalDaysInMonth = totalDaysInMonth,
            budget = budget
        )
    }

    /** Learns a relative multiplier per weekday from historical daily totals. Empty if not enough data. */
    fun weekdayFactorsFrom(historicalDailyTotals: Map<Long, Double>): Map<DayOfWeek, Double> {
        if (historicalDailyTotals.size < 14) return emptyMap()

        val totalsByWeekday = HashMap<DayOfWeek, MutableList<Double>>()
        for ((epochDay, amount) in historicalDailyTotals) {
            val weekday = LocalDate.ofEpochDay(epochDay).dayOfWeek
            totalsByWeekday.getOrPut(weekday) { mutableListOf() }.add(amount)
        }
        if (totalsByWeekday.size < 7) return emptyMap()

        val overallAverage = historicalDailyTotals.values.average()
        if (overallAverage <= 0.0) return emptyMap()

        return totalsByWeekday.mapValues { (_, amounts) ->
            (amounts.average() / overallAverage).coerceIn(0.25, 4.0)
        }
    }
}
