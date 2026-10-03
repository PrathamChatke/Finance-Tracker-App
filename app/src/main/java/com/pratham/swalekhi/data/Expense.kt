package com.pratham.swalekhi.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single recorded transaction — an expense by default, or income when [isIncome] is set.
 *
 * [epochDay] is the calendar day the transaction belongs to (days since epoch, via
 * [java.time.LocalDate.toEpochDay]) — used for grouping and month math independent of time zone.
 * [createdAt] is the wall-clock instant it was recorded, used only for stable ordering.
 * [photoPath] is an optional path to a receipt image copied into the app's private storage.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId"), Index("epochDay"), Index("accountId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    // Null means the category it belonged to was deleted ("Uncategorized" in the UI).
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val epochDay: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val isIncome: Boolean = false,
    val photoPath: String? = null,
    // Set when this row was auto-created from a RecurringRule, so it isn't duplicated.
    val recurringRuleId: Long? = null
)
