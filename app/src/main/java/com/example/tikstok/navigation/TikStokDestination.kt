package com.example.tikstok.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.tikstok.R

/** Top-level destinations shown in the bottom navigation bar. */
enum class TikStokDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    INVEST("invest", R.string.tab_invest, Icons.AutoMirrored.Filled.TrendingUp),
    PORTFOLIO("portfolio", R.string.tab_portfolio, Icons.Filled.PieChart),
    ACCOUNT("account", R.string.tab_account, Icons.Filled.AccountCircle);

    companion object {
        val START = INVEST

        fun fromRoute(route: String?): TikStokDestination =
            entries.firstOrNull { it.route == route } ?: START
    }
}
