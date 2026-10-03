package com.pratham.swalekhi.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Fixed catalogue of icons a user can attach to a category. Icons are stored on
 * [com.pratham.swalekhi.data.Category] as a stable string key (not a resource id) so we can
 * change the underlying icon assets later without invalidating saved data.
 */
object CategoryIcons {
    val catalogue: Map<String, ImageVector> = mapOf(
        "restaurant" to Icons.Filled.Restaurant,
        "cart" to Icons.Filled.ShoppingCart,
        "car" to Icons.Filled.DirectionsCar,
        "bus" to Icons.Filled.DirectionsBus,
        "bag" to Icons.Filled.ShoppingBag,
        "receipt" to Icons.Filled.Receipt,
        "movie" to Icons.Filled.Movie,
        "music" to Icons.Filled.MusicNote,
        "health" to Icons.Filled.LocalHospital,
        "fitness" to Icons.Filled.FitnessCenter,
        "school" to Icons.Filled.School,
        "flight" to Icons.Filled.Flight,
        "home" to Icons.Filled.Home,
        "pets" to Icons.Filled.Pets,
        "coffee" to Icons.Filled.Coffee,
        "game" to Icons.Filled.SportsEsports,
        "clothes" to Icons.Filled.Checkroom,
        "card" to Icons.Filled.CreditCard,
        "savings" to Icons.Filled.Savings,
        "other" to Icons.Filled.MoreHoriz
    )

    fun iconFor(key: String): ImageVector = catalogue[key] ?: Icons.Filled.Category
}

/** Curated swatches offered when creating/editing a category. */
val CategoryColorPalette = listOf(
    "#FF7043", "#FFB300", "#EC407A", "#AB47BC",
    "#7E57C2", "#5C6BC0", "#42A5F5", "#26C6DA",
    "#26A69A", "#66BB6A", "#9CCC65", "#8D6E63",
    "#78909C", "#D64545"
)

fun parseHexColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: IllegalArgumentException) {
    Color(0xFF78909C)
}
