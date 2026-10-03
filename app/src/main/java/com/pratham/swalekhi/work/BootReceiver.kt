package com.pratham.swalekhi.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pratham.swalekhi.data.SettingsRepository

/**
 * WorkManager persists its own schedule and normally survives a reboot on its own, but a few OEM
 * skins are aggressive about clearing that state — re-asserting the schedule here is cheap
 * insurance so recurring transactions and the daily reminder keep firing.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        WorkScheduler.scheduleRecurringCheck(context)
        val settings = SettingsRepository(context)
        if (settings.dailyReminderEnabled.value) {
            WorkScheduler.scheduleDailyReminder(context, settings.reminderHour.value, settings.reminderMinute.value)
        }
    }
}
