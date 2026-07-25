package com.brandcrafts.erp.feature.employee

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.bottomsheet.UniversalFormSheet
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SearchBar
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.dialogs.ConfirmationDialog
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun EmployeeManagementScreen(
    state: EmployeeManagementUiState,
    onEvent: (EmployeeManagementUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onEvent(EmployeeManagementUiEvent.AddEmployeeClicked) }) {
                Text(stringResource(R.string.employee_management_add))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(
                title = stringResource(R.string.employee_management_title),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { onEvent(EmployeeManagementUiEvent.SearchChanged(it)) },
                placeholder = stringResource(R.string.employee_management_search),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            when (state.content) {
                EmployeeManagementUiState.Content.Loading -> LoadingView(
                    message = stringResource(R.string.employee_management_loading),
                )
                EmployeeManagementUiState.Content.Error -> ErrorState(
                    title = stringResource(R.string.employee_management_error),
                    description = stringResource(R.string.employee_management_error_description),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { onEvent(EmployeeManagementUiEvent.RetryClicked) },
                )
                EmployeeManagementUiState.Content.Empty -> EmptyState(
                    title = stringResource(
                        if (state.searchQuery.isBlank()) R.string.employee_management_empty
                        else R.string.employee_management_no_results,
                    ),
                    description = stringResource(R.string.employee_management_empty_description),
                )
                EmployeeManagementUiState.Content.Loaded -> EmployeeList(
                    employees = state.visibleEmployees,
                    updatingEmployeeUid = state.updatingEmployeeUid,
                    onEmployeeClick = { onEvent(EmployeeManagementUiEvent.EmployeeClicked(it)) },
                    onStatusClick = { onEvent(EmployeeManagementUiEvent.StatusChangeRequested(it)) },
                )
            }
        }
    }
    state.selectedEmployee?.let { employee ->
        EmployeeActionsSheet(
            employee = employee,
            selectedRole = state.selectedRole ?: employee.role,
            loading = state.updatingEmployeeUid == employee.uid,
            onRoleSelected = { onEvent(EmployeeManagementUiEvent.RoleSelected(it)) },
            onRoleChange = { onEvent(EmployeeManagementUiEvent.RoleChangeConfirmed) },
            onStatusChange = { onEvent(EmployeeManagementUiEvent.StatusChangeRequested(employee)) },
            onEdit = { onEvent(EmployeeManagementUiEvent.EditEmployeeClicked) },
            onDismiss = { onEvent(EmployeeManagementUiEvent.EmployeeActionsDismissed) },
        )
    }
    state.pendingStatusChange?.let { employee ->
        ConfirmationDialog(
            title = stringResource(
                if (employee.active) R.string.employee_management_deactivate_confirmation_title
                else R.string.employee_management_activate_confirmation_title,
            ),
            description = stringResource(
                if (employee.active) R.string.employee_management_deactivate_confirmation_description
                else R.string.employee_management_activate_confirmation_description,
                employee.name,
            ),
            confirmLabel = stringResource(
                if (employee.active) R.string.employee_management_deactivate
                else R.string.employee_management_activate,
            ),
            dismissLabel = stringResource(R.string.cancel),
            onConfirm = { onEvent(EmployeeManagementUiEvent.StatusChangeConfirmed) },
            onDismiss = { onEvent(EmployeeManagementUiEvent.StatusChangeDismissed) },
            confirmLoading = state.updatingEmployeeUid == employee.uid,
        )
    }
}

