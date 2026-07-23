package com.brandcrafts.erp.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.PrimaryButton
import com.brandcrafts.erp.ui.components.SecondaryButton
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun ForgotPasswordScreen(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    isSuccess: Boolean,
    onEmailChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onReturnToLoginClick: () -> Unit,
    title: String,
    description: String,
    emailLabel: String,
    submitLabel: String,
    returnToLoginLabel: String,
    successTitle: String,
    successDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isSuccess) {
                ForgotPasswordSuccessContent(
                    title = successTitle,
                    description = successDescription,
                    returnToLoginLabel = returnToLoginLabel,
                    onReturnToLoginClick = onReturnToLoginClick,
                )
            } else {
                ForgotPasswordFormContent(
                    email = email,
                    emailError = emailError,
                    isLoading = isLoading,
                    onEmailChange = onEmailChange,
                    onSubmitClick = onSubmitClick,
                    onReturnToLoginClick = onReturnToLoginClick,
                    title = title,
                    description = description,
                    emailLabel = emailLabel,
                    submitLabel = submitLabel,
                    returnToLoginLabel = returnToLoginLabel,
                )
            }
        }
    }
}

@Composable
fun ForgotPasswordRoute(
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                ForgotPasswordUiEffect.NavigateToLogin -> onNavigateToLogin()
                is ForgotPasswordUiEffect.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(effect.error.messageRes()),
                )
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        ForgotPasswordScreen(
            email = state.email,
            emailError = state.emailError?.toMessage(),
            isLoading = state.isLoading,
            isSuccess = state.isSuccess,
            onEmailChange = { viewModel.onEvent(ForgotPasswordUiEvent.EmailChanged(it)) },
            onSubmitClick = { viewModel.onEvent(ForgotPasswordUiEvent.SubmitClicked) },
            onReturnToLoginClick = { viewModel.onEvent(ForgotPasswordUiEvent.ReturnToLoginClicked) },
            title = stringResource(R.string.forgot_password_title),
            description = stringResource(R.string.forgot_password_description),
            emailLabel = stringResource(R.string.login_email_label),
            submitLabel = stringResource(R.string.forgot_password_submit),
            returnToLoginLabel = stringResource(R.string.forgot_password_return_to_login),
            successTitle = stringResource(R.string.forgot_password_success_title),
            successDescription = stringResource(R.string.forgot_password_success_description),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ForgotPasswordFormContent(
    email: String,
    emailError: String?,
    isLoading: Boolean,
    onEmailChange: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onReturnToLoginClick: () -> Unit,
    title: String,
    description: String,
    emailLabel: String,
    submitLabel: String,
    returnToLoginLabel: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = description, style = MaterialTheme.typography.bodyLarge)
        AppTextField(
            value = email,
            onValueChange = onEmailChange,
            label = emailLabel,
            errorMessage = emailError,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        PrimaryButton(
            text = submitLabel,
            onClick = onSubmitClick,
            loading = isLoading,
        )
        SecondaryButton(
            text = returnToLoginLabel,
            onClick = onReturnToLoginClick,
            enabled = !isLoading,
        )
    }
}

@Composable
private fun ForgotPasswordSuccessContent(
    title: String,
    description: String,
    returnToLoginLabel: String,
    onReturnToLoginClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(text = description, style = MaterialTheme.typography.bodyLarge)
        PrimaryButton(text = returnToLoginLabel, onClick = onReturnToLoginClick)
    }
}

@Composable
private fun ForgotPasswordEmailError.toMessage(): String = when (this) {
    ForgotPasswordEmailError.REQUIRED -> stringResource(R.string.login_email_required)
    ForgotPasswordEmailError.INVALID -> stringResource(R.string.login_email_invalid)
}

private fun AuthenticationError.messageRes(): Int = when (this) {
    AuthenticationError.NETWORK_UNAVAILABLE -> R.string.forgot_password_network_error
    else -> R.string.forgot_password_error
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,dpi=420")
@Composable
private fun ForgotPasswordFormPreview() {
    BrandCraftsTheme {
        ForgotPasswordScreen(
            email = "",
            emailError = null,
            isLoading = false,
            isSuccess = false,
            onEmailChange = {},
            onSubmitClick = {},
            onReturnToLoginClick = {},
            title = "Reset your password",
            description = "Enter your work email to receive a reset link.",
            emailLabel = "Work email",
            submitLabel = "Send reset link",
            returnToLoginLabel = "Return to sign in",
            successTitle = "Check your email",
            successDescription = "If an account matches this email, a reset link has been sent.",
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ForgotPasswordSuccessPreview() {
    BrandCraftsTheme(darkTheme = true) {
        ForgotPasswordScreen(
            email = "",
            emailError = null,
            isLoading = false,
            isSuccess = true,
            onEmailChange = {},
            onSubmitClick = {},
            onReturnToLoginClick = {},
            title = "Reset your password",
            description = "Enter your work email to receive a reset link.",
            emailLabel = "Work email",
            submitLabel = "Send reset link",
            returnToLoginLabel = "Return to sign in",
            successTitle = "Check your email",
            successDescription = "If an account matches this email, a reset link has been sent.",
        )
    }
}
