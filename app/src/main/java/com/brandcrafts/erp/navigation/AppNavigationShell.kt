package com.brandcrafts.erp.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.BottomNavigationBar
import com.brandcrafts.erp.ui.components.BottomNavigationItem
import com.brandcrafts.erp.ui.components.ProfileSideMenu
import com.brandcrafts.erp.ui.components.TopBarAction
import com.brandcrafts.erp.ui.dialogs.ConfirmationDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.brandcrafts.erp.ui.LocalBottomChromeVisible

@Composable
fun AppNavigationShell(
    navController: androidx.navigation.NavHostController,
    onLogout: () -> Unit,
    isLogoutInProgress: Boolean,
    logoutErrorMessage: String?,
    onLogoutErrorShown: () -> Unit,
    onUnavailableFeature: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onEmployeeManagement: () -> Unit = onUnavailableFeature,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val user = (LocalCurrentUser.current as? CurrentUserState.Authenticated)?.user ?: return
    val role = user.role
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route ?: AppDestination.HOME.route
    val selectedRoute = when {
        currentRoute.startsWith("inventory/") || currentRoute.startsWith("stock/") -> AppDestination.STOCK.route
        currentRoute.startsWith("contacts/") || currentRoute.startsWith("employees") -> AppDestination.CONTACTS.route
        currentRoute.startsWith("quotation/") ||
            currentRoute.startsWith("purchaseorders/") ||
            currentRoute.startsWith("invoices/") ||
            currentRoute.startsWith("deliverychallans/") -> AppDestination.ORDERS.route
        else -> currentRoute
    }
    val isDashboard = selectedRoute == AppDestination.HOME.route
    val showsFullScreenDetail = currentRoute.startsWith("quotation/details/") ||
        currentRoute.startsWith("purchaseorders/details/") ||
        currentRoute.startsWith("invoices/details/") ||
        currentRoute.startsWith("deliverychallans/details/")
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    val openSideMenu: () -> Unit = {
        drawerScope.launch { drawerState.open() }
        Unit
    }
    fun dismissSideMenu(afterDismiss: () -> Unit) {
        drawerScope.launch {
            drawerState.close()
            afterDismiss()
        }
    }
    var bottomChromeVisible by remember { mutableStateOf(true) }
    val bottomChromeScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -2f -> bottomChromeVisible = false
                    available.y > 2f -> bottomChromeVisible = true
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(selectedRoute) { bottomChromeVisible = true }
    val destinations = listOf(AppDestination.HOME, AppDestination.STOCK, AppDestination.ORDERS, AppDestination.CONTACTS)
    val items = destinations.map { destination ->
        BottomNavigationItem(
            id = destination.route,
            label = stringResource(destination.titleRes),
            drawableRes = if (destination == AppDestination.CONTACTS) null else destination.bottomNavigationIcon(),
            icon = if (destination == AppDestination.CONTACTS) Icons.Outlined.Groups else null,
            contentDescription = stringResource(destination.titleRes),
        )
    }
    if (showsFullScreenDetail) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF111A25) else Color.White,
                ) {
                    ProfileSideMenu(
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
                        onProfileClick = { dismissSideMenu(onUnavailableFeature) },
                        onChangePasswordClick = { dismissSideMenu(onUnavailableFeature) },
                        onEmployeeManagementClick = { dismissSideMenu(onEmployeeManagement) },
                        onSettingsClick = { dismissSideMenu(onUnavailableFeature) },
                        onLogoutClick = { dismissSideMenu { showLogoutConfirmation = true } },
                    )
                }
            },
        ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(bottomChromeScrollConnection),
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier.padding(bottom = if (bottomChromeVisible) 84.dp else 16.dp),
                    )
                },
                topBar = {
                AppTopBar(
                    title = stringResource(
                        if (isDashboard) {
                            R.string.login_brand_name
                        } else if (selectedRoute == AppDestination.STOCK.route) {
                            R.string.inventory_title
                        } else {
                            destinations.firstOrNull { it.route == selectedRoute }?.titleRes ?: R.string.profile
                        },
                    ),
                    navigationIcon = Icons.Outlined.Menu,
                    navigationContentDescription = stringResource(R.string.dashboard_open_menu),
                    onNavigationClick = openSideMenu,
                    actions = listOf(
                        TopBarAction(
                            icon = Icons.Outlined.NotificationsNone,
                            contentDescription = stringResource(R.string.dashboard_notifications),
                            onClick = onUnavailableFeature,
                        ),
                    ),
                    transparent = isDashboard,
                )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .padding(padding),
                ) {
                    CompositionLocalProvider(LocalBottomChromeVisible provides bottomChromeVisible) {
                        content()
                    }
                }
            }
            AnimatedVisibility(
                visible = bottomChromeVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                BottomNavigationBar(
                    items = items,
                    selectedItemId = selectedRoute,
                    onItemSelected = { item ->
                        if (item.id != selectedRoute) {
                        // The Employee quick action is a People sub-route. Returning home
                        // must remove that sub-route instead of leaving it above Dashboard.
                        val returnedToExistingHome = item.id == AppDestination.HOME.route &&
                            navController.popBackStack(AppDestination.HOME.route, inclusive = false)
                        if (!returnedToExistingHome) {
                            navController.navigate(item.id) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                            }
                        }
                        }
                    },
                )
            }
        }
        }
    }
    if (showLogoutConfirmation) {
        ConfirmationDialog(
            title = stringResource(R.string.logout_confirmation_title),
            description = stringResource(R.string.logout_confirmation_description),
            confirmLabel = stringResource(R.string.logout),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = onLogout,
            onDismiss = { if (!isLogoutInProgress) showLogoutConfirmation = false },
            confirmLoading = isLogoutInProgress,
            destructive = true,
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

private fun AppDestination.bottomNavigationIcon(): Int = when (this) {
    AppDestination.HOME -> R.drawable.home_icon
    AppDestination.STOCK -> R.drawable.inventory_icon
    AppDestination.ORDERS -> R.drawable.orders_icon
    AppDestination.CONTACTS -> R.drawable.contact_icon
    else -> error("Bottom navigation icon requested for non-bottom destination: $this")
}
