package com.pratham.swalekhi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A transaction category. Users can create their own in addition to the seeded defaults.
 * Categories are split into expense categories and income categories via [isIncome] — the
 * category picker in Add Transaction filters to whichever type is being recorded.
 *
 * [colorHex] is a "#RRGGBB" string used to tint chips/charts.
 * [iconKey] maps to a fixed set of Material icons via [com.pratham.swalekhi.ui.components.CategoryIcons].
 * [budget] is an optional standing monthly target for this category, compared against actual
 * spend on the dashboard — independent of the overall monthly budget.
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconKey: String,
    val isDefault: Boolean = false,
    val isIncome: Boolean = false,
    val budget: Double? = null
)
