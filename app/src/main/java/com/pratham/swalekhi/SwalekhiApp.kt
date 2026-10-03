package com.pratham.swalekhi

import android.app.Application
import com.pratham.swalekhi.data.AppDatabase
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository

class SwalekhiApp : Application() {

    val repository: ExpenseRepository by lazy {
        val db = AppDatabase.getInstance(this)
        ExpenseRepository(db.expenseDao(), db.categoryDao(), db.accountDao(), db.recurringDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
}
