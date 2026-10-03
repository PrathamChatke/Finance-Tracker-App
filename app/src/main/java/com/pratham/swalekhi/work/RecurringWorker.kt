package com.pratham.swalekhi.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pratham.swalekhi.data.AppDatabase
import com.pratham.swalekhi.data.Expense
import com.pratham.swalekhi.data.ExpenseRepository
import java.time.LocalDate

/**
 * Runs roughly once a day: finds every active [com.pratham.swalekhi.data.RecurringRule] whose
 * next due date has arrived, materializes it as a real [Expense], and advances the rule to its
 * next occurrence. Guards against double-logging (e.g. the worker running twice on the same day
 * after a reboot) by checking [ExpenseRepository.hasMaterializedRuleToday] first.
 */
class RecurringWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = ExpenseRepository(db.expenseDao(), db.categoryDao(), db.accountDao(), db.recurringDao())

        val today = LocalDate.now()
        val dueRules = repository.getDueRecurringRules(today.toEpochDay())
        var materializedCount = 0

        for (rule in dueRules) {
            if (repository.hasMaterializedRuleToday(rule.id, today.toEpochDay())) continue

            repository.addExpense(
                Expense(
                    amount = rule.amount,
                    categoryId = rule.categoryId,
                    accountId = rule.accountId,
                    note = rule.note,
                    epochDay = today.toEpochDay(),
                    isIncome = rule.isIncome,
                    recurringRuleId = rule.id
                )
            )
            materializedCount++

            val nextDue = RecurrenceCalculator.advance(rule.frequency, rule.dayOfPeriod, today)
            repository.updateRecurringRule(rule.copy(nextDueEpochDay = nextDue.toEpochDay()))
        }

        if (materializedCount > 0) {
            NotificationHelper.ensureChannels(applicationContext)
            NotificationHelper.showRecurringLogged(applicationContext, materializedCount)
        }
        return Result.success()
    }
}
