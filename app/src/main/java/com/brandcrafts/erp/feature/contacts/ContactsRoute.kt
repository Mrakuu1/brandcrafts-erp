package com.brandcrafts.erp.feature.contacts

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.feature.employee.EmployeeManagementUiEffect
import com.brandcrafts.erp.feature.employee.EmployeeManagementUiEvent
import com.brandcrafts.erp.feature.employee.EmployeeManagementViewModel
import com.brandcrafts.erp.ui.LocalCurrentUser

@Composable
fun ContactsRoute(
    initialTab: PeopleTab = PeopleTab.CUSTOMERS,
    onAddCustomerClick: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onEditCustomerClick: (String) -> Unit,
    onEditSupplierClick: (String) -> Unit,
    onAddEmployeeClick: () -> Unit,
    onEditEmployeeClick: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
    employeeViewModel: EmployeeManagementViewModel = hiltViewModel(),
) {
    val contactState by viewModel.state.collectAsStateWithLifecycle()
    val employeeState by employeeViewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = LocalCurrentUser.current as? CurrentUserState.Authenticated
    val effectiveRole = currentUser?.user?.role ?: contactState.role
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val unavailableMessage = stringResource(R.string.feature_coming_later)
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentAddEmployee by rememberUpdatedState(onAddEmployeeClick)
    val currentEditEmployee by rememberUpdatedState(onEditEmployeeClick)

    LaunchedEffect(initialTab, effectiveRole) {
        selectedTab = if (initialTab == PeopleTab.EMPLOYEES && effectiveRole == UserRole.ADMIN) {
            PeopleTab.EMPLOYEES
        } else if (selectedTab == PeopleTab.EMPLOYEES && effectiveRole != UserRole.ADMIN) {
            PeopleTab.CUSTOMERS
        } else {
            selectedTab
        }
    }
    LaunchedEffect(viewModel, unavailableMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ContactsUiEffect.RequestAddCustomer -> onAddCustomerClick()
                ContactsUiEffect.RequestAddSupplier -> onAddSupplierClick()
                is ContactsUiEffect.RequestEditCustomer -> onEditCustomerClick(effect.id)
                is ContactsUiEffect.RequestEditSupplier -> onEditSupplierClick(effect.id)
                ContactsUiEffect.ShowUnavailableFeature -> snackbarHostState.showSnackbar(unavailableMessage)
            }
        }
    }
    LaunchedEffect(employeeViewModel) {
        employeeViewModel.effects.collect { effect ->
            when (effect) {
                EmployeeManagementUiEffect.OpenEmployeeCreate -> currentAddEmployee()
                is EmployeeManagementUiEffect.OpenEmployeeEdit -> currentEditEmployee(effect.uid)
                is EmployeeManagementUiEffect.ShowMessage -> snackbarHostState.showSnackbar(
                    currentResources.getString(effect.messageRes),
                )
                is EmployeeManagementUiEffect.UnauthorizedAccess -> Unit
            }
        }
    }

    PeopleScreen(
        contactState = contactState,
        employeeState = employeeState,
        role = effectiveRole,
        selectedTab = selectedTab,
        onTabSelected = { tab ->
            selectedTab = tab
            when (tab) {
                PeopleTab.CUSTOMERS -> viewModel.onEvent(ContactsUiEvent.TypeSelected(ContactType.CUSTOMER))
                PeopleTab.SUPPLIERS -> viewModel.onEvent(ContactsUiEvent.TypeSelected(ContactType.SUPPLIER))
                PeopleTab.EMPLOYEES -> Unit
            }
        },
        onContactEvent = viewModel::onEvent,
        onEmployeeEvent = employeeViewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}
