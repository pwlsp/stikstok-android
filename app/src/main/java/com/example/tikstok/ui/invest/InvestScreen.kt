package com.example.tikstok.ui.invest

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tikstok.R
import com.example.tikstok.ui.components.PlaceholderContent

@Composable
fun InvestScreen(modifier: Modifier = Modifier) {
    PlaceholderContent(
        title = stringResource(R.string.invest_title),
        message = stringResource(R.string.invest_placeholder),
        modifier = modifier,
    )
}
