package com.pratham.swalekhi.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val recurringDao: RecurringDao
) {
    // --- Categories ---

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    fun observeCategoriesByType(isIncome: Boolean): Flow<List<Category>> = categoryDao.observeByType(isIncome)

    suspend fun getCategory(id: Long?): Category? = id?.let { categoryDao.getById(it) }

    suspend fun addCategory(category: Category): Long = categoryDao.insert(category)

    suspend fun updateCategory(category: Category) = categoryDao.update(category)

    /** Returns true if deleted, false if this category still has expenses and was kept. */
    suspend fun deleteCategoryIfUnused(category: Category): Boolean {
        if (category.isDefault) return false
        if (categoryDao.expenseCountForCategory(category.id) > 0) return false
        categoryDao.delete(category)
        return true
    }

    suspend fun categoryTotalInRange(categoryId: Long, startDay: Long, endDay: Long): Double =
        expenseDao.totalForCategoryInRange(categoryId, startDay, endDay)

    // --- Accounts ---

    fun observeAccounts(): Flow<List<Account>> = accountDao.observeAll()

    suspend fun getAccount(id: Long?): Account? = id?.let { accountDao.getById(it) }

    suspend fun addAccount(account: Account): Long = accountDao.insert(account)

    suspend fun updateAccount(account: Account) = accountDao.update(account)

    suspend fun deleteAccountIfUnused(account: Account): Boolean {
        if (account.isDefault) return false
        if (expenseDao.expenseCountForAccount(account.id) > 0) return false
        accountDao.delete(account)
        return true
    }

    // --- Expenses / income ---

    fun observeExpensesInRange(startDay: Long, endDay: Long): Flow<List<Expense>> =
        expenseDao.observeInRange(startDay, endDay)

    fun observeTotalInRange(startDay: Long, endDay: Long, isIncome: Boolean = false): Flow<Double> =
        expenseDao.observeTotalInRange(startDay, endDay, isIncome)

    fun observeCategoryTotalsInRange(startDay: Long, endDay: Long, isIncome: Boolean = false): Flow<List<CategorySpend>> =
        expenseDao.observeCategoryTotalsInRange(startDay, endDay, isIncome)

    fun observeExpensesSince(startDay: Long, endDay: Long): Flow<List<Expense>> =
        expenseDao.observeExpensesSince(startDay, endDay)

    suspend fun getExpense(id: Long): Expense? = expenseDao.getById(id)

    suspend fun addExpense(expense: Expense): Long = expenseDao.insert(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.update(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    // --- Recurring rules ---

    fun observeRecurringRules(): Flow<List<RecurringRule>> = recurringDao.observeAll()

    suspend fun getRecurringRule(id: Long): RecurringRule? = recurringDao.getById(id)

    suspend fun addRecurringRule(rule: RecurringRule): Long = recurringDao.insert(rule)

    suspend fun updateRecurringRule(rule: RecurringRule) = recurringDao.update(rule)

    suspend fun deleteRecurringRule(rule: RecurringRule) = recurringDao.delete(rule)

    suspend fun getDueRecurringRules(todayEpochDay: Long): List<RecurringRule> =
        recurringDao.getDue(todayEpochDay)

    suspend fun hasMaterializedRuleToday(ruleId: Long, epochDay: Long): Boolean =
        expenseDao.countForRuleOnDay(ruleId, epochDay) > 0
}
