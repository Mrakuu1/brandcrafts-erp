package com.brandcrafts.erp.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.ui.components.EmptyState

@Composable
fun DashboardRoute(
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
    when (val currentUser = LocalCurrentUser.current) {
        is CurrentUserState.Authenticated -> DashboardScreen(
            user = currentUser.user,
            uiState = uiState,
            onAddStockClick = onAddStockClick,
            onInvoiceClick = onInvoiceClick,
            onQuotationClick = onQuotationClick,
            onEmployeeManagementClick = onEmployeeManagementClick,
            onStockInClick = onStockInClick,
            onStockOutClick = onStockOutClick,
            onMaterialUsageClick = onMaterialUsageClick,
            onRetryClick = onRetryClick,
            modifier = modifier,
        )
        CurrentUserState.Unauthenticated -> EmptyState(
            title = stringResource(R.string.dashboard_session_unavailable_title),
            description = stringResource(R.string.dashboard_session_unavailable_description),
            modifier = modifier,
        )
    }
}
