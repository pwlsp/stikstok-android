package com.example.tikstok.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.navigation.TikStokDestination
import com.example.tikstok.ui.account.AccountScreen
import com.example.tikstok.ui.invest.DownColor
import com.example.tikstok.ui.invest.InvestScreen
import com.example.tikstok.ui.invest.UpColor
import com.example.tikstok.ui.invest.formatUsd
import com.example.tikstok.ui.portfolio.PortfolioScreen
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikStokApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = TikStokDestination.fromRoute(backStackEntry?.destination?.route)

    Scaffold(
        topBar = {
            TopAppBar(
                // Session cash for now; moves to the active profile once Firestore/Room land.
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "$" + formatUsd(PortfolioStore.cash),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        val delta = PortfolioStore.lastTradeDelta
                        val ts = PortfolioStore.lastTradeTimestamp
                        if (delta != null) {
                            var visible by remember(ts) { mutableStateOf(true) }
                            LaunchedEffect(ts) {
                                delay(2000)
                                visible = false
                            }
                            AnimatedVisibility(
                                visible = visible,
                                enter = fadeIn() + slideInVertically(initialOffsetY = { -it / 2 }),
                                exit = fadeOut()
                            ) {
                                val color = if (delta >= 0) UpColor else DownColor
                                val sign = if (delta >= 0) "+" else "-"
                                Text(
                                    text = " ($sign$" + formatUsd(abs(delta)) + ")",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = color,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    // TODO: open the profile switcher; placeholder until profiles exist.
                    AssistChip(
                        onClick = { },
                        label = { Text(stringResource(R.string.profile_chip)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.SwapVert,
                                contentDescription = stringResource(R.string.cd_profile_switcher),
                                modifier = Modifier.size(AssistChipDefaults.IconSize),
                            )
                        },
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
            )
        },
        bottomBar = {
            // Match the page background so the nav bar blends with the content instead of sitting
            // on the default tinted surface container.
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                TikStokDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = current == destination,
                        onClick = { navController.navigateToTab(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TikStokDestination.START.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TikStokDestination.INVEST.route) { InvestScreen() }
            composable(TikStokDestination.PORTFOLIO.route) { PortfolioScreen() }
            composable(TikStokDestination.ACCOUNT.route) { AccountScreen() }
        }
    }
}

/** Switches tabs while keeping a single back-stack entry per destination and restoring its state. */
private fun androidx.navigation.NavHostController.navigateToTab(destination: TikStokDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