@Composable
private fun EmployeeList(
    employees: List<EmployeeListItemUi>,
    updatingEmployeeUid: String?,
    onEmployeeClick: (EmployeeListItemUi) -> Unit,
    onStatusClick: (EmployeeListItemUi) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = employees, key = EmployeeListItemUi::uid) { employee ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onEmployeeClick(employee) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                ListItem(
                    headlineContent = { Text(employee.name) },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (employee.designation.isNotBlank()) Text(employee.designation)
                            Text(employee.email)
                            if (employee.phone.isNotBlank()) Text(employee.phone)
                            employee.createdDate?.let {
                                Text(stringResource(R.string.employee_management_created_date, it))
                            }
                        }
                    },
                    trailingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            StatusChip(label = stringResource(employee.role.labelRes()), tone = StatusTone.INFO)
                            StatusChip(
                                label = stringResource(
                                    if (employee.active) R.string.employee_management_active else R.string.employee_management_inactive,
                                ),
                                tone = if (employee.active) StatusTone.SUCCESS else StatusTone.NEUTRAL,
                            )
                            if (employee.firstLogin) {
                                StatusChip(
                                    label = stringResource(R.string.employee_management_first_login),
                                    tone = StatusTone.WARNING,
                                )
                            }
                            TextButton(
                                onClick = { onStatusClick(employee) },
                                enabled = updatingEmployeeUid == null,
                            ) {
                                Text(
                                    stringResource(
                                        if (employee.active) R.string.employee_management_deactivate
                                        else R.string.employee_management_activate,
                                    ),
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun EmployeeActionsSheet(
    employee: EmployeeListItemUi,
    selectedRole: UserRole,
    loading: Boolean,
    onRoleSelected: (UserRole) -> Unit,
    onRoleChange: () -> Unit,
    onStatusChange: () -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
) {
    UniversalFormSheet(
        title = employee.name,
        primaryActionLabel = stringResource(R.string.employee_management_change_role),
        onPrimaryAction = onRoleChange,
        onDismissRequest = onDismiss,
        primaryActionLoading = loading,
        primaryActionEnabled = selectedRole != employee.role && !loading,
        cancelActionLabel = stringResource(R.string.cancel),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusChip(label = stringResource(employee.role.labelRes()), tone = StatusTone.INFO)
            StatusChip(
                label = stringResource(
                    if (employee.active) R.string.employee_management_active else R.string.employee_management_inactive,
                ),
                tone = if (employee.active) StatusTone.SUCCESS else StatusTone.NEUTRAL,
            )
            Text(
                text = stringResource(selectedRole.effectiveAccessSummaryRes()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.employee_management_role_label),
                style = MaterialTheme.typography.titleSmall,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                UserRole.entries.forEachIndexed { index, role ->
                    SegmentedButton(
                        selected = selectedRole == role,
                        onClick = { onRoleSelected(role) },
                        shape = SegmentedButtonDefaults.itemShape(index, UserRole.entries.size),
                        label = { Text(stringResource(role.labelRes())) },
                    )
                }
            }
            TextButton(onClick = onStatusChange, enabled = !loading) {
                Text(
                    stringResource(
                        if (employee.active) R.string.employee_management_deactivate
                        else R.string.employee_management_activate,
                    ),
                )
            }
            TextButton(onClick = onEdit, enabled = !loading) {
                Text(stringResource(R.string.employee_management_edit))
            }
        }
    }
}

private fun UserRole.labelRes(): Int = when (this) {
    UserRole.ADMIN -> R.string.role_admin
    UserRole.EMPLOYEE -> R.string.role_employee
}

private fun UserRole.effectiveAccessSummaryRes(): Int = when (this) {
    UserRole.ADMIN -> R.string.employee_management_admin_access_summary
    UserRole.EMPLOYEE -> R.string.employee_management_employee_access_summary
}

@Preview(showBackground = true)
@Composable
private fun EmployeeManagementLoadedPreview() {
    BrandCraftsTheme {
        EmployeeManagementScreen(
            state = previewEmployeeState(),
            onEvent = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmployeeManagementLoadingPreview() {
    BrandCraftsTheme {
        EmployeeManagementScreen(
            state = EmployeeManagementUiState(), onEvent = {}, snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmployeeManagementEmptyPreview() {
    BrandCraftsTheme(darkTheme = true) {
        EmployeeManagementScreen(
            state = EmployeeManagementUiState(content = EmployeeManagementUiState.Content.Empty),
            onEvent = {}, snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmployeeManagementErrorPreview() {
    BrandCraftsTheme {
        EmployeeManagementScreen(
            state = EmployeeManagementUiState(content = EmployeeManagementUiState.Content.Error),
            onEvent = {}, snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

private fun previewEmployeeState() = EmployeeManagementUiState(
    content = EmployeeManagementUiState.Content.Loaded,
    allEmployees = listOf(previewEmployee()),
    visibleEmployees = listOf(previewEmployee()),
)

private fun previewEmployee() = EmployeeListItemUi(
    uid = "preview-user", name = "Avery Shah", email = "avery@example.com", phone = "+91 98765 43210",
    role = UserRole.EMPLOYEE, active = true, firstLogin = true, designation = "Designer", createdDate = "24 Jul 2026",
)
