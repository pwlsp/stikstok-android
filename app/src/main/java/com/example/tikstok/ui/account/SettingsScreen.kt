package com.example.tikstok.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.tikstok.R
import com.example.tikstok.data.auth.AuthRepository
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.locale.AppLanguage
import com.example.tikstok.locale.LocaleHelper
import kotlinx.coroutines.launch

/**
 * Account settings (deck slide 8, right phone): editable account identity, language, and a danger
 * zone. Nickname and email actually update the in-memory account; password, sign out, and delete
 * are presentation-only placeholders until Firebase auth lands.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var language by remember { mutableStateOf(LocaleHelper.current(context)) }

    var editNickname by remember { mutableStateOf(false) }
    var editEmail by remember { mutableStateOf(false) }
    var changePassword by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Header(onBack = onBack)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { UserCard(nickname = PortfolioStore.nickname, email = PortfolioStore.email) }

            item { SectionLabel(stringResource(R.string.settings_account)) }
            item {
                SettingRow(
                    label = stringResource(R.string.settings_nickname),
                    value = PortfolioStore.nickname,
                    action = stringResource(R.string.settings_edit),
                    onClick = { editNickname = true },
                )
            }
            item {
                SettingRow(
                    label = stringResource(R.string.settings_email),
                    value = PortfolioStore.email,
                    action = stringResource(R.string.settings_edit),
                    onClick = { editEmail = true },
                )
            }
            item {
                SettingRow(
                    label = stringResource(R.string.settings_password),
                    value = "•••••••••••",
                    action = stringResource(R.string.settings_change),
                    actionColor = MaterialTheme.colorScheme.primary,
                    onClick = { changePassword = true },
                )
            }

            item { SectionLabel(stringResource(R.string.language)) }
            item {
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

            item { SectionLabel(stringResource(R.string.settings_danger_zone), MaterialTheme.colorScheme.error) }
            item {
                SettingRow(
                    label = null,
                    value = stringResource(R.string.settings_sign_out),
                    action = null,
                    onClick = { confirmSignOut = true },
                )
            }
            item { DeleteAccountCard(onClick = { confirmDelete = true }) }
        }
    }

    if (editNickname) {
        EditFieldDialog(
            title = stringResource(R.string.settings_edit_nickname),
            label = stringResource(R.string.settings_nickname),
            initial = PortfolioStore.nickname,
            onSave = { PortfolioStore.updateNickname(it); editNickname = false },
            onDismiss = { editNickname = false },
        )
    }
    if (editEmail) {
        EditFieldDialog(
            title = stringResource(R.string.settings_edit_email),
            label = stringResource(R.string.settings_email),
            initial = PortfolioStore.email,
            keyboardType = KeyboardType.Email,
            onSave = { PortfolioStore.updateEmail(it); editEmail = false },
            onDismiss = { editEmail = false },
        )
    }
    if (changePassword) {
        PasswordDialog(onDismiss = { changePassword = false })
    }
    if (confirmSignOut) {
        ConfirmDialog(
            title = stringResource(R.string.settings_sign_out),
            message = stringResource(R.string.settings_sign_out_message),
            confirmLabel = stringResource(R.string.settings_sign_out),
            // The auth-state listener in the gate swaps back to the login screen.
            onConfirm = {
                confirmSignOut = false
                AuthRepository.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
    if (confirmDelete) {
        val reauthMessage = stringResource(R.string.auth_reauth_required)
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_title),
            message = stringResource(R.string.settings_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    try {
                        AuthRepository.deleteAccount()
                        // Success: account gone, listener returns us to login.
                    } catch (e: Exception) {
                        // Firebase rejects deletion of a stale session — ask for a fresh sign-in.
                        Toast.makeText(context, reauthMessage, Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun UserCard(nickname: String, email: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = nickname.trim().firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = nickname,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
private fun SettingRow(
    label: String?,
    value: String,
    action: String?,
    onClick: () -> Unit,
    actionColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (label != null) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (label == null) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
            if (action != null) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = actionColor,
                )
                Spacer(Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DeleteAccountCard(onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = error.copy(alpha = 0.14f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = error,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_delete_account),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = error,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.settings_delete_permanent),
                style = MaterialTheme.typography.labelMedium,
                color = error.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun EditFieldDialog(
    title: String,
    label: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(label) },
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun PasswordDialog(onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_change_password)) },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                label = { Text(stringResource(R.string.settings_new_password)) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = password.isNotBlank()) {
                Text(stringResource(R.string.settings_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
