package com.brandcrafts.erp.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.domain.model.AuthenticatedUser

@Composable
fun LoginScreen(
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
    passwordVisibilityLabel: String = "",
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    emailError: String? = null,
    passwordError: String? = null,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            LoginContent(
                email = email,
                password = password,
                onEmailChange = onEmailChange,
                onPasswordChange = onPasswordChange,
                onSignInSuccess = onSignInSuccess,
                onForgotPasswordClick = onForgotPasswordClick,
                brandName = brandName,
                welcomeTitle = welcomeTitle,
                welcomeDescription = welcomeDescription,
                emailLabel = emailLabel,
                passwordLabel = passwordLabel,
                forgotPasswordLabel = forgotPasswordLabel,
                loginLabel = loginLabel,
                passwordVisibilityLabel = passwordVisibilityLabel,
                loading = loading,
                emailError = emailError,
                passwordError = passwordError,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityToggle = onPasswordVisibilityToggle,
            )
        }
    }
}

@Composable
fun LoginRoute(
    onSignInSuccess: (AuthenticatedUser) -> Unit,
    onForgotPasswordClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, context) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is LoginUiEffect.NavigateToMainShell -> onSignInSuccess(effect.user)
                is LoginUiEffect.ShowError -> snackbarHostState.showSnackbar(
                    message = context.getString(effect.error.messageRes()),
                )
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        LoginScreen(
            email = state.email,
            password = state.password,
            onEmailChange = { viewModel.onEvent(LoginUiEvent.EmailChanged(it)) },
            onPasswordChange = { viewModel.onEvent(LoginUiEvent.PasswordChanged(it)) },
            onSignInSuccess = { viewModel.onEvent(LoginUiEvent.SignInClicked) },
            onForgotPasswordClick = onForgotPasswordClick,
            brandName = stringResource(R.string.login_brand_name),
            welcomeTitle = stringResource(R.string.login_welcome_title),
            welcomeDescription = stringResource(R.string.login_welcome_description),
            emailLabel = stringResource(R.string.login_email_label),
            passwordLabel = stringResource(R.string.login_password_label),
            forgotPasswordLabel = stringResource(R.string.login_forgot_password),
            loginLabel = stringResource(R.string.login_sign_in),
            loading = state.isLoading, emailError = state.emailError?.toMessage(), passwordError = state.passwordError?.toMessage(),
            passwordVisibilityLabel = stringResource(if (state.isPasswordVisible) R.string.login_hide_password else R.string.login_show_password),
            isPasswordVisible = state.isPasswordVisible, onPasswordVisibilityToggle = { viewModel.onEvent(LoginUiEvent.PasswordVisibilityToggled) },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun LoginFieldError.toMessage(): String = when (this) {
    LoginFieldError.EMAIL_REQUIRED -> stringResource(R.string.login_email_required)
    LoginFieldError.EMAIL_INVALID -> stringResource(R.string.login_email_invalid)
    LoginFieldError.PASSWORD_REQUIRED -> stringResource(R.string.login_password_required)
}

private fun AuthenticationError.messageRes(): Int = when (this) {
    AuthenticationError.INVALID_CREDENTIALS -> R.string.login_invalid_credentials
    AuthenticationError.ACCOUNT_DISABLED -> R.string.login_account_disabled
    AuthenticationError.USER_PROFILE_MISSING -> R.string.login_account_configuration_error
    AuthenticationError.NETWORK_UNAVAILABLE -> R.string.login_network_error
    AuthenticationError.UNKNOWN -> R.string.login_unknown_error
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,dpi=420")
@Composable
private fun LoginScreenPreview() {
    BrandCraftsTheme {
        LoginScreen(
            email = "",
            password = "",
            onEmailChange = {},
            onPasswordChange = {},
            onSignInSuccess = {},
            onForgotPasswordClick = {},
            brandName = "BrandCrafts ERP",
            welcomeTitle = "Welcome back",
            welcomeDescription = "Sign in to manage your business with confidence.",
            emailLabel = "Work email",
            passwordLabel = "Password",
            forgotPasswordLabel = "Forgot password?",
            loginLabel = "Sign in",
        )
    }
}
