package com.brandcrafts.erp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
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

@Composable
fun AppNavigationShell(
    navController: androidx.navigation.NavHostController,
    onLogout: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentUser = LocalCurrentUser.current
    val role = (currentUser as? CurrentUserState.Authenticated)?.user?.role
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route ?: AppDestination.HOME.route
    var profileExpanded by remember { mutableStateOf(false) }
    val destinations = listOf(AppDestination.HOME, AppDestination.STOCK, AppDestination.ORDERS, AppDestination.CONTACTS)
    val items = destinations.map { BottomNavigationItem(it.route, stringResource(it.titleRes)) }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            AppTopBar(title = stringResource(destinations.firstOrNull { it.route == currentRoute }?.titleRes ?: R.string.profile))
            ProfileMenu(
                expanded = profileExpanded, onDismissRequest = { profileExpanded = false }, isAdmin = role == UserRole.ADMIN,
                profileLabel = stringResource(R.string.profile), changePasswordLabel = stringResource(R.string.change_password),
                employeeManagementLabel = stringResource(R.string.employee_management), settingsLabel = stringResource(R.string.business_settings),
                logoutLabel = stringResource(R.string.logout),
                onProfileClick = { profileExpanded = false; navController.navigate(AppDestination.PROFILE.route) },
                onChangePasswordClick = { profileExpanded = false },
                onEmployeeManagementClick = { profileExpanded = false; navController.navigate(AppDestination.EMPLOYEES.route) },
                onSettingsClick = { profileExpanded = false; navController.navigate(AppDestination.SETTINGS.route) },
                onLogoutClick = { profileExpanded = false; onLogout() },
            )
        },
        bottomBar = { BottomNavigationBar(items, currentRoute, onItemSelected = { navController.navigate(it.id) }) },
    ) { padding -> androidx.compose.foundation.layout.Box(Modifier.padding(padding)) { content() } }
}
