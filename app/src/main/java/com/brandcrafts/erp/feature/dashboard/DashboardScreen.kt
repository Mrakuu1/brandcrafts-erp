package com.brandcrafts.erp.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.bottomsheet.BrandBottomSheet
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onViewInventoryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        DashboardUiState.Loading -> LoadingView(
            modifier = modifier,
            message = stringResource(R.string.dashboard_loading),
        )
        is DashboardUiState.Error -> ErrorState(
            title = stringResource(R.string.dashboard_error_title),
            description = stringResource(if (uiState.type == DashboardErrorType.NETWORK) R.string.dashboard_error_network else R.string.dashboard_error_unknown),
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
                onStockInClick = onStockInClick,
                onStockOutClick = onStockOutClick,
                onMaterialUsageClick = onMaterialUsageClick,
                onViewInventoryClick = onViewInventoryClick,
                modifier = modifier,
            )
            UserRole.EMPLOYEE -> EmployeeDashboardContent(
                uiState = uiState,
                onStockInClick = onStockInClick,
                onStockOutClick = onStockOutClick,
                onMaterialUsageClick = onMaterialUsageClick,
                onViewInventoryClick = onViewInventoryClick,
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
    onStockInClick: () -> Unit,
    onStockOutClick: () -> Unit,
    onMaterialUsageClick: () -> Unit,
    onViewInventoryClick: () -> Unit,
    modifier: Modifier,
) {
    var showAllSnapshots by rememberSaveable { mutableStateOf(false) }
    Surface(modifier = modifier.fillMaxSize(), color = dashboardPageColor()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item { DashboardGreeting() }
            item {
                uiState.adminMetrics?.let { metrics ->
                    DashboardSnapshotPreview(metrics, onViewAllClick = { showAllSnapshots = true })
                } ?: DashboardCompactUnavailable()
            }
            item {
                DashboardQuickActionGrid(
                    actions = adminQuickActions(
                        onAddStockClick, onInvoiceClick, onQuotationClick, onEmployeeManagementClick,
                        onStockInClick, onStockOutClick, onMaterialUsageClick,
                    ),
                    editable = true,
                )
            }
            item { DashboardLowStockCard(uiState.lowStockAlerts.take(3), onViewInventoryClick) }
            item { DashboardRecentActivities(uiState.recentActivities.take(5)) }
        }
    }
    if (showAllSnapshots) uiState.adminMetrics?.let { DashboardSnapshotDialog(it) { showAllSnapshots = false } }
}

@Composable
private fun EmployeeDashboardContent(
    uiState: DashboardUiState.Loaded,
    onStockInClick: () -> Unit,
    onStockOutClick: () -> Unit,
    onMaterialUsageClick: () -> Unit,
    onViewInventoryClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = dashboardPageColor()) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item { DashboardGreeting() }
            item {
                DashboardQuickActionGrid(
                    actions = listOf(
                        DashboardQuickActionOption("stock_in", R.string.dashboard_action_stock_in, Icons.Outlined.Inventory2, onStockInClick),
                        DashboardQuickActionOption("stock_out", R.string.dashboard_action_stock_out, Icons.Outlined.WarningAmber, onStockOutClick),
                        DashboardQuickActionOption("material_usage", R.string.dashboard_action_material_usage, Icons.Outlined.Info, onMaterialUsageClick),
                    ),
                    editable = false,
                )
            }
            item { DashboardLowStockCard(uiState.lowStockAlerts.take(3), onViewInventoryClick) }
            item { DashboardRecentActivities(uiState.recentActivities.take(5)) }
        }
    }
}

@Composable
private fun DashboardGreeting() {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = stringResource(R.string.dashboard_greeting, ""),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        Text(
            text = stringResource(R.string.dashboard_business_context),
            style = MaterialTheme.typography.bodySmall,
            color = dashboardSecondaryColor(),
        )
    }
}

@Composable
private fun DashboardSnapshotPreview(metrics: AdminDashboardMetrics, onViewAllClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionHeader(R.string.dashboard_business_snapshot, R.string.dashboard_view_all, onViewAllClick)
        DashboardMetricGrid(snapshotMetrics(metrics).take(4), compact = false)
    }
}

