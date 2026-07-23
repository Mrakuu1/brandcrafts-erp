package com.brandcrafts.erp.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.components.StatCard
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

@Composable
fun DashboardScreen(
    user: AuthenticatedUser,
    uiState: DashboardUiState,
    onAddStockClick: () -> Unit,
    onInvoiceClick: () -> Unit,
    onQuotationClick: () -> Unit,
    onEmployeeManagementClick: () -> Unit,
    onStockInClick: () -> Unit,
    onStockOutClick: () -> Unit,
    onMaterialUsageClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        DashboardUiState.Loading -> LoadingView(
            message = stringResource(R.string.dashboard_loading),
            modifier = modifier,
        )
        is DashboardUiState.Error -> ErrorState(
            title = stringResource(R.string.dashboard_error_title),
            description = stringResource(
                if (uiState.type == DashboardErrorType.NETWORK) {
                    R.string.dashboard_error_network
                } else {
                    R.string.dashboard_error_unknown
                },
            ),
            retryLabel = stringResource(R.string.retry),
            onRetry = onRetryClick,
            modifier = modifier,
        )
        is DashboardUiState.Loaded -> when (user.role) {
            UserRole.ADMIN -> AdminDashboardContent(
                uiState = uiState,
                onAddStockClick = onAddStockClick,
                onInvoiceClick = onInvoiceClick,
                onQuotationClick = onQuotationClick,
                onEmployeeManagementClick = onEmployeeManagementClick,
                modifier = modifier,
            )
            UserRole.EMPLOYEE -> EmployeeDashboardContent(
                uiState = uiState,
                onStockInClick = onStockInClick,
                onStockOutClick = onStockOutClick,
                onMaterialUsageClick = onMaterialUsageClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun AdminDashboardContent(
    uiState: DashboardUiState.Loaded,
    onAddStockClick: () -> Unit,
    onInvoiceClick: () -> Unit,
    onQuotationClick: () -> Unit,
    onEmployeeManagementClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionHeader(title = stringResource(R.string.dashboard_business_snapshot)) }
        item {
            val metrics = uiState.adminMetrics
            if (metrics == null) {
                DashboardUnavailableSection(
                    title = stringResource(R.string.dashboard_metrics_unavailable_title),
                    description = stringResource(R.string.dashboard_metrics_unavailable_description),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(
                            title = stringResource(R.string.dashboard_total_sales),
                            value = metrics.totalSales.asCurrencyText(),
                            modifier = Modifier.weight(1f),
                        )
                        StatCard(
                            title = stringResource(R.string.dashboard_outstanding_payments),
                            value = metrics.outstandingPayments.asCurrencyText(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    StatCard(
                        title = stringResource(R.string.dashboard_low_stock_count),
                        value = metrics.lowStockCount.toString(),
                    )
                }
            }
        }
        item { SectionHeader(title = stringResource(R.string.dashboard_quick_actions)) }
        item {
            DashboardActionList(
                actions = listOf(
                    DashboardAction(stringResource(R.string.dashboard_action_add_stock), onAddStockClick),
                    DashboardAction(stringResource(R.string.dashboard_action_create_invoice), onInvoiceClick),
                    DashboardAction(stringResource(R.string.dashboard_action_create_quotation), onQuotationClick),
                    DashboardAction(stringResource(R.string.dashboard_action_employee_management), onEmployeeManagementClick),
                ),
            )
        }
        item { SectionHeader(title = stringResource(R.string.dashboard_low_stock_alerts)) }
        item { LowStockAlertList(alerts = uiState.lowStockAlerts) }
        item { SectionHeader(title = stringResource(R.string.dashboard_recent_activities)) }
        item { DashboardActivityList(activities = uiState.recentActivities) }
    }
}

@Composable
private fun EmployeeDashboardContent(
    uiState: DashboardUiState.Loaded,
    onStockInClick: () -> Unit,
    onStockOutClick: () -> Unit,
    onMaterialUsageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionHeader(title = stringResource(R.string.dashboard_operational_overview)) }
        item {
            val metrics = uiState.employeeMetrics
            if (metrics == null) {
                DashboardUnavailableSection(
                    title = stringResource(R.string.dashboard_metrics_unavailable_title),
                    description = stringResource(R.string.dashboard_metrics_unavailable_description),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        title = stringResource(R.string.dashboard_assigned_tasks),
                        value = metrics.assignedTaskCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                    StatCard(
                        title = stringResource(R.string.dashboard_low_stock_alerts),
                        value = metrics.lowStockAlertCount.toString(),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item { SectionHeader(title = stringResource(R.string.dashboard_quick_actions)) }
        item {
            DashboardActionList(
                actions = listOf(
                    DashboardAction(stringResource(R.string.dashboard_action_stock_in), onStockInClick),
                    DashboardAction(stringResource(R.string.dashboard_action_stock_out), onStockOutClick),
                    DashboardAction(stringResource(R.string.dashboard_action_material_usage), onMaterialUsageClick),
                ),
            )
        }
        item { SectionHeader(title = stringResource(R.string.dashboard_assigned_tasks)) }
        item { DashboardTaskList(tasks = uiState.assignedTasks) }
        item { SectionHeader(title = stringResource(R.string.dashboard_low_stock_alerts)) }
        item { LowStockAlertList(alerts = uiState.lowStockAlerts) }
        item { SectionHeader(title = stringResource(R.string.dashboard_recent_activities)) }
        item { DashboardActivityList(activities = uiState.recentActivities) }
    }
}

@Composable
private fun DashboardActionList(actions: List<DashboardAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.forEach { action -> DashboardQuickAction(label = action.label, onClick = action.onClick) }
    }
}

@Composable
private fun DashboardUnavailableSection(title: String, description: String) {
    EmptyState(title = title, description = description)
}

@Composable
private fun DashboardActivityList(activities: List<DashboardActivity>) {
    if (activities.isEmpty()) {
        DashboardUnavailableSection(
            title = stringResource(R.string.dashboard_no_recent_activity_title),
            description = stringResource(R.string.dashboard_no_recent_activity_description),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            activities.forEach { activity ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    ListItem(
                        headlineContent = { Text(activity.title) },
                        supportingContent = { Text(activity.description) },
                        trailingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusChip(
                                    label = stringResource(activity.status.labelRes()),
                                    tone = activity.status.tone(),
                                )
                                Text(activity.timestamp, style = MaterialTheme.typography.labelSmall)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardTaskList(tasks: List<DashboardTask>) {
    if (tasks.isEmpty()) {
        DashboardUnavailableSection(
            title = stringResource(R.string.dashboard_no_assigned_tasks_title),
            description = stringResource(R.string.dashboard_no_assigned_tasks_description),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tasks.forEach { task ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    ListItem(
                        headlineContent = { Text(task.title) },
                        supportingContent = { Text(task.description) },
                        trailingContent = {
                            StatusChip(
                                label = stringResource(task.status.labelRes()),
                                tone = task.status.tone(),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LowStockAlertList(alerts: List<LowStockAlert>) {
    if (alerts.isEmpty()) {
        DashboardUnavailableSection(
            title = stringResource(R.string.dashboard_no_low_stock_alerts_title),
            description = stringResource(R.string.dashboard_no_low_stock_alerts_description),
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            alerts.forEach { alert ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    ListItem(
                        headlineContent = { Text(alert.materialName) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.dashboard_low_stock_quantity,
                                    alert.availableQuantity,
                                    alert.unit,
                                ),
                            )
                        },
                        trailingContent = {
                            StatusChip(
                                label = stringResource(R.string.dashboard_low_stock_status),
                                tone = StatusTone.WARNING,
                            )
                        },
                    )
                }
            }
        }
    }
}

private data class DashboardAction(val label: String, val onClick: () -> Unit)

private fun DashboardCurrencyAmount.asCurrencyText(): String = NumberFormat.getCurrencyInstance().apply {
    currency = Currency.getInstance(currencyCode)
}.format(amount)

private fun DashboardActivityStatus.labelRes(): Int = when (this) {
    DashboardActivityStatus.PENDING -> R.string.dashboard_status_pending
    DashboardActivityStatus.COMPLETED -> R.string.dashboard_status_completed
    DashboardActivityStatus.INFO -> R.string.dashboard_status_info
}

private fun DashboardActivityStatus.tone(): StatusTone = when (this) {
    DashboardActivityStatus.PENDING -> StatusTone.WARNING
    DashboardActivityStatus.COMPLETED -> StatusTone.SUCCESS
    DashboardActivityStatus.INFO -> StatusTone.INFO
}

private fun DashboardTaskStatus.labelRes(): Int = when (this) {
    DashboardTaskStatus.PENDING -> R.string.dashboard_status_pending
    DashboardTaskStatus.IN_PROGRESS -> R.string.dashboard_status_in_progress
    DashboardTaskStatus.COMPLETED -> R.string.dashboard_status_completed
}

private fun DashboardTaskStatus.tone(): StatusTone = when (this) {
    DashboardTaskStatus.PENDING -> StatusTone.WARNING
    DashboardTaskStatus.IN_PROGRESS -> StatusTone.INFO
    DashboardTaskStatus.COMPLETED -> StatusTone.SUCCESS
}

@Preview(showBackground = true)
@Composable
private fun AdminDashboardPreview() {
    BrandCraftsTheme {
        DashboardScreen(
            user = previewAdminUser(),
            uiState = previewLoadedState(),
            onAddStockClick = {}, onInvoiceClick = {}, onQuotationClick = {}, onEmployeeManagementClick = {},
            onStockInClick = {}, onStockOutClick = {}, onMaterialUsageClick = {}, onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmployeeDashboardPreview() {
    BrandCraftsTheme {
        DashboardScreen(
            user = previewEmployeeUser(),
            uiState = previewLoadedState(),
            onAddStockClick = {}, onInvoiceClick = {}, onQuotationClick = {}, onEmployeeManagementClick = {},
            onStockInClick = {}, onStockOutClick = {}, onMaterialUsageClick = {}, onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AdminDashboardDarkPreview() {
    BrandCraftsTheme(darkTheme = true) {
        DashboardScreen(
            user = previewAdminUser(),
            uiState = previewLoadedState(),
            onAddStockClick = {}, onInvoiceClick = {}, onQuotationClick = {}, onEmployeeManagementClick = {},
            onStockInClick = {}, onStockOutClick = {}, onMaterialUsageClick = {}, onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyDashboardPreview() {
    BrandCraftsTheme {
        DashboardScreen(
            user = previewEmployeeUser(),
            uiState = DashboardUiState.Loaded(),
            onAddStockClick = {}, onInvoiceClick = {}, onQuotationClick = {}, onEmployeeManagementClick = {},
            onStockInClick = {}, onStockOutClick = {}, onMaterialUsageClick = {}, onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingDashboardPreview() {
    BrandCraftsTheme {
        DashboardScreen(
            user = previewAdminUser(),
            uiState = DashboardUiState.Loading,
            onAddStockClick = {}, onInvoiceClick = {}, onQuotationClick = {}, onEmployeeManagementClick = {},
            onStockInClick = {}, onStockOutClick = {}, onMaterialUsageClick = {}, onRetryClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorDashboardPreview() {
    BrandCraftsTheme(darkTheme = true) {
        DashboardScreen(
            user = previewAdminUser(),
            uiState = DashboardUiState.Error(DashboardErrorType.NETWORK),
            onAddStockClick = {}, onInvoiceClick = {}, onQuotationClick = {}, onEmployeeManagementClick = {},
            onStockInClick = {}, onStockOutClick = {}, onMaterialUsageClick = {}, onRetryClick = {},
        )
    }
}

private fun previewLoadedState() = DashboardUiState.Loaded(
    adminMetrics = AdminDashboardMetrics(
        totalSales = DashboardCurrencyAmount(BigDecimal("84500"), "INR"),
        outstandingPayments = DashboardCurrencyAmount(BigDecimal("12000"), "INR"),
        lowStockCount = 3,
    ),
    employeeMetrics = EmployeeDashboardMetrics(assignedTaskCount = 2, lowStockAlertCount = 3),
    recentActivities = listOf(
        DashboardActivity("activity-1", "Invoice created", "INV-0001025", "10 min ago", DashboardActivityStatus.COMPLETED),
    ),
    assignedTasks = listOf(
        DashboardTask("task-1", "Prepare vinyl stock", "Update the material usage record", DashboardTaskStatus.IN_PROGRESS),
    ),
    lowStockAlerts = listOf(LowStockAlert("material-1", "Blue Vinyl", 2.0, "rolls")),
)

private fun previewAdminUser() = AuthenticatedUser(
    uid = "admin-preview", name = "Aarav Mehta", email = "admin@example.com", phone = "",
    role = UserRole.ADMIN, active = true, firstLogin = false, designation = "Administrator", profileImage = "",
    createdAtMillis = null, updatedAtMillis = null, createdBy = "", updatedBy = "",
)

private fun previewEmployeeUser() = previewAdminUser().copy(
    uid = "employee-preview", name = "Riya Shah", email = "employee@example.com", role = UserRole.EMPLOYEE,
)
