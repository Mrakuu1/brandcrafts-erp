package com.brandcrafts.erp.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.feature.auth.LoginRoute
import com.brandcrafts.erp.feature.dashboard.DashboardRoute
import com.brandcrafts.erp.feature.dashboard.DashboardUiState
import com.brandcrafts.erp.ui.CurrentUserProvider
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import kotlinx.coroutines.launch

private const val LOGIN_ROUTE = "login"
private const val MAIN_SHELL_ROUTE = "main_shell"

enum class AppDestination(val route: String, val titleRes: Int) {
    HOME("home", R.string.nav_home), STOCK("stock", R.string.nav_stock),
    ORDERS("orders", R.string.nav_orders), CONTACTS("contacts", R.string.nav_contacts),
    PROFILE("profile", R.string.profile), EMPLOYEES("employees", R.string.employee_management),
    SETTINGS("settings", R.string.business_settings),
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    startupViewModel: StartupViewModel = hiltViewModel(),
) {
    val state by startupViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by startupViewModel.currentUser.collectAsStateWithLifecycle()

    CurrentUserProvider(currentUser = currentUser) {
        when (val startupState = state) {
            StartupUiState.Loading -> StartupLoading(modifier)
            is StartupUiState.RecoverableError -> StartupError(
                error = startupState.error,
                onRetry = { startupViewModel.onEvent(StartupUiEvent.RetryClicked) },
                onSignOut = { startupViewModel.onEvent(StartupUiEvent.SignOutClicked) },
                modifier = modifier,
            )
            StartupUiState.Unauthenticated -> AppSessionNavHost(
                startDestination = LOGIN_ROUTE,
                onSignInSuccess = { startupViewModel.onEvent(StartupUiEvent.LoginSucceeded(it)) },
                onLogout = { startupViewModel.onEvent(StartupUiEvent.SignOutClicked) },
                modifier = modifier,
            )
            StartupUiState.Authenticated -> AppSessionNavHost(
                startDestination = MAIN_SHELL_ROUTE,
                onSignInSuccess = { startupViewModel.onEvent(StartupUiEvent.LoginSucceeded(it)) },
                onLogout = { startupViewModel.onEvent(StartupUiEvent.SignOutClicked) },
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AppSessionNavHost(
    startDestination: String,
    onSignInSuccess: (com.brandcrafts.erp.domain.model.AuthenticatedUser) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val unavailableFeatureMessage = stringResource(R.string.feature_coming_later)
    val onUnavailableFeature = remember(coroutineScope, snackbarHostState, unavailableFeatureMessage) {
        {
            coroutineScope.launch { snackbarHostState.showSnackbar(unavailableFeatureMessage) }
            Unit
        }
    }
    val dashboardUiState = remember { DashboardUiState.Loaded() }
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        if (startDestination == LOGIN_ROUTE) {
            composable(LOGIN_ROUTE) {
                LoginRoute(onSignInSuccess = onSignInSuccess, onForgotPasswordClick = {})
            }
        } else {
            navigation(startDestination = AppDestination.HOME.route, route = MAIN_SHELL_ROUTE) {
                AppDestination.entries
                    .filter { it in setOf(AppDestination.HOME, AppDestination.STOCK, AppDestination.ORDERS, AppDestination.CONTACTS) }
                    .forEach { destination ->
                        composable(destination.route) {
                            AppNavigationShell(
                                navController = navController,
                                onLogout = onLogout,
                                snackbarHostState = snackbarHostState,
                            ) {
                                if (destination == AppDestination.HOME) {
                                    DashboardRoute(
                                        uiState = dashboardUiState,
                                        onAddStockClick = onUnavailableFeature,
                                        onInvoiceClick = onUnavailableFeature,
                                        onQuotationClick = onUnavailableFeature,
                                        onEmployeeManagementClick = onUnavailableFeature,
                                        onStockInClick = onUnavailableFeature,
                                        onStockOutClick = onUnavailableFeature,
                                        onMaterialUsageClick = onUnavailableFeature,
                                        onRetryClick = onUnavailableFeature,
                                    )
                                } else {
                                    PlaceholderScreen(title = stringResource(destination.titleRes))
                                }
                            }
                        }
                    }
            }
        }
    }
}

@Composable
private fun StartupLoading(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingView(message = stringResource(R.string.startup_loading))
    }
}

@Composable
private fun StartupError(
    error: AuthenticationError,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ErrorState(
            title = stringResource(R.string.startup_session_error_title),
            description = stringResource(
                if (error == AuthenticationError.NETWORK_UNAVAILABLE) {
                    R.string.startup_session_network_error
                } else {
                    R.string.startup_session_error
                },
            ),
            retryLabel = stringResource(R.string.retry),
            onRetry = onRetry,
            secondaryActionLabel = stringResource(R.string.logout),
            onSecondaryAction = onSignOut,
        )
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.placeholder_screen_message), style = MaterialTheme.typography.bodyMedium)
    }
}
