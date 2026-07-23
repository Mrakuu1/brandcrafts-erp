package com.brandcrafts.erp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.PrimaryButton
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun LoginContent(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSignInSuccess: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    brandName: String,
    welcomeTitle: String,
    welcomeDescription: String,
    emailLabel: String,
    passwordLabel: String,
    forgotPasswordLabel: String,
    loginLabel: String,
    passwordVisibilityLabel: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    emailError: String? = null,
    passwordError: String? = null,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = brandName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(text = welcomeTitle, style = MaterialTheme.typography.headlineMedium)
                Text(text = welcomeDescription, style = MaterialTheme.typography.bodyLarge)
            }
            AppTextField(value = email, onValueChange = onEmailChange, label = emailLabel, errorMessage = emailError)
            AppTextField(
                value = password, onValueChange = onPasswordChange, label = passwordLabel, errorMessage = passwordError,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingContent = { TextButton(onClick = onPasswordVisibilityToggle) { Text(passwordVisibilityLabel) } },
            )
            TextButton(onClick = onForgotPasswordClick) { Text(forgotPasswordLabel) }
            PrimaryButton(text = loginLabel, onClick = onSignInSuccess, modifier = Modifier.fillMaxWidth(), loading = loading)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginContentPreview() {
    BrandCraftsTheme {
        LoginContent(
            email = "designer@brandcrafts.in", password = "", onEmailChange = {}, onPasswordChange = {},
            onSignInSuccess = {}, onForgotPasswordClick = {}, brandName = "BrandCrafts ERP", welcomeTitle = "Welcome back",
            welcomeDescription = "Sign in to manage your business with confidence.", emailLabel = "Work email",
            passwordLabel = "Password", forgotPasswordLabel = "Forgot password?", loginLabel = "Sign in", passwordVisibilityLabel = "Show password", onPasswordVisibilityToggle = {},
        )
    }
}
