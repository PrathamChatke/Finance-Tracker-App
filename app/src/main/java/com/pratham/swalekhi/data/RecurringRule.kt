package com.pratham.swalekhi.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class RecurrenceFrequency { WEEKLY, MONTHLY }

/**
 * A standing rule that auto-creates an [Expense] when due — rent, subscriptions, salary, etc.
 * A daily background worker ([com.pratham.swalekhi.work.RecurringWorker]) advances [nextDueEpochDay]
 * each time it materializes a transaction, so this table always points at the *next* occurrence.
 */
@Entity(tableName = "recurring_rules")
data class RecurringRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val isIncome: Boolean = false,
    val frequency: RecurrenceFrequency,
    // 1-7 (Monday=1) for WEEKLY, 1-31 for MONTHLY (clamped to the month's last day when short).
    val dayOfPeriod: Int,
    val nextDueEpochDay: Long,
    val isActive: Boolean = true
)

class RecurringConverters {
    @TypeConverter
    fun fromFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)
}
