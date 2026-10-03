package com.pratham.swalekhi.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object WorkScheduler {
    private const val RECURRING_WORK_NAME = "recurring_transactions_daily"
    private const val REMINDER_WORK_NAME = "daily_reminder"

    /** Call once at app start — cheap no-op if already scheduled. */
    fun scheduleRecurringCheck(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayUntil(LocalTime.of(0, 5)))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(RECURRING_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayUntil(LocalTime.of(hour, minute)))
            .build()
        // REPLACE so changing the time in Settings takes effect on the next scheduling call.
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(REMINDER_WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, request)
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REMINDER_WORK_NAME)
    }

    private fun initialDelayUntil(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(time)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target)
    }
}
