package com.pratham.swalekhi.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object History : Screen("history")
    data object Insights : Screen("insights")
    data object Settings : Screen("settings")

    data object AddExpense : Screen("add_expense?expenseId={expenseId}") {
        const val ARG_EXPENSE_ID = "expenseId"
        fun new(): String = "add_expense"
        fun edit(expenseId: Long): String = "add_expense?expenseId=$expenseId"
    }

    data object Categories : Screen("categories")
    data object AddCategory : Screen("add_category?categoryId={categoryId}&isIncome={isIncome}") {
        const val ARG_CATEGORY_ID = "categoryId"
        const val ARG_IS_INCOME = "isIncome"
        fun new(isIncome: Boolean): String = "add_category?isIncome=$isIncome"
        fun edit(categoryId: Long): String = "add_category?categoryId=$categoryId"
    }

    data object Accounts : Screen("accounts")
    data object AddAccount : Screen("add_account?accountId={accountId}") {
        const val ARG_ACCOUNT_ID = "accountId"
        fun new(): String = "add_account"
        fun edit(accountId: Long): String = "add_account?accountId=$accountId"
    }

    data object Recurring : Screen("recurring")
    data object AddRecurring : Screen("add_recurring?ruleId={ruleId}") {
        const val ARG_RULE_ID = "ruleId"
        fun new(): String = "add_recurring"
        fun edit(ruleId: Long): String = "add_recurring?ruleId=$ruleId"
    }

    // Onboarding isn't a nav destination — MainActivity gates the whole NavHost behind it
    // (and behind the app-lock screen) before the graph above is ever composed.

    companion object {
        val bottomTabs = listOf(Dashboard, History, Insights, Settings)
    }
}
