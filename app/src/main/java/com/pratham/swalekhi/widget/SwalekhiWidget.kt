package com.pratham.swalekhi.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pratham.swalekhi.MainActivity
import com.pratham.swalekhi.data.AppDatabase
import com.pratham.swalekhi.data.ExpenseRepository
import com.pratham.swalekhi.data.SettingsRepository
import com.pratham.swalekhi.prediction.SpendingPredictor
import com.pratham.swalekhi.util.CurrencyFormatter
import com.pratham.swalekhi.util.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** A small home-screen widget showing this month's spend-so-far and projected total. Tapping it
 * opens the app. Kept deliberately plain (no custom colors/background) since Glance's styling
 * surface renders via RemoteViews and is easy to get subtly wrong across widget hosts. */
class SwalekhiWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getInstance(context)
        val repository = ExpenseRepository(db.expenseDao(), db.categoryDao(), db.accountDao(), db.recurringDao())
        val settings = SettingsRepository(context)

        val today = LocalDate.now()
        val (startDay, endDay) = DateUtils.currentMonthRange(today)
        val daysElapsed = DateUtils.daysElapsedInMonth(today)
        val totalDays = DateUtils.totalDaysInMonth(today)

        val expenses = repository.observeExpensesInRange(startDay, endDay).first().filter { !it.isIncome }
        val dailyTotals = expenses.groupBy { it.epochDay }.mapValues { (_, list) -> list.sumOf { it.amount } }
        val currencyCode = settings.currencyCode.first()

        val projection = SpendingPredictor.project(
            dailyTotalsThisMonth = dailyTotals,
            daysElapsed = daysElapsed,
            totalDaysInMonth = totalDays,
            firstEpochDayOfMonth = startDay
        )

        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>())
            ) {
                Text(text = "Projected this month", style = TextStyle(fontSize = 11.sp))
                Text(
                    text = CurrencyFormatter.format(projection.projectedTotal, currencyCode),
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Spent so far: ${CurrencyFormatter.format(projection.spentSoFar, currencyCode)}",
                    style = TextStyle(fontSize = 12.sp)
                )
            }
        }
    }
}

class SwalekhiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SwalekhiWidget()
}
