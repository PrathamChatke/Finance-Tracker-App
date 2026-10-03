package com.pratham.swalekhi.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pratham.swalekhi.SwalekhiApp
import com.pratham.swalekhi.ui.accounts.AccountsScreen
import com.pratham.swalekhi.ui.accounts.AccountsViewModel
import com.pratham.swalekhi.ui.accounts.AddAccountScreen
import com.pratham.swalekhi.ui.accounts.AddAccountViewModel
import com.pratham.swalekhi.ui.addexpense.AddExpenseScreen
import com.pratham.swalekhi.ui.addexpense.AddExpenseViewModel
import com.pratham.swalekhi.ui.categories.AddCategoryScreen
import com.pratham.swalekhi.ui.categories.AddCategoryViewModel
import com.pratham.swalekhi.ui.categories.CategoriesScreen
import com.pratham.swalekhi.ui.categories.CategoriesViewModel
import com.pratham.swalekhi.ui.dashboard.DashboardScreen
import com.pratham.swalekhi.ui.dashboard.DashboardViewModel
import com.pratham.swalekhi.ui.history.HistoryScreen
import com.pratham.swalekhi.ui.history.HistoryViewModel
import com.pratham.swalekhi.ui.insights.InsightsScreen
import com.pratham.swalekhi.ui.insights.InsightsViewModel
import com.pratham.swalekhi.ui.recurring.AddRecurringScreen
import com.pratham.swalekhi.ui.recurring.AddRecurringViewModel
import com.pratham.swalekhi.ui.recurring.RecurringScreen
import com.pratham.swalekhi.ui.recurring.RecurringViewModel
import com.pratham.swalekhi.ui.settings.SettingsScreen
import com.pratham.swalekhi.ui.settings.SettingsViewModel

