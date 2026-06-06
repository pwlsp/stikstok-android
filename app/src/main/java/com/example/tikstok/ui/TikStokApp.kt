package com.example.tikstok.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tikstok.R
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.ui.components.MoneyText
import com.example.tikstok.navigation.TikStokDestination
import com.example.tikstok.ui.account.AccountScreen
import com.example.tikstok.ui.account.SettingsScreen
import com.example.tikstok.ui.invest.DownColor
import com.example.tikstok.ui.invest.InvestScreen
import com.example.tikstok.ui.invest.UpColor
import com.example.tikstok.ui.invest.formatUsd
import com.example.tikstok.ui.portfolio.CashHistoryScreen
import com.example.tikstok.ui.portfolio.PortfolioScreen
import com.example.tikstok.ui.portfolio.TransactionHistoryScreen
import kotlinx.coroutines.delay
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TikStokApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    // Detail subpages report their parent tab here, so the bottom bar keeps that tab highlighted.
    val current = tabForRoute(backStackEntry?.destination?.route)
    // Bumped by the top-bar refresh button; InvestScreen re-fetches market data when it changes.
    var refreshTick by remember { mutableStateOf(0) }
    // Whether the top-bar profile switcher dropdown is showing.
    var profileMenuOpen by remember { mutableStateOf(false) }
    // Direction the chip name slides on the next profile change: true = up, false = down.
    var profileSlideUp by remember { mutableStateOf(true) }

    // Brief bottom notice shown whenever the cash cap auto-withdrew an excess anywhere in the app.
    val snackbarHostState = remember { SnackbarHostState() }
    val capReachedMessage = stringResource(
        R.string.cash_cap_reached,
        "$" + formatUsd(PortfolioStore.MAX_CASH),
    )
    // Only react to a *new* cap event, not to the tick value that was already non-zero when this
    // composition started — otherwise recreating the activity (e.g. a language switch) would
    // re-show the notice just because the account is already at the cap.
    var lastCapTick by remember { mutableStateOf(PortfolioStore.cashCapTick) }
    val capTick = PortfolioStore.cashCapTick
    LaunchedEffect(capTick) {
        if (capTick != lastCapTick) {
            lastCapTick = capTick
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message = capReachedMessage, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // Session cash for now; moves to the active profile once Firestore/Room land.
                title = {
                    val delta = PortfolioStore.lastTradeDelta
                    val ts = PortfolioStore.lastTradeTimestamp
                    val activeId = PortfolioStore.active.id
                    // Hold the displayed balance at its pre-trade value while the +/- delta shows,
                    // then reveal the new total — so the effect plays before the number changes.
                    var displayedCash by remember { mutableStateOf(PortfolioStore.cash) }
                    var showDelta by remember(ts) { mutableStateOf(delta != null) }
                    LaunchedEffect(ts) {
                        if (delta != null) {
                            delay(1200)
                            displayedCash = PortfolioStore.cash
                            showDelta = false
                        } else {
                            displayedCash = PortfolioStore.cash
                        }
                    }
                    // Switching profiles shows the new balance instantly, with no trade effect.
                    LaunchedEffect(activeId) {
                        displayedCash = PortfolioStore.cash
                        showDelta = false
                    }
                    val up = (delta ?: 0.0) >= 0.0
                    AnimatedContent(
                        targetState = showDelta && delta != null,
                        transitionSpec = {
                            (slideInVertically { h -> if (up) h else -h } + fadeIn()) togetherWith
                                (slideOutVertically { h -> if (up) -h else h } + fadeOut())
                        },
                        label = "cashOrDelta",
                    ) { showing ->
                        if (showing && delta != null) {
                            val color = if (delta >= 0) UpColor else DownColor
                            val sign = if (delta >= 0) "+" else "-"
                            MoneyText(
                                text = "$sign$" + formatUsd(abs(delta)),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = color,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = UpColor,
                                )
                                MoneyText(
                                    text = formatUsd(displayedCash),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh),
                        )
                    }
                    // The chip shows the active profile and opens a dropdown to switch between
                    // profiles by name; profiles are created and managed on the Account screen.
                    Box(modifier = Modifier.padding(end = 12.dp)) {
                        // Match the dropdown width to the chip so the menu sits flush under it.
                        var chipWidthPx by remember { mutableStateOf(0) }
                        val density = LocalDensity.current
                        AssistChip(
                            // With a single profile there's nothing to switch to, so the chip is inert.
                            onClick = { if (PortfolioStore.profiles.size > 1) profileMenuOpen = true },
                            // Fixed width (~"profile #1") so the chip doesn't jump as names change;
                            // longer names ellipsize. The name slides when the profile changes —
                            // up toward the next profile, down toward the previous.
                            label = {
                                AnimatedContent(
                                    targetState = PortfolioStore.active.id,
                                    transitionSpec = {
                                        if (profileSlideUp) {
                                            (slideInVertically { it } + fadeIn()) togetherWith
                                                (slideOutVertically { -it } + fadeOut())
                                        } else {
                                            (slideInVertically { -it } + fadeIn()) togetherWith
                                                (slideOutVertically { it } + fadeOut())
                                        }
                                    },
                                    label = "profileName",
                                ) { id ->
                                    val name = PortfolioStore.profiles
                                        .firstOrNull { it.id == id }?.name
                                        ?: PortfolioStore.active.name
                                    Text(
                                        text = name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.SwapVert,
                                    contentDescription = stringResource(R.string.cd_profile_switcher),
                                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                leadingIconContentColor = MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .width(120.dp)
                                .onSizeChanged { chipWidthPx = it.width }
                                // Swipe up = next profile, swipe down = previous; tap still opens
                                // the dropdown.
                                .pointerInput(Unit) {
                                    var total = 0f
                                    detectVerticalDragGestures(
                                        onDragStart = { total = 0f },
                                        onVerticalDrag = { _, dragAmount -> total += dragAmount },
                                        onDragEnd = {
                                            val threshold = 24.dp.toPx()
                                            when {
                                                total <= -threshold -> {
                                                    profileSlideUp = true
                                                    PortfolioStore.cycleActiveProfile(1)
                                                }
                                                total >= threshold -> {
                                                    profileSlideUp = false
                                                    PortfolioStore.cycleActiveProfile(-1)
                                                }
                                            }
                                        },
                                    )
                                },
                        )
                        DropdownMenu(
                            expanded = profileMenuOpen,
                            onDismissRequest = { profileMenuOpen = false },
                            // Sit just below the chip, same width.
                            offset = DpOffset(0.dp, 4.dp),
                            modifier = Modifier.width(with(density) { chipWidthPx.toDp() }),
                            // Flat, with the chip's rounded corners, background and outline, so the
                            // menu reads as the chip itself opened up rather than a floating popup.
                            shape = RoundedCornerShape(8.dp),
                            containerColor = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            // Only the other profiles — the active one is already shown on the chip.
                            val others = PortfolioStore.profiles
                                .filter { it.id != PortfolioStore.active.id }
                            others.forEachIndexed { index, profile ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 10.dp),
                                    )
                                }
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    onClick = {
                                        val ps = PortfolioStore.profiles
                                        profileSlideUp =
                                            ps.indexOf(profile) >= ps.indexOf(PortfolioStore.active)
                                        PortfolioStore.selectProfile(profile)
                                        profileMenuOpen = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier.height(44.dp),
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
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
                        // Presentation style: the selected tab just turns copper — no pill behind it.
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = Color.Transparent,
                        ),
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
            composable(TikStokDestination.INVEST.route) { InvestScreen(refreshTick = refreshTick) }
            composable(TikStokDestination.PORTFOLIO.route) {
                PortfolioScreen(
                    refreshTick = refreshTick,
                    onOpenHistory = { symbol ->
                        navController.navigate("history" + if (symbol != null) "?symbol=$symbol" else "")
                    },
                    onOpenCashHistory = { navController.navigate("cash_history") },
                )
            }
            composable(ROUTE_CASH_HISTORY) {
                CashHistoryScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = ROUTE_HISTORY,
                arguments = listOf(
                    navArgument("symbol") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                TransactionHistoryScreen(
                    initialSymbol = entry.arguments?.getString("symbol"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TikStokDestination.ACCOUNT.route) {
                AccountScreen(
                    refreshTick = refreshTick,
                    onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

// Detail subpages that live "under" a tab. They keep the parent tab highlighted, and tapping that
// tab pops back out to its main view.
private const val ROUTE_CASH_HISTORY = "cash_history"
private const val ROUTE_HISTORY = "history?symbol={symbol}"
private const val ROUTE_SETTINGS = "settings"

/** The bottom-nav tab a route belongs to, mapping each detail subpage onto its parent tab. */
private fun tabForRoute(route: String?): TikStokDestination? = when {
    route == null -> null
    route == ROUTE_CASH_HISTORY -> TikStokDestination.PORTFOLIO
    route.startsWith("history") -> TikStokDestination.PORTFOLIO
    route == ROUTE_SETTINGS -> TikStokDestination.ACCOUNT
    else -> TikStokDestination.fromRoute(route)
}

/**
 * Opens a tab's main view from the bottom bar. A nav-bar tap always resets to the first screen:
 * any detail subpages are popped off and no previously saved sub-stack is restored, so e.g. tapping
 * Portfolio while in the transaction history returns to the Portfolio main page.
 */
private fun androidx.navigation.NavHostController.navigateToTab(destination: TikStokDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { inclusive = false }
        launchSingleTop = true
    }
}
