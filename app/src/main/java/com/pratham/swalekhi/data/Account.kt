package com.pratham.swalekhi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A wallet/account a transaction was paid from or into — e.g. Cash, Bank, Credit Card. */
@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val isDefault: Boolean = false
)

object DefaultAccounts {
    val seed = listOf(
        Account(name = "Cash", colorHex = "#66BB6A", iconKey = "savings", isDefault = true),
        Account(name = "Bank", colorHex = "#42A5F5", iconKey = "card", isDefault = true)
    )
}
