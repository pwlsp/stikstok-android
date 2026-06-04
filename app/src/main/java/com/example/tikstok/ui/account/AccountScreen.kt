package com.example.tikstok.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.tikstok.R
import com.example.tikstok.locale.AppLanguage
import com.example.tikstok.locale.LocaleHelper

@Composable
fun AccountScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Changing the language recreates the activity, so this is re-read on the next composition.
    var language by remember { mutableStateOf(LocaleHelper.current(context)) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.account_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.account_placeholder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text(
            text = stringResource(R.string.language),
            style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { option ->
                FilterChip(
                    selected = language == option,
                    onClick = {
                        language = option
                        LocaleHelper.set(context, option)
                    },
                    label = { Text(stringResource(option.labelRes)) },
                )
            }
        }
    }
}
