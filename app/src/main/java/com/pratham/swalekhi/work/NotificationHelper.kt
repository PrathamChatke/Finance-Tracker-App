package com.pratham.swalekhi.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pratham.swalekhi.MainActivity
import com.pratham.swalekhi.R
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository
import com.pratham.swalekhi.util.CurrencyFormatter
import com.pratham.swalekhi.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate

object NotificationHelper {
    const val CHANNEL_BUDGET = "budget_alerts"
    const val CHANNEL_REMINDER = "daily_reminder"
    const val CHANNEL_RECURRING = "recurring"

    private const val NOTIF_ID_BUDGET = 1001
    private const val NOTIF_ID_REMINDER = 1002
    private const val NOTIF_ID_RECURRING = 1003

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BUDGET, "Budget alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Lets you know when you're approaching or over a budget"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "A daily nudge to log today's expenses"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_RECURRING, "Recurring transactions", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Lets you know when a recurring transaction was logged automatically"
            }
        )
    }

    private fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }

    fun showReminder(context: Context) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Don't forget today's expenses")
            .setContentText("Tap to log anything you've spent today.")
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID_REMINDER, notification)
    }

    fun showRecurringLogged(context: Context, count: Int) {
        if (!canNotify(context) || count <= 0) return
        val notification = NotificationCompat.Builder(context, CHANNEL_RECURRING)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Recurring transactions logged")
            .setContentText(if (count == 1) "1 recurring transaction was added." else "$count recurring transactions were added.")
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID_RECURRING, notification)
    }

    private fun showBudgetAlert(context: Context, title: String, text: String) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_ID_BUDGET, notification)
    }

    /**
     * Compares this month's projected spend against the budget and fires an alert the first time
     * it crosses 80%, 90%, or 100% — tracked per-month in [SettingsRepository] so it only fires once
     * per threshold rather than on every expense saved.
     */
    suspend fun checkBudgetThresholds(
        context: Context,
        repository: ExpenseRepository,
        settingsRepository: SettingsRepository,
        currencyCode: String
    ) {
        val budget = settingsRepository.monthlyBudget.value ?: return
        val today = LocalDate.now()
        val (startDay, endDay) = DateUtils.currentMonthRange(today)
        // observeExpensesInRange is a Flow; take a one-shot snapshot via its first emitted value.
        val snapshot = repository.observeExpensesInRange(startDay, endDay).first()
        val dailyTotals = snapshot.filter { !it.isIncome }
            .groupBy { it.epochDay }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val spentSoFar = dailyTotals.values.sum()

        val progressPercent = if (budget > 0) (spentSoFar / budget * 100).toInt() else 0
        val monthKey = today.year * 100 + today.monthValue
        val lastNotified = settingsRepository.lastNotifiedThreshold(monthKey.toString())

        val crossedThreshold = listOf(100, 90, 80).firstOrNull { progressPercent >= it && lastNotified < it }
        if (crossedThreshold != null) {
            settingsRepository.setLastNotifiedThreshold(monthKey.toString(), crossedThreshold)
            val spentText = CurrencyFormatter.format(spentSoFar, currencyCode)
            val budgetText = CurrencyFormatter.format(budget, currencyCode)
            if (crossedThreshold >= 100) {
                showBudgetAlert(context, "Budget exceeded", "You've spent $spentText of your $budgetText budget this month.")
            } else {
                showBudgetAlert(context, "$crossedThreshold% of budget used", "$spentText of $budgetText spent so far this month.")
            }
        }
    }
}
