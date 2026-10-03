package com.pratham.swalekhi.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Category::class, Expense::class, Account::class, RecurringRule::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(RecurringConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun accountDao(): AccountDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "swalekhi.db"
                )
                    .addCallback(SeedCallback(context.applicationContext))
                    // The app has no released users yet, so a schema bump just recreates the
                    // database rather than shipping a migration path.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }

    /**
     * Seeds default categories/accounts from [Callback.onOpen] — called on every launch, not just
     * [Callback.onCreate] — and checks row counts first. onCreate alone isn't reliable here: a
     * schema bump that falls back to a destructive migration recreates the tables without
     * necessarily replaying onCreate, which would otherwise leave a returning user with an empty
     * category list.
     */
    private class SeedCallback(private val context: Context) : Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            CoroutineScope(Dispatchers.IO).launch {
                val instance = getInstance(context)
                if (instance.categoryDao().count() == 0) {
                    instance.categoryDao().insertAll(DefaultCategories.seed)
                }
                if (instance.accountDao().count() == 0) {
                    instance.accountDao().insertAll(DefaultAccounts.seed)
                }
            }
        }
    }
}

/** Categories pre-populated on first launch so the app is usable immediately. */
object DefaultCategories {
    val seed = listOf(
        // Expense categories
        Category(name = "Food & Dining", colorHex = "#FF7043", iconKey = "restaurant", isDefault = true),
        Category(name = "Groceries", colorHex = "#66BB6A", iconKey = "cart", isDefault = true),
        Category(name = "Transport", colorHex = "#42A5F5", iconKey = "car", isDefault = true),
        Category(name = "Shopping", colorHex = "#AB47BC", iconKey = "bag", isDefault = true),
        Category(name = "Bills & Utilities", colorHex = "#FFB300", iconKey = "receipt", isDefault = true),
        Category(name = "Entertainment", colorHex = "#EC407A", iconKey = "movie", isDefault = true),
        Category(name = "Health", colorHex = "#26A69A", iconKey = "health", isDefault = true),
        Category(name = "Other", colorHex = "#78909C", iconKey = "other", isDefault = true),
        // Income categories
        Category(name = "Salary", colorHex = "#2E9E5B", iconKey = "savings", isDefault = true, isIncome = true),
        Category(name = "Freelance", colorHex = "#5C6BC0", iconKey = "card", isDefault = true, isIncome = true),
        Category(name = "Gifts", colorHex = "#EC407A", iconKey = "bag", isDefault = true, isIncome = true),
        Category(name = "Other Income", colorHex = "#78909C", iconKey = "other", isDefault = true, isIncome = true)
    )
}