@Composable
fun SwalekhiNavHost(app: SwalekhiApp) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = Screen.bottomTabs.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SwalekhiBottomBar(currentRoute = currentRoute, navController = navController)
            }
        },
        floatingActionButton = {
            if (currentRoute == Screen.Dashboard.route || currentRoute == Screen.History.route) {
                FloatingActionButton(onClick = { navController.navigate(Screen.AddExpense.new()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add expense")
                }
            }
        },
        // Zeroed out deliberately: each destination handles its own safe-area insets (either
        // directly, or via its own Scaffold+TopAppBar). Leaving this at the Material3 default of
        // WindowInsets.safeDrawing would double-count the bottom system-bar/gesture inset on every
        // screen that has its own Scaffold, since bottomBar is empty on those routes.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = viewModel(
                    factory = DashboardViewModel.Factory(app.repository, app.settingsRepository)
                )
                DashboardScreen(viewModel = viewModel)
            }
            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(app.repository, app.settingsRepository)
                )
                HistoryScreen(
                    viewModel = viewModel,
                    onExpenseClick = { id -> navController.navigate(Screen.AddExpense.edit(id)) }
                )
            }
            composable(Screen.Insights.route) {
                val viewModel: InsightsViewModel = viewModel(
                    factory = InsightsViewModel.Factory(app.repository, app.settingsRepository)
                )
                InsightsScreen(viewModel = viewModel)
            }
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(app.repository, app.settingsRepository)
                )
                SettingsScreen(
                    viewModel = viewModel,
                    onOpenCategories = { navController.navigate(Screen.Categories.route) },
                    onOpenAccounts = { navController.navigate(Screen.Accounts.route) },
                    onOpenRecurring = { navController.navigate(Screen.Recurring.route) }
                )
            }

            composable(
                route = Screen.AddExpense.route,
                arguments = listOf(navArgument(Screen.AddExpense.ARG_EXPENSE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entry ->
                val expenseId = entry.arguments?.getString(Screen.AddExpense.ARG_EXPENSE_ID)?.toLongOrNull()
                val viewModel: AddExpenseViewModel = viewModel(
                    factory = AddExpenseViewModel.Factory(app, app.repository, app.settingsRepository, expenseId)
                )
                AddExpenseScreen(
                    viewModel = viewModel,
                    onDone = { navController.popBackStack() },
                    onNavigateToCreateCategory = { isIncome -> navController.navigate(Screen.AddCategory.new(isIncome)) }
                )
            }

            composable(Screen.Categories.route) {
                val viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory(app.repository))
                val currencyCode by app.settingsRepository.currencyCode.collectAsState()
                CategoriesScreen(
                    viewModel = viewModel,
                    currencyCode = currencyCode,
                    onBack = { navController.popBackStack() },
                    onAddCategory = { isIncome -> navController.navigate(Screen.AddCategory.new(isIncome)) },
                    onEditCategory = { id -> navController.navigate(Screen.AddCategory.edit(id)) }
                )
            }
            composable(
                route = Screen.AddCategory.route,
                arguments = listOf(
                    navArgument(Screen.AddCategory.ARG_CATEGORY_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Screen.AddCategory.ARG_IS_INCOME) { type = NavType.BoolType; defaultValue = false }
                )
            ) { entry ->
                val categoryId = entry.arguments?.getString(Screen.AddCategory.ARG_CATEGORY_ID)?.toLongOrNull()
                val initialIsIncome = entry.arguments?.getBoolean(Screen.AddCategory.ARG_IS_INCOME) ?: false
                val viewModel: AddCategoryViewModel = viewModel(
                    factory = AddCategoryViewModel.Factory(app.repository, categoryId, initialIsIncome)
                )
                val currencyCode by app.settingsRepository.currencyCode.collectAsState()
                AddCategoryScreen(viewModel = viewModel, currencyCode = currencyCode, onDone = { navController.popBackStack() })
            }

            composable(Screen.Accounts.route) {
                val viewModel: AccountsViewModel = viewModel(factory = AccountsViewModel.Factory(app.repository))
                AccountsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddAccount = { navController.navigate(Screen.AddAccount.new()) },
                    onEditAccount = { id -> navController.navigate(Screen.AddAccount.edit(id)) }
                )
            }
            composable(
                route = Screen.AddAccount.route,
                arguments = listOf(navArgument(Screen.AddAccount.ARG_ACCOUNT_ID) { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                val accountId = entry.arguments?.getString(Screen.AddAccount.ARG_ACCOUNT_ID)?.toLongOrNull()
                val viewModel: AddAccountViewModel = viewModel(factory = AddAccountViewModel.Factory(app.repository, accountId))
                AddAccountScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
            }

            composable(Screen.Recurring.route) {
                val viewModel: RecurringViewModel = viewModel(factory = RecurringViewModel.Factory(app.repository, app.settingsRepository))
                RecurringScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onAddRule = { navController.navigate(Screen.AddRecurring.new()) },
                    onEditRule = { id -> navController.navigate(Screen.AddRecurring.edit(id)) }
                )
            }
            composable(
                route = Screen.AddRecurring.route,
                arguments = listOf(navArgument(Screen.AddRecurring.ARG_RULE_ID) { type = NavType.StringType; nullable = true; defaultValue = null })
            ) { entry ->
                val ruleId = entry.arguments?.getString(Screen.AddRecurring.ARG_RULE_ID)?.toLongOrNull()
                val viewModel: AddRecurringViewModel = viewModel(
                    factory = AddRecurringViewModel.Factory(app.repository, app.settingsRepository, ruleId)
                )
                AddRecurringScreen(viewModel = viewModel, onDone = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun SwalekhiBottomBar(currentRoute: String?, navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Screen.Dashboard.route,
            onClick = { navController.navigateToTab(Screen.Dashboard.route) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.History.route,
            onClick = { navController.navigateToTab(Screen.History.route) },
            icon = { Icon(Icons.Filled.History, contentDescription = null) },
            label = { Text("History") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Insights.route,
            onClick = { navController.navigateToTab(Screen.Insights.route) },
            icon = { Icon(Icons.Filled.Insights, contentDescription = null) },
            label = { Text("Insights") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Settings.route,
            onClick = { navController.navigateToTab(Screen.Settings.route) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            label = { Text("Settings") }
        )
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
