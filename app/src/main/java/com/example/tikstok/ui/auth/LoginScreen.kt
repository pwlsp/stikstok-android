package com.example.tikstok.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tikstok.R
import com.example.tikstok.ui.invest.Glowing
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }
    val googleConfigured = remember { webClientResId(context) != 0 }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Glowing(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(24.dp),
            radius = 40.dp,
            alpha = 0.40f,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (viewModel.isSignUp) R.string.auth_subtitle_sign_up
                else R.string.auth_subtitle_sign_in,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = viewModel.email,
            onValueChange = viewModel::onEmailChange,
            singleLine = true,
            label = { Text(stringResource(R.string.settings_email)) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            isError = viewModel.error == AuthError.INVALID_CREDENTIALS ||
                viewModel.error == AuthError.EMAIL_IN_USE,
            enabled = !viewModel.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = viewModel.password,
            onValueChange = viewModel::onPasswordChange,
            singleLine = true,
            label = { Text(stringResource(R.string.settings_password)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.auth_hide_password
                            else R.string.auth_show_password,
                        ),
                    )
                }
            },
            isError = viewModel.error == AuthError.WEAK_PASSWORD,
            enabled = !viewModel.loading,
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = viewModel.isSignUp) {
            val mismatch = viewModel.confirmPassword.isNotEmpty() &&
                viewModel.confirmPassword != viewModel.password
            Column {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.auth_confirm_password)) },
                    visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    isError = mismatch,
                    supportingText = if (mismatch) {
                        { Text(stringResource(R.string.auth_password_mismatch)) }
                    } else {
                        null
                    },
                    enabled = !viewModel.loading,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        viewModel.error?.let { err ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = err.message(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = viewModel.canSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            if (viewModel.loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = stringResource(
                        if (viewModel.isSignUp) R.string.auth_sign_up else R.string.auth_sign_in,
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (googleConfigured) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.auth_or),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    viewModel.onGoogleStart()
                    scope.launch {
                        try {
                            val token = requestGoogleIdToken(context)
                            if (token != null) viewModel.onGoogleToken(token)
                            else viewModel.onGoogleFailed()
                        } catch (_: GetCredentialCancellationException) {
                            viewModel.onGoogleCancelled()
                        } catch (_: GetCredentialException) {
                            viewModel.onGoogleFailed()
                        } catch (_: Exception) {
                            viewModel.onGoogleFailed()
                        }
                    }
                },
                enabled = !viewModel.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text(stringResource(R.string.auth_continue_google))
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = viewModel::toggleMode, enabled = !viewModel.loading) {
            Text(
                stringResource(
                    if (viewModel.isSignUp) R.string.auth_have_account
                    else R.string.auth_no_account,
                ),
            )
        }
    }
    }
}

private suspend fun requestGoogleIdToken(context: android.content.Context): String? {
    val resId = webClientResId(context)
    if (resId == 0) return null
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(context.getString(resId))
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    val result = CredentialManager.create(context).getCredential(context, request)
    val credential = result.credential
    return if (credential is CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
    ) {
        GoogleIdTokenCredential.createFrom(credential.data).idToken
    } else {
        null
    }
}

private fun webClientResId(context: android.content.Context): Int =
    context.resources.getIdentifier("default_web_client_id", "string", context.packageName)

@Composable
private fun AuthError.message(): String = stringResource(
    when (this) {
        AuthError.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
        AuthError.EMAIL_IN_USE -> R.string.auth_error_email_in_use
        AuthError.WEAK_PASSWORD -> R.string.auth_error_weak_password
        AuthError.NETWORK -> R.string.auth_error_network
        AuthError.GOOGLE_FAILED -> R.string.auth_error_google
        AuthError.UNKNOWN -> R.string.auth_error_unknown
    },
)
