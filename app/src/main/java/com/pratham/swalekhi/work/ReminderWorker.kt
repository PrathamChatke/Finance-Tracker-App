package com.pratham.swalekhi.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Posts the "log today's expenses" nudge. Rescheduled by [WorkScheduler] whenever the user
 * changes the reminder time, so it keeps firing at roughly the same time each day. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        NotificationHelper.ensureChannels(applicationContext)
        NotificationHelper.showReminder(applicationContext)
        return Result.success()
    }
}
