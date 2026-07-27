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
import com.brandcrafts.erp.feature.auth.ForgotPasswordRoute
import com.brandcrafts.erp.feature.dashboard.DashboardRoute
import com.brandcrafts.erp.feature.contacts.ContactsRoute
import com.brandcrafts.erp.feature.contacts.ContactFormMode
import com.brandcrafts.erp.feature.contacts.ContactFormRoute
import com.brandcrafts.erp.feature.employee.EmployeeManagementRoute
import com.brandcrafts.erp.feature.quotation.QuotationRoute
import com.brandcrafts.erp.feature.quotation.QuotationFormRoute
import com.brandcrafts.erp.feature.purchaseorder.OrdersRoute
import com.brandcrafts.erp.feature.purchaseorder.PurchaseOrderFormRoute
import com.brandcrafts.erp.feature.purchaseorder.PurchaseOrderDetailsRoute
import com.brandcrafts.erp.feature.invoice.InvoiceDetailsRoute
import com.brandcrafts.erp.feature.invoice.InvoiceFormRoute
import com.brandcrafts.erp.feature.deliverychallan.DeliveryChallanDetailsRoute
import com.brandcrafts.erp.feature.deliverychallan.DeliveryChallanFormRoute
import com.brandcrafts.erp.feature.employee.EmployeeFormMode
import com.brandcrafts.erp.feature.employee.EmployeeFormRoute
import com.brandcrafts.erp.feature.inventory.InventoryRoute
import com.brandcrafts.erp.feature.inventory.InventoryFormRoute
import com.brandcrafts.erp.feature.inventory.StockInRoute
import com.brandcrafts.erp.feature.inventory.StockOutRoute
import com.brandcrafts.erp.ui.CurrentUserProvider
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import kotlinx.coroutines.launch

