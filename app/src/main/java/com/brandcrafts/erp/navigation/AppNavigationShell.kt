package com.brandcrafts.erp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.BottomNavigationBar
import com.brandcrafts.erp.ui.components.BottomNavigationItem
import com.brandcrafts.erp.ui.components.ProfileMenu
import com.brandcrafts.erp.ui.components.ProfileAvatar
import com.brandcrafts.erp.ui.dialogs.ConfirmationDialog

@Composable
fun AppNavigationShell(
    navController: androidx.navigation.NavHostController,
    onLogout: () -> Unit,
    isLogoutInProgress: Boolean,
    logoutErrorMessage: String?,
    onLogoutErrorShown: () -> Unit,
    onUnavailableFeature: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val user = (LocalCurrentUser.current as? CurrentUserState.Authenticated)?.user ?: return
    val role = user.role
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route ?: AppDestination.HOME.route
    val selectedRoute = when {
        currentRoute.startsWith("inventory/") || currentRoute.startsWith("stock/") -> AppDestination.STOCK.route
        currentRoute.startsWith("contacts/") -> AppDestination.CONTACTS.route
        else -> currentRoute
    }
    var profileExpanded by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    val destinations = listOf(AppDestination.HOME, AppDestination.STOCK, AppDestination.ORDERS, AppDestination.CONTACTS)
    val items = destinations.map { BottomNavigationItem(it.route, stringResource(it.titleRes)) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppTopBar(
                title = stringResource(
                    if (selectedRoute == AppDestination.STOCK.route) {
                        R.string.inventory_title
                    } else {
                        destinations.firstOrNull { it.route == selectedRoute }?.titleRes ?: R.string.profile
                    },
                ),
                trailingContent = {
                    Box {
                        IconButton(onClick = { profileExpanded = true }) {
                            ProfileAvatar(
                                initials = user.name.initials(),
                                contentDescription = stringResource(R.string.profile_avatar_content_description, user.name),
                            )
                        }
                        ProfileMenu(
                            expanded = profileExpanded,
                            onDismissRequest = { profileExpanded = false },
                            isAdmin = role == UserRole.ADMIN,
                            displayName = user.name,
                            email = user.email,
                            roleLabel = stringResource(role.labelRes()),
                            avatarContentDescription = stringResource(R.string.profile_avatar_content_description, user.name),
                            profileLabel = stringResource(R.string.profile),
                            changePasswordLabel = stringResource(R.string.change_password),
                            employeeManagementLabel = stringResource(R.string.employee_management),
                            settingsLabel = stringResource(R.string.business_settings),
                            logoutLabel = stringResource(R.string.logout),
                            onProfileClick = { profileExpanded = false; onUnavailableFeature() },
                            onChangePasswordClick = { profileExpanded = false; onUnavailableFeature() },
                            onEmployeeManagementClick = { profileExpanded = false; onUnavailableFeature() },
                            onSettingsClick = { profileExpanded = false; onUnavailableFeature() },
                            onLogoutClick = { profileExpanded = false; showLogoutConfirmation = true },
                        )
                    }
                },
            )
        },
        bottomBar = { BottomNavigationBar(items, selectedRoute, onItemSelected = { navController.navigate(it.id) }) },
    ) { padding -> androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { content() } }
    if (showLogoutConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.logout_confirmation_title),
            description = stringResource(R.string.logout_confirmation_description),
            confirmLabel = stringResource(R.string.logout),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = onLogout,
            onDismiss = { if (!isLogoutInProgress) showLogoutConfirmation = false },
            confirmLoading = isLogoutInProgress,
        )
    }
    LaunchedEffect(logoutErrorMessage) {
        if (logoutErrorMessage != null) {
            snackbarHostState.showSnackbar(logoutErrorMessage)
            onLogoutErrorShown()
        }
    }
}

private fun String.initials(): String = trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .take(2)
    .joinToString(separator = "") { it.first().uppercase() }

private fun UserRole.labelRes(): Int = when (this) {
    UserRole.ADMIN -> R.string.role_admin
    UserRole.EMPLOYEE -> R.string.role_employee
}
