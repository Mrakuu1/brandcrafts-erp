package com.brandcrafts.erp.feature.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.ui.components.EmptyState

@Composable
fun DashboardRoute(
    onAddStockClick: () -> Unit,
    onInvoiceClick: () -> Unit,
    onQuotationClick: () -> Unit,
    onEmployeeManagementClick: () -> Unit,
    onStockInClick: () -> Unit,
    onStockOutClick: () -> Unit,
    onMaterialUsageClick: () -> Unit,
    onViewInventoryClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
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
            onRetryClick = viewModel::retry,
            onViewInventoryClick = onViewInventoryClick,
            modifier = modifier,
        )
        CurrentUserState.Unauthenticated -> EmptyState(
            title = stringResource(R.string.dashboard_session_unavailable_title),
            description = stringResource(R.string.dashboard_session_unavailable_description),
            modifier = modifier,
        )
    }
}