private const val LOGIN_ROUTE = "login"
private const val FORGOT_PASSWORD_ROUTE = "forgot_password"
private const val MAIN_SHELL_ROUTE = "main_shell"
private const val INVENTORY_CREATE_ROUTE = "inventory/create"
private const val INVENTORY_EDIT_ROUTE = "inventory/edit/{itemId}"
private const val STOCK_IN_ROUTE = "stock/in/{materialId}"
private const val STOCK_OUT_ROUTE = "stock/out/{materialId}"
private const val CONTACT_FORM_ROUTE = "contacts/form/{mode}/{contactId}/{contactType}"
private const val CONTACT_CREATE_CUSTOMER_ROUTE = "contacts/form/create/_/customer"
private const val CONTACT_CREATE_SUPPLIER_ROUTE = "contacts/form/create/_/supplier"
private const val CONTACT_EDIT_ROUTE = "contacts/form/edit/{contactId}/_"
private const val EMPLOYEE_FORM_ROUTE = "employees/form/{mode}/{employeeId}"
private const val EMPLOYEE_CREATE_ROUTE = "employees/form/create/_"
private const val EMPLOYEE_EDIT_ROUTE = "employees/form/edit/{employeeId}"
private const val QUOTATION_CREATE_ROUTE = "quotation/create"
private const val QUOTATION_EDIT_ROUTE = "quotation/edit/{quotationId}"
private const val PURCHASE_ORDER_CREATE_ROUTE = "purchaseorders/create"
private const val PURCHASE_ORDER_EDIT_ROUTE = "purchaseorders/edit/{purchaseOrderId}"
private const val PURCHASE_ORDER_DETAILS_ROUTE = "purchaseorders/details/{purchaseOrderId}"
private const val INVOICE_CREATE_ROUTE = "invoices/create"
private const val INVOICE_EDIT_ROUTE = "invoices/edit/{invoiceId}"
private const val INVOICE_DETAILS_ROUTE = "invoices/details/{invoiceId}"
private const val DELIVERY_CHALLAN_CREATE_ROUTE = "deliverychallans/create"
private const val DELIVERY_CHALLAN_CREATE_FROM_INVOICE_ROUTE = "deliverychallans/create/from-invoice/{invoiceId}"
private const val DELIVERY_CHALLAN_EDIT_ROUTE = "deliverychallans/edit/{challanId}"
private const val DELIVERY_CHALLAN_DETAILS_ROUTE = "deliverychallans/details/{challanId}"

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
    val logoutState by startupViewModel.logoutUiState.collectAsStateWithLifecycle()
    val logoutErrorMessage = logoutState.error?.let { error ->
        stringResource(
            if (error == AuthenticationError.NETWORK_UNAVAILABLE) {
                R.string.logout_network_error
            } else {
                R.string.logout_error
            },
        )
    }

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
                onLogoutConfirmed = { startupViewModel.onEvent(StartupUiEvent.LogoutConfirmed) },
                isLogoutInProgress = logoutState.isLoading,
                logoutErrorMessage = logoutErrorMessage,
                onLogoutErrorShown = { startupViewModel.onEvent(StartupUiEvent.LogoutErrorShown) },
                modifier = modifier,
            )
            StartupUiState.Authenticated -> AppSessionNavHost(
                startDestination = MAIN_SHELL_ROUTE,
                onSignInSuccess = { startupViewModel.onEvent(StartupUiEvent.LoginSucceeded(it)) },
                onLogout = { startupViewModel.onEvent(StartupUiEvent.SignOutClicked) },
                onLogoutConfirmed = { startupViewModel.onEvent(StartupUiEvent.LogoutConfirmed) },
                isLogoutInProgress = logoutState.isLoading,
                logoutErrorMessage = logoutErrorMessage,
                onLogoutErrorShown = { startupViewModel.onEvent(StartupUiEvent.LogoutErrorShown) },
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
    onLogoutConfirmed: () -> Unit,
    isLogoutInProgress: Boolean,
    logoutErrorMessage: String?,
    onLogoutErrorShown: () -> Unit,
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
    val inventorySavedMessage = stringResource(R.string.inventory_form_save_success)
    val stockInSavedMessage = stringResource(R.string.stock_in_success)
    val stockOutSavedMessage = stringResource(R.string.stock_out_success)
    val contactCreatedMessage = stringResource(R.string.contact_form_create_success)
    val contactUpdatedMessage = stringResource(R.string.contact_form_update_success)
    val employeeCreatedMessage = stringResource(R.string.employee_form_create_success)
    val employeeUpdatedMessage = stringResource(R.string.employee_form_update_success)
    val quotationCreatedMessage = stringResource(R.string.quotation_form_create_success)
    val quotationUpdatedMessage = stringResource(R.string.quotation_form_update_success)
    val quotationUnauthorizedMessage = stringResource(R.string.quotation_form_unauthorized_description)
    val quotationBlockedMessage = stringResource(R.string.quotation_form_edit_unavailable_description)
    val onEmployeeManagement = { navController.navigate(AppDestination.EMPLOYEES.route) }
    val openInvoiceDetailsAfterSave: (String) -> Unit = { invoiceId ->
        navController.popBackStack()
        if (navController.currentDestination?.route == INVOICE_DETAILS_ROUTE) {
            navController.popBackStack()
        }
        navController.navigate(invoiceDetailsRoute(invoiceId))
    }
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        if (startDestination == LOGIN_ROUTE) {
            composable(LOGIN_ROUTE) {
                LoginRoute(
                    onSignInSuccess = onSignInSuccess,
                    onForgotPasswordClick = { navController.navigate(FORGOT_PASSWORD_ROUTE) },
                )
            }
            composable(FORGOT_PASSWORD_ROUTE) {
                ForgotPasswordRoute(onNavigateToLogin = { navController.popBackStack() })
            }
        } else {
            navigation(startDestination = AppDestination.HOME.route, route = MAIN_SHELL_ROUTE) {
                AppDestination.entries
                    .filter { it in setOf(AppDestination.HOME, AppDestination.STOCK, AppDestination.ORDERS, AppDestination.CONTACTS) }
                    .forEach { destination ->
                        composable(destination.route) {
                            AppNavigationShell(
                                navController = navController,
                                onLogout = onLogoutConfirmed,
                                isLogoutInProgress = isLogoutInProgress,
                                logoutErrorMessage = logoutErrorMessage,
                                onLogoutErrorShown = onLogoutErrorShown,
                                onUnavailableFeature = onUnavailableFeature,
                                snackbarHostState = snackbarHostState,
                                onEmployeeManagement = onEmployeeManagement,
                            ) {
                                when (destination) {
                                    AppDestination.HOME -> DashboardRoute(
                                        onAddStockClick = { navController.navigate(AppDestination.STOCK.route) },
                                        onInvoiceClick = { navController.navigate(INVOICE_CREATE_ROUTE) },
                                        onQuotationClick = { navController.navigate(QUOTATION_CREATE_ROUTE) },
                                        onEmployeeManagementClick = onEmployeeManagement,
                                        onStockInClick = onUnavailableFeature,
                                        onStockOutClick = onUnavailableFeature,
                                        onMaterialUsageClick = onUnavailableFeature,
                                    )
                                    AppDestination.STOCK -> InventoryRoute(
                                        onItemDetailsClick = { onUnavailableFeature() },
                                        onCreateItemClick = { navController.navigate(INVENTORY_CREATE_ROUTE) },
                                        onEditItemClick = { itemId -> navController.navigate("inventory/edit/$itemId") },
                                        onStockInClick = { itemId -> navController.navigate("stock/in/$itemId") },
                                        onStockOutClick = { itemId -> navController.navigate("stock/out/$itemId") },
                                        onShowMessage = { message -> snackbarHostState.showSnackbar(message) },
                                    )
                                    AppDestination.CONTACTS -> ContactsRoute(
                                        onAddCustomerClick = { navController.navigate(CONTACT_CREATE_CUSTOMER_ROUTE) },
                                        onAddSupplierClick = { navController.navigate(CONTACT_CREATE_SUPPLIER_ROUTE) },
                                        onEditCustomerClick = { id -> navController.navigate(contactEditRoute(id)) },
                                        onEditSupplierClick = { id -> navController.navigate(contactEditRoute(id)) },
                                    )
                                    AppDestination.ORDERS -> OrdersRoute(
                                        onCreateQuotation = { navController.navigate(QUOTATION_CREATE_ROUTE) },
                                        onEditQuotation = { quotationId -> navController.navigate(quotationEditRoute(quotationId)) },
                                        onCreatePurchaseOrder = { navController.navigate(PURCHASE_ORDER_CREATE_ROUTE) },
                                        onOpenPurchaseOrder = { id -> navController.navigate(purchaseOrderDetailsRoute(id)) },
                                        onEditPurchaseOrder = { id -> navController.navigate(purchaseOrderEditRoute(id)) },
                                        onCreateInvoice = { navController.navigate(INVOICE_CREATE_ROUTE) },
                                        onOpenInvoice = { id -> navController.navigate(invoiceDetailsRoute(id)) },
                                        onEditInvoice = { id -> navController.navigate(invoiceEditRoute(id)) },
                                        onCreateDeliveryChallan = { navController.navigate(DELIVERY_CHALLAN_CREATE_ROUTE) },
                                        onCreateDeliveryChallanFromInvoice = onUnavailableFeature,
                                        onOpenDeliveryChallan = { id -> navController.navigate(deliveryChallanDetailsRoute(id)) },
                                        onEditDeliveryChallan = { id -> navController.navigate(deliveryChallanEditRoute(id)) },
                                        onUnauthorized = { message -> coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
                                    )
                                    else -> PlaceholderScreen(title = stringResource(destination.titleRes))
                                }
                            }
                        }
                    }
                composable(INVENTORY_CREATE_ROUTE) {
                    AppNavigationShell(
                        navController = navController,
                        onLogout = onLogoutConfirmed,
                        isLogoutInProgress = isLogoutInProgress,
                        logoutErrorMessage = logoutErrorMessage,
                        onLogoutErrorShown = onLogoutErrorShown,
                        onUnavailableFeature = onUnavailableFeature,
                        snackbarHostState = snackbarHostState,
                    ) {
                        InventoryFormRoute(
                            onNavigateBack = { navController.popBackStack() },
                            onItemSaved = {
                                navController.popBackStack()
                                coroutineScope.launch { snackbarHostState.showSnackbar(inventorySavedMessage) }
                            },
                        )
                    }
                }
                composable(INVENTORY_EDIT_ROUTE) {
                    AppNavigationShell(
                        navController = navController,
                        onLogout = onLogoutConfirmed,
                        isLogoutInProgress = isLogoutInProgress,
                        logoutErrorMessage = logoutErrorMessage,
                        onLogoutErrorShown = onLogoutErrorShown,
                        onUnavailableFeature = onUnavailableFeature,
                        snackbarHostState = snackbarHostState,
                    ) {
                        InventoryFormRoute(
                            onNavigateBack = { navController.popBackStack() },
                            onItemSaved = {
                                navController.popBackStack()
                                coroutineScope.launch { snackbarHostState.showSnackbar(inventorySavedMessage) }
                            },
                        )
                    }
                }
                composable(STOCK_IN_ROUTE) {
                    AppNavigationShell(
                        navController = navController, onLogout = onLogoutConfirmed, isLogoutInProgress = isLogoutInProgress,
                        logoutErrorMessage = logoutErrorMessage, onLogoutErrorShown = onLogoutErrorShown,
                        onUnavailableFeature = onUnavailableFeature, snackbarHostState = snackbarHostState,
                    ) {
                        StockInRoute(
                            onNavigateBack = { navController.popBackStack() },
                            onSaved = {
                                navController.popBackStack()
                                coroutineScope.launch { snackbarHostState.showSnackbar(stockInSavedMessage) }
                            },
                        )
                    }
                }
                composable(STOCK_OUT_ROUTE) { AppNavigationShell(navController,onLogoutConfirmed,isLogoutInProgress,logoutErrorMessage,onLogoutErrorShown,onUnavailableFeature,snackbarHostState) { StockOutRoute(back = { navController.popBackStack() }, saved = { navController.popBackStack(); coroutineScope.launch { snackbarHostState.showSnackbar(stockOutSavedMessage) } }) } }
                composable(AppDestination.EMPLOYEES.route) {
                    AppNavigationShell(
                        navController = navController,
                        onLogout = onLogoutConfirmed,
                        isLogoutInProgress = isLogoutInProgress,
                        logoutErrorMessage = logoutErrorMessage,
                        onLogoutErrorShown = onLogoutErrorShown,
                        onUnavailableFeature = onUnavailableFeature,
                        snackbarHostState = snackbarHostState,
                        onEmployeeManagement = onEmployeeManagement,
                    ) {
                        EmployeeManagementRoute(
                            onUnauthorized = { message ->
                                navController.popBackStack()
                                coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                            },
                            onCreateEmployee = { navController.navigate(EMPLOYEE_CREATE_ROUTE) },
                            onEditEmployee = { uid -> navController.navigate(employeeEditRoute(uid)) },
                        )
                    }
                }
                composable(EMPLOYEE_FORM_ROUTE) {
                    AppNavigationShell(
                        navController = navController,
                        onLogout = onLogoutConfirmed,
                        isLogoutInProgress = isLogoutInProgress,
                        logoutErrorMessage = logoutErrorMessage,
                        onLogoutErrorShown = onLogoutErrorShown,
                        onUnavailableFeature = onUnavailableFeature,
                        snackbarHostState = snackbarHostState,
                        onEmployeeManagement = onEmployeeManagement,
                    ) {
                        EmployeeFormRoute(
                            onNavigateBack = { navController.popBackStack() },
                            onEmployeeSaved = { mode ->
                                navController.popBackStack()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (mode == EmployeeFormMode.CREATE) employeeCreatedMessage else employeeUpdatedMessage,
                                    )
                                }
                            },
                        )
                    }
                }
                composable(CONTACT_FORM_ROUTE) {
                    AppNavigationShell(
                        navController = navController,
                        onLogout = onLogoutConfirmed,
                        isLogoutInProgress = isLogoutInProgress,
                        logoutErrorMessage = logoutErrorMessage,
                        onLogoutErrorShown = onLogoutErrorShown,
                        onUnavailableFeature = onUnavailableFeature,
                        snackbarHostState = snackbarHostState,
                    ) {
                        ContactFormRoute(
                            onNavigateBack = { navController.popBackStack() },
                            onContactSaved = { mode ->
                                navController.popBackStack()
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (mode == ContactFormMode.CREATE) contactCreatedMessage else contactUpdatedMessage,
                                    )
                                }
                            },
                        )
                    }
                }
                composable(QUOTATION_CREATE_ROUTE) {
                    QuotationFormRoute(
                        onSaved = {
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(quotationCreatedMessage) }
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onUnauthorized = {
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(quotationUnauthorizedMessage) }
                        },
                        onEditingBlocked = {
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(quotationBlockedMessage) }
                        },
                    )
                }
                composable(QUOTATION_EDIT_ROUTE) {
                    QuotationFormRoute(
                        onSaved = {
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(quotationUpdatedMessage) }
                        },
                        onNavigateBack = { navController.popBackStack() },
                        onUnauthorized = {
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(quotationUnauthorizedMessage) }
                        },
                        onEditingBlocked = {
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(quotationBlockedMessage) }
                        },
                    )
                }
                composable(PURCHASE_ORDER_CREATE_ROUTE) {
                    PurchaseOrderFormRoute(
                        onBack = { navController.popBackStack() },
                        onPurchaseOrderSaved = { id -> navController.navigate(purchaseOrderDetailsRoute(id)) { popUpTo(PURCHASE_ORDER_CREATE_ROUTE) { inclusive = true } } },
                        onUnauthorized = { message -> navController.popBackStack(); coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
                    )
                }
                composable(PURCHASE_ORDER_EDIT_ROUTE) {
                    PurchaseOrderFormRoute(
                        onBack = { navController.popBackStack() },
                        onPurchaseOrderSaved = { id -> navController.navigate(purchaseOrderDetailsRoute(id)) { popUpTo(PURCHASE_ORDER_EDIT_ROUTE) { inclusive = true } } },
                        onUnauthorized = { message -> navController.popBackStack(); coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
                    )
                }
                composable(PURCHASE_ORDER_DETAILS_ROUTE) {
                    PurchaseOrderDetailsRoute(
                        onBack = { navController.popBackStack() },
                        onEditPurchaseOrder = { id -> navController.navigate(purchaseOrderEditRoute(id)) },
                        onUnauthorized = { message -> navController.popBackStack(); coroutineScope.launch { snackbarHostState.showSnackbar(message) } },
                    )
                }
                composable(INVOICE_CREATE_ROUTE) {
                    InvoiceFormRoute(
                        onBack = { navController.popBackStack() },
                        onInvoiceSaved = openInvoiceDetailsAfterSave,
                        onUnauthorized = { message ->
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
                composable(INVOICE_EDIT_ROUTE) {
                    InvoiceFormRoute(
                        onBack = { navController.popBackStack() },
                        onInvoiceSaved = openInvoiceDetailsAfterSave,
                        onUnauthorized = { message ->
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
                composable(INVOICE_DETAILS_ROUTE) {
                    InvoiceDetailsRoute(
                        onBack = { navController.popBackStack() },
                        onEditInvoice = { id -> navController.navigate(invoiceEditRoute(id)) },
                        onUnauthorized = { message ->
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
                composable(DELIVERY_CHALLAN_CREATE_ROUTE) {
                    DeliveryChallanFormRoute(
                        onBack = { navController.popBackStack() },
                        onSaved = { id ->
                            navController.navigate(deliveryChallanDetailsRoute(id)) {
                                popUpTo(DELIVERY_CHALLAN_CREATE_ROUTE) { inclusive = true }
                            }
                        },
                        onUnauthorized = { message ->
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
                composable(DELIVERY_CHALLAN_CREATE_FROM_INVOICE_ROUTE) {
                    DeliveryChallanFormRoute(
                        onBack = { navController.popBackStack() },
                        onSaved = { id ->
                            navController.navigate(deliveryChallanDetailsRoute(id)) {
                                popUpTo(DELIVERY_CHALLAN_CREATE_FROM_INVOICE_ROUTE) { inclusive = true }
                            }
                        },
                        onUnauthorized = { message ->
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
                composable(DELIVERY_CHALLAN_EDIT_ROUTE) {
                    DeliveryChallanFormRoute(
                        onBack = { navController.popBackStack() },
                        onSaved = { id ->
                            navController.navigate(deliveryChallanDetailsRoute(id)) {
                                popUpTo(DELIVERY_CHALLAN_EDIT_ROUTE) { inclusive = true }
                            }
                        },
                        onUnauthorized = { message ->
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
                composable(DELIVERY_CHALLAN_DETAILS_ROUTE) {
                    DeliveryChallanDetailsRoute(
                        onBack = { navController.popBackStack() },
                        onEditDraft = { id -> navController.navigate(deliveryChallanEditRoute(id)) },
                        onUnauthorized = { message ->
                            navController.popBackStack()
                            coroutineScope.launch { snackbarHostState.showSnackbar(message) }
                        },
                    )
                }
            }
        }
    }
}

private fun contactEditRoute(contactId: String): String =
    CONTACT_EDIT_ROUTE.replace("{contactId}", contactId)

private fun employeeEditRoute(employeeId: String): String =
    EMPLOYEE_EDIT_ROUTE.replace("{employeeId}", employeeId)

private fun quotationEditRoute(quotationId: String): String =
    QUOTATION_EDIT_ROUTE.replace("{quotationId}", quotationId)

private fun purchaseOrderEditRoute(purchaseOrderId: String): String = PURCHASE_ORDER_EDIT_ROUTE.replace("{purchaseOrderId}", purchaseOrderId)
private fun purchaseOrderDetailsRoute(purchaseOrderId: String): String = PURCHASE_ORDER_DETAILS_ROUTE.replace("{purchaseOrderId}", purchaseOrderId)
private fun invoiceEditRoute(invoiceId: String): String = INVOICE_EDIT_ROUTE.replace("{invoiceId}", invoiceId)
private fun invoiceDetailsRoute(invoiceId: String): String = INVOICE_DETAILS_ROUTE.replace("{invoiceId}", invoiceId)
private fun deliveryChallanEditRoute(challanId: String): String =
    DELIVERY_CHALLAN_EDIT_ROUTE.replace("{challanId}", challanId)
private fun deliveryChallanDetailsRoute(challanId: String): String =
    DELIVERY_CHALLAN_DETAILS_ROUTE.replace("{challanId}", challanId)
private fun deliveryChallanCreateFromInvoiceRoute(invoiceId: String): String =
    DELIVERY_CHALLAN_CREATE_FROM_INVOICE_ROUTE.replace("{invoiceId}", invoiceId)

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
