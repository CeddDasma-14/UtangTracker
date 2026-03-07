package com.cedd.utangtracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PREMIUM_PASSWORD = "11996614"

@Composable
fun PremiumUpgradeDialog(
    featureName: String,
    featureDescription: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit
) {
    var showPassword  by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var wrongPassword by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun tryUnlock() {
        if (passwordInput == PREMIUM_PASSWORD) {
            onUpgrade()
        } else {
            wrongPassword = true
            passwordInput = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("\u2B50", fontSize = 36.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (showPassword) "Enter Access Code" else "Premium Feature",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            if (showPassword) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter the premium access code to unlock all features.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value               = passwordInput,
                        onValueChange       = { passwordInput = it; wrongPassword = false },
                        placeholder         = { Text("Access code") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions     = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions     = KeyboardActions(onDone = { tryUnlock() }),
                        isError             = wrongPassword,
                        singleLine          = true,
                        modifier            = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                    if (wrongPassword) {
                        Text(
                            "Incorrect access code. Please try again.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (featureName.isNotBlank()) {
                        Text(
                            featureName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 15.sp,
                            color      = MaterialTheme.colorScheme.primary
                        )
                        Text(featureDescription, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        "Upgrade to Premium to unlock this and all other premium features.",
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (showPassword) tryUnlock() else showPassword = true }) {
                Text(if (showPassword) "Confirm" else "Unlock Premium")
            }
        },
        dismissButton = {
            TextButton(onClick = { if (showPassword) showPassword = false else onDismiss() }) {
                Text(if (showPassword) "Back" else "Maybe Later")
            }
        }
    )
}

/** Small badge shown next to premium-gated labels. */
@Composable
fun PremiumBadge() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            "\u2B50 Premium",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
