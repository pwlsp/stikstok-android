package com.example.tikstok.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tikstok.R
import com.example.tikstok.data.auth.AuthRepository
import com.example.tikstok.data.portfolio.PortfolioStore
import com.example.tikstok.ui.invest.Glowing
import com.example.tikstok.ui.invest.formatUsd

private const val TOTAL_STEPS = 3

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableStateOf(0) }

    val suggestedName = remember {
        AuthRepository.currentUser?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }.orEmpty()
    }
    var nickname by rememberSaveable { mutableStateOf(suggestedName) }
    var profileName by rememberSaveable { mutableStateOf("profile #1") }
    var amount by rememberSaveable { mutableStateOf("1000") }

    val parsed = amount.replace(',', '.').toDoubleOrNull()
    val balanceValid = parsed != null && parsed > 0.0 && parsed <= PortfolioStore.MAX_CASH

    val canAdvance = when (step) {
        0 -> nickname.trim().isNotEmpty()
        1 -> profileName.trim().isNotEmpty() && balanceValid
        else -> true
    }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp),
        ) {
            StepDots(current = step)
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.onboarding_step, step + 1, TOTAL_STEPS),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(220)) togetherWith fadeOut(tween(160)))
                },
                label = "onboarding-step",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { current ->
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    when (current) {
                        0 -> NameStep(nickname = nickname, onNicknameChange = { nickname = it })
                        1 -> ProfileStep(
                            profileName = profileName,
                            onProfileNameChange = { profileName = it },
                            amount = amount,
                            onAmountChange = { new ->
                                amount = new.filter { it.isDigit() || it == '.' || it == ',' }
                            },
                            parsed = parsed,
                            balanceValid = balanceValid,
                        )
                        else -> DoneStep(nickname = nickname.trim())
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.height(50.dp),
                    ) {
                        Text(stringResource(R.string.cd_back))
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Button(
                    onClick = {
                        if (step < TOTAL_STEPS - 1) {
                            step++
                        } else {
                            PortfolioStore.completeOnboarding(nickname, profileName, parsed ?: 0.0)
                            onDone()
                        }
                    },
                    enabled = canAdvance,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                ) {
                    Text(
                        text = stringResource(
                            if (step < TOTAL_STEPS - 1) R.string.onboarding_next
                            else R.string.onboarding_finish,
                        ),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDots(current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(TOTAL_STEPS) { index ->
            val reached = index <= current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(
                        color = if (reached) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(50),
                    ),
            )
        }
    }
}

@Composable
private fun StepHeading(title: String, subtitle: String? = null) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    if (subtitle != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun NameStep(nickname: String, onNicknameChange: (String) -> Unit) {
    StepHeading(
        title = stringResource(R.string.onboarding_name_title),
        subtitle = stringResource(R.string.onboarding_name_subtitle),
    )
    OutlinedTextField(
        value = nickname,
        onValueChange = onNicknameChange,
        singleLine = true,
        label = { Text(stringResource(R.string.onboarding_name_label)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileStep(
    profileName: String,
    onProfileNameChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    parsed: Double?,
    balanceValid: Boolean,
) {
    StepHeading(
        title = stringResource(R.string.onboarding_profile_title),
        subtitle = stringResource(R.string.onboarding_profile_intro),
    )
    OutlinedTextField(
        value = profileName,
        onValueChange = onProfileNameChange,
        singleLine = true,
        label = { Text(stringResource(R.string.account_profile_name)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = amount,
        onValueChange = onAmountChange,
        singleLine = true,
        label = { Text(stringResource(R.string.account_starting_balance)) },
        leadingIcon = { Text("$", style = MaterialTheme.typography.titleMedium) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = amount.isNotEmpty() && !balanceValid,
        supportingText = {
            Text(
                stringResource(
                    R.string.account_max_balance,
                    "$" + formatUsd(PortfolioStore.MAX_CASH),
                ),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PortfolioStore.STARTING_BALANCES.forEach { option ->
            FilterChip(
                selected = parsed == option,
                onClick = { onAmountChange(option.toLong().toString()) },
                label = { Text("$" + formatUsd(option).removeSuffix(".00")) },
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(
            R.string.onboarding_profile_virtual,
            "$" + formatUsd(PortfolioStore.MAX_CASH).removeSuffix(".00"),
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun DoneStep(nickname: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Glowing(
            color = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            radius = 40.dp,
            alpha = 0.6f,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.onboarding_done_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.onboarding_done_subtitle, nickname),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
