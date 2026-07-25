package com.brandcrafts.erp.feature.quotation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.LocalCurrentUser

@Composable
fun QuotationRoute(
    onCreateQuotation: () -> Unit,
    onEditQuotation: (String) -> Unit,
    viewModel: QuotationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUser = (LocalCurrentUser.current as? CurrentUserState.Authenticated)?.user
    val canManageQuotations = currentUser?.active == true && currentUser.role == UserRole.ADMIN
    QuotationScreen(
        state = state,
        canManageQuotations = canManageQuotations,
        onCreateQuotation = onCreateQuotation,
        onEditQuotation = onEditQuotation,
        onSearch = viewModel::search,
        onStatus = viewModel::status,
        onRetry = viewModel::retry,
    )
}
