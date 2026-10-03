package com.pratham.swalekhi.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategorySpend(
    val categoryId: Long?,
    val total: Double
)

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE epochDay BETWEEN :startDay AND :endDay ORDER BY epochDay DESC, createdAt DESC")
    fun observeInRange(startDay: Long, endDay: Long): Flow<List<Expense>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE epochDay BETWEEN :startDay AND :endDay AND isIncome = :isIncome")
    fun observeTotalInRange(startDay: Long, endDay: Long, isIncome: Boolean): Flow<Double>

    @Query(
        "SELECT categoryId, COALESCE(SUM(amount), 0) as total FROM expenses " +
            "WHERE epochDay BETWEEN :startDay AND :endDay AND isIncome = :isIncome GROUP BY categoryId"
    )
    fun observeCategoryTotalsInRange(startDay: Long, endDay: Long, isIncome: Boolean): Flow<List<CategorySpend>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE categoryId = :categoryId AND epochDay BETWEEN :startDay AND :endDay")
    suspend fun totalForCategoryInRange(categoryId: Long, startDay: Long, endDay: Long): Double

    /** Wide-range expense-only rows, used to compute month-over-month and weekday trend stats client-side. */
    @Query("SELECT * FROM expenses WHERE isIncome = 0 AND epochDay BETWEEN :startDay AND :endDay ORDER BY epochDay ASC")
    fun observeExpensesSince(startDay: Long, endDay: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    @Query("SELECT COUNT(*) FROM expenses WHERE accountId = :accountId")
    suspend fun expenseCountForAccount(accountId: Long): Int

    @Query("SELECT COUNT(*) FROM expenses WHERE recurringRuleId = :ruleId AND epochDay = :epochDay")
    suspend fun countForRuleOnDay(ruleId: Long, epochDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}
