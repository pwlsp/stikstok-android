package com.example.tikstok.ui.portfolio

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tikstok.R
import com.example.tikstok.ui.components.PlaceholderContent

@Composable
fun PortfolioScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(
        title = stringResource(R.string.portfolio_title),
        message = stringResource(R.string.portfolio_placeholder),
        modifier = modifier,
    )
}
