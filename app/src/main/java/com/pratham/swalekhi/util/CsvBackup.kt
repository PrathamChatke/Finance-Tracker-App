package com.pratham.swalekhi.util

import android.content.Context
import android.net.Uri
import com.pratham.swalekhi.data.Account
import com.pratham.swalekhi.data.Category
import com.pratham.swalekhi.data.Expense
import com.pratham.swalekhi.data.ExpenseRepository
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.YearMonth

/**
 * Exports/imports all transactions as CSV — a portable backup format that also opens fine in a
 * spreadsheet. Categories/accounts are referenced by name (created on import if missing) rather
 * than by database id, since ids aren't meaningful across two different installs.
 */
object CsvBackup {
    private const val HEADER = "date,type,amount,category,account,note"

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(current.toString()); current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    suspend fun export(context: Context, uri: Uri, repository: ExpenseRepository): Int {
        val categories = repository.observeCategories().first().associateBy { it.id }
        val accounts = repository.observeAccounts().first().associateBy { it.id }
        // A wide range comfortably covering "all time" for a personal finance app.
        val start = LocalDate.of(2000, 1, 1).toEpochDay()
        val end = LocalDate.now().plusYears(1).toEpochDay()
        // observeExpensesInRange returns every transaction in range regardless of type —
        // income rows included — so no separate income query is needed.
        val expenses = repository.observeExpensesInRange(start, end).first()

        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.appendLine(HEADER)
                for (expense in expenses.sortedBy { it.epochDay }) {
                    val row = listOf(
                        LocalDate.ofEpochDay(expense.epochDay).toString(),
                        if (expense.isIncome) "income" else "expense",
                        expense.amount.toString(),
                        categories[expense.categoryId]?.name ?: "",
                        accounts[expense.accountId]?.name ?: "",
                        expense.note
                    ).joinToString(",") { escape(it) }
                    writer.appendLine(row)
                }
            }
        }
        return expenses.size
    }

    /** Writes a single month's transactions to a cache file suitable for sharing via [android.content.Intent.ACTION_SEND]. */
    suspend fun exportMonthToCacheFile(context: Context, repository: ExpenseRepository, yearMonth: YearMonth): File {
        val categories = repository.observeCategories().first().associateBy { it.id }
        val accounts = repository.observeAccounts().first().associateBy { it.id }
        val start = yearMonth.atDay(1).toEpochDay()
        val end = yearMonth.atEndOfMonth().toEpochDay()
        val expenses = repository.observeExpensesInRange(start, end).first().sortedBy { it.epochDay }

        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "swalekhi_${yearMonth}.csv")
        file.bufferedWriter().use { writer ->
            writer.appendLine(HEADER)
            for (expense in expenses) {
                val row = listOf(
                    LocalDate.ofEpochDay(expense.epochDay).toString(),
                    if (expense.isIncome) "income" else "expense",
                    expense.amount.toString(),
                    categories[expense.categoryId]?.name ?: "",
                    accounts[expense.accountId]?.name ?: "",
                    expense.note
                ).joinToString(",") { escape(it) }
                writer.appendLine(row)
            }
            val income = expenses.filter { it.isIncome }.sumOf { it.amount }
            val expense = expenses.filter { !it.isIncome }.sumOf { it.amount }
            writer.appendLine()
            writer.appendLine("Summary for $yearMonth")
            writer.appendLine("Total income,$income")
            writer.appendLine("Total expense,$expense")
            writer.appendLine("Net,${income - expense}")
        }
        return file
    }

    data class ImportResult(val importedCount: Int, val skippedCount: Int)

    suspend fun import(context: Context, uri: Uri, repository: ExpenseRepository): ImportResult {
        var imported = 0
        var skipped = 0
        val categoryCache = HashMap<Pair<String, Boolean>, Long>()
        val accountCache = HashMap<String, Long>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                val lines = reader.readLines()
                for ((index, line) in lines.withIndex()) {
                    if (index == 0 || line.isBlank()) continue // header
                    val fields = parseCsvLine(line)
                    if (fields.size < 6) { skipped++; continue }

                    val date = runCatching { LocalDate.parse(fields[0].trim()) }.getOrNull()
                    val isIncome = fields[1].trim().equals("income", ignoreCase = true)
                    val amount = fields[2].trim().toDoubleOrNull()
                    if (date == null || amount == null || amount <= 0.0) { skipped++; continue }

                    val categoryName = fields[3].trim()
                    val accountName = fields[4].trim()
                    val note = fields.getOrElse(5) { "" }

                    val categoryId = categoryName.takeIf { it.isNotEmpty() }?.let { name ->
                        categoryCache.getOrPut(name to isIncome) {
                            findOrCreateCategory(repository, name, isIncome)
                        }
                    }
                    val accountId = accountName.takeIf { it.isNotEmpty() }?.let { name ->
                        accountCache.getOrPut(name) { findOrCreateAccount(repository, name) }
                    }

                    repository.addExpense(
                        Expense(
                            amount = amount,
                            categoryId = categoryId,
                            accountId = accountId,
                            note = note,
                            epochDay = date.toEpochDay(),
                            isIncome = isIncome
                        )
                    )
                    imported++
                }
            }
        }
        return ImportResult(imported, skipped)
    }

    private suspend fun findOrCreateCategory(repository: ExpenseRepository, name: String, isIncome: Boolean): Long {
        val existing = repository.observeCategoriesByType(isIncome).first().firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing.id
        return repository.addCategory(
            Category(name = name, colorHex = "#78909C", iconKey = "other", isIncome = isIncome)
        )
    }

    private suspend fun findOrCreateAccount(repository: ExpenseRepository, name: String): Long {
        val existing = repository.observeAccounts().first().firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return existing.id
        return repository.addAccount(Account(name = name, colorHex = "#78909C", iconKey = "savings"))
    }
}