@Composable
private fun DashboardMetricGrid(metrics: List<DashboardMetric>, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)) {
                row.forEach { metric -> DashboardMetricCard(metric, compact, Modifier.weight(1f)) }
                repeat(2 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(metric: DashboardMetric, compact: Boolean, modifier: Modifier = Modifier) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .height(if (compact) 64.dp else 84.dp)
            .then(if (isDashboardDark()) Modifier.border(1.dp, Color(0xFF283646), shape) else Modifier.shadow(3.dp, shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = dashboardCardColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(if (compact) 9.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 28.dp else 30.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(metric.tint.copy(alpha = .13f)),
                contentAlignment = Alignment.Center,
            ) { Icon(metric.icon, null, Modifier.size(17.dp), metric.tint) }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = stringResource(metric.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = dashboardSecondaryColor(),
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metric.value,
                    style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DashboardLowStockCard(alerts: List<LowStockAlert>, onViewAllClick: () -> Unit) {
    DashboardGroupedCard {
        DashboardSectionHeader(R.string.dashboard_low_stock_alerts, R.string.dashboard_view_all, onViewAllClick)
        if (alerts.isEmpty()) {
            Text(stringResource(R.string.dashboard_no_low_stock_alerts_description), style = MaterialTheme.typography.bodySmall, color = dashboardSecondaryColor())
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                alerts.forEach { alert ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).background(Color(0xFFFF6500), androidx.compose.foundation.shape.CircleShape))
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(alert.materialName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.dashboard_low_stock_quantity, alert.availableQuantity, alert.unit), style = MaterialTheme.typography.labelSmall, color = dashboardSecondaryColor())
                        }
                        Text(
                            text = stringResource(R.string.dashboard_low_stock_quantity, alert.availableQuantity, alert.unit),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardRecentActivities(activities: List<DashboardActivity>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DashboardSectionHeader(R.string.dashboard_recent_activities)
        DashboardGroupedCard {
            if (activities.isEmpty()) {
                Text(stringResource(R.string.dashboard_no_recent_activity_description), style = MaterialTheme.typography.bodySmall, color = dashboardSecondaryColor())
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activities.forEach { activity ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val tint = activity.status.tint()
                            Box(
                                modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(9.dp)).background(tint.copy(alpha = .14f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Outlined.Info, null, Modifier.size(18.dp), tint) }
                            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(stringResource(activity.titleRes), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(activity.description, style = MaterialTheme.typography.labelSmall, color = dashboardSecondaryColor(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(activity.status.labelRes()), style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
                                Text(activity.timestamp, style = MaterialTheme.typography.labelSmall, color = dashboardSecondaryColor(), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardGroupedCard(content: @Composable ((ColumnScope) -> Unit)) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = dashboardCardColor()),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDashboardDark()) 0.dp else 3.dp),
        modifier = Modifier.fillMaxWidth().then(if (isDashboardDark()) Modifier.border(1.dp, Color(0xFF283646), shape) else Modifier),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun DashboardCompactUnavailable() = DashboardGroupedCard {
    Text(stringResource(R.string.dashboard_metrics_unavailable_description), style = MaterialTheme.typography.bodySmall, color = dashboardSecondaryColor())
}

@Composable
private fun DashboardSectionHeader(
    titleRes: Int,
    actionRes: Int? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        if (actionRes != null && onActionClick != null) {
            Text(
                text = stringResource(actionRes),
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .clickable(onClick = onActionClick)
                    .padding(4.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun DashboardSnapshotDialog(metrics: AdminDashboardMetrics, onDismissRequest: () -> Unit) {
    BrandBottomSheet(
        title = stringResource(R.string.dashboard_business_snapshot),
        onDismissRequest = onDismissRequest,
        containerColor = if (isDashboardDark()) Color(0xFF111A25) else Color.White,
        header = {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.dashboard_business_snapshot),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = SimpleDateFormat("MMMM-yyyy", Locale.US).format(Date()),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.close))
                }
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.padding(top = 12.dp).heightIn(max = 620.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { DashboardMetricGrid(snapshotMetrics(metrics), compact = true) }
        }
    }
}

private data class DashboardMetric(val labelRes: Int, val value: String, val icon: ImageVector, val tint: Color)

private fun snapshotMetrics(metrics: AdminDashboardMetrics): List<DashboardMetric> = listOfNotNull(
    metrics.totalSales?.let { DashboardMetric(R.string.dashboard_total_sales, it.asCurrencyText(), Icons.Outlined.TrendingUp, Color(0xFFFF6500)) },
    metrics.outstandingPayments?.let { DashboardMetric(R.string.dashboard_outstanding_payments, it.asCurrencyText(), Icons.Outlined.AccountBalanceWallet, Color(0xFFFF6500)) },
    metrics.overduePayments?.let { DashboardMetric(R.string.dashboard_overdue_payments, it.asCurrencyText(), Icons.Outlined.WarningAmber, Color(0xFFFF6500)) },
    metrics.lowStockCount?.let { DashboardMetric(R.string.dashboard_low_stock_count, it.toString(), Icons.Outlined.Inventory2, Color(0xFFFF6500)) },
    metrics.invoiceCount?.let { DashboardMetric(R.string.dashboard_invoice_count, it.toString(), Icons.Outlined.ReceiptLong, Color(0xFFFF6500)) },
    metrics.employeeCount?.let { DashboardMetric(R.string.dashboard_employee_count, it.toString(), Icons.Outlined.Groups, Color(0xFFFF6500)) },
    metrics.customerCount?.let { DashboardMetric(R.string.dashboard_customer_count, it.toString(), Icons.Outlined.Groups, Color(0xFFFF6500)) },
    metrics.quotationCount?.let { DashboardMetric(R.string.dashboard_quotation_count, it.toString(), Icons.Outlined.RequestQuote, Color(0xFFFF6500)) },
    metrics.purchaseOrderCount?.let { DashboardMetric(R.string.dashboard_purchase_order_count, it.toString(), Icons.Outlined.Payments, Color(0xFFFF6500)) },
    metrics.deliveryChallanCount?.let { DashboardMetric(R.string.dashboard_delivery_challan_count, it.toString(), Icons.Outlined.Inventory2, Color(0xFFFF6500)) },
    metrics.draftQuotationCount?.let { DashboardMetric(R.string.dashboard_draft_quotation_count, it.toString(), Icons.Outlined.RequestQuote, Color(0xFFFF6500)) },
    metrics.issuedInvoiceCount?.let { DashboardMetric(R.string.dashboard_issued_invoice_count, it.toString(), Icons.Outlined.ReceiptLong, Color(0xFFFF6500)) },
    metrics.approvedPurchaseOrderCount?.let { DashboardMetric(R.string.dashboard_approved_purchase_order_count, it.toString(), Icons.Outlined.Payments, Color(0xFFFF6500)) },
    metrics.dispatchedDeliveryChallanCount?.let { DashboardMetric(R.string.dashboard_dispatched_delivery_challan_count, it.toString(), Icons.Outlined.Inventory2, Color(0xFFFF6500)) },
)

private fun adminQuickActions(
    onAddStockClick: () -> Unit,
    onInvoiceClick: () -> Unit,
    onQuotationClick: () -> Unit,
    onEmployeeManagementClick: () -> Unit,
    onStockInClick: () -> Unit,
    onStockOutClick: () -> Unit,
    onMaterialUsageClick: () -> Unit,
) = listOf(
    DashboardQuickActionOption("add_stock", R.string.dashboard_action_add_stock, Icons.Outlined.Inventory2, onAddStockClick),
    DashboardQuickActionOption("create_invoice", R.string.dashboard_action_create_invoice, Icons.Outlined.ReceiptLong, onInvoiceClick),
    DashboardQuickActionOption("create_quotation", R.string.dashboard_action_create_quotation, Icons.Outlined.RequestQuote, onQuotationClick),
    DashboardQuickActionOption("employees", R.string.dashboard_action_employee_management, Icons.Outlined.Groups, onEmployeeManagementClick),
    DashboardQuickActionOption("stock_in", R.string.dashboard_action_stock_in, Icons.Outlined.Inventory2, onStockInClick),
    DashboardQuickActionOption("stock_out", R.string.dashboard_action_stock_out, Icons.Outlined.WarningAmber, onStockOutClick),
    DashboardQuickActionOption("material_usage", R.string.dashboard_action_material_usage, Icons.Outlined.Info, onMaterialUsageClick),
)

@Composable
private fun dashboardPageColor(): Color = if (isDashboardDark()) Color(0xFF070D14) else Color(0xFFFFFCFA)

@Composable
private fun dashboardCardColor(): Color = if (isDashboardDark()) Color(0xFF16212E) else Color.White

@Composable
private fun dashboardSecondaryColor(): Color = if (isDashboardDark()) Color(0xFFB2BBC6) else Color(0xFF6B6B6B)

@Composable
private fun isDashboardDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

private fun DashboardCurrencyAmount.asCurrencyText(): String = formatIndianCurrency(amount)

private fun DashboardActivityStatus.labelRes(): Int = when (this) {
    DashboardActivityStatus.PENDING -> R.string.dashboard_status_pending
    DashboardActivityStatus.COMPLETED -> R.string.dashboard_status_completed
    DashboardActivityStatus.INFO -> R.string.dashboard_status_info
}

private fun DashboardActivityStatus.tint(): Color = when (this) {
    DashboardActivityStatus.PENDING -> Color(0xFFFF8A00)
    DashboardActivityStatus.COMPLETED -> Color(0xFF20894E)
    DashboardActivityStatus.INFO -> Color(0xFF2E71C7)
}
