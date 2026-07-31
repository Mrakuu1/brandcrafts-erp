package com.brandcrafts.erp.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.domain.model.AuthenticatedUser
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme

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
    val darkMode = MaterialTheme.colorScheme.background.luminance() < .5f
    val foreground = if (darkMode) Color(0xFFF8FAFC) else Color(0xFF141414)
    CompositionLocalProvider(LocalContentColor provides foreground) {
        Box(modifier = modifier.fillMaxSize()) {
            LoginBackground(modifier = Modifier.fillMaxSize(), darkMode = darkMode)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, top = 96.dp, end = 24.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                LoginBrandHeader(brandName = brandName)
                LoginContent(
                    modifier = Modifier.padding(top = 96.dp, bottom = 32.dp),
                    email = email,
                    password = password,
                    onEmailChange = onEmailChange,
                    onPasswordChange = onPasswordChange,
                    onSignInSuccess = onSignInSuccess,
                    onForgotPasswordClick = onForgotPasswordClick,
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
}

@Composable
private fun LoginBrandHeader(brandName: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = brandName,
            modifier = Modifier.size(88.dp).clip(MaterialTheme.shapes.medium),
        )
        Text(
            text = brandTitle(brandName),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = LocalContentColor.current,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.login_brand_supporting_text),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun brandTitle(brandName: String): AnnotatedString = buildAnnotatedString {
    val suffix = " ERP"
    if (brandName.endsWith(suffix)) {
        append(brandName.removeSuffix(suffix))
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(suffix) }
    } else {
        append(brandName)
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
            welcomeDescription = "Login to continue to your account",
            emailLabel = "Work email",
            passwordLabel = "Password",
            forgotPasswordLabel = "Forgot password?",
            loginLabel = "Login",
        )
    }
}

@Preview(showBackground = true, device = "spec:width=412dp,height=915dp,dpi=420")
@Composable
private fun LoginScreenDarkThemePreview() {
    BrandCraftsTheme(darkTheme = true) {
        LoginScreen(
            email = "",
            password = "",
            onEmailChange = {},
            onPasswordChange = {},
            onSignInSuccess = {},
            onForgotPasswordClick = {},
            brandName = "BrandCrafts ERP",
            welcomeTitle = "Welcome back",
            welcomeDescription = "Login to continue to your account",
            emailLabel = "Work email",
            passwordLabel = "Password",
            forgotPasswordLabel = "Forgot password?",
            loginLabel = "Login",
        )
    }
}
