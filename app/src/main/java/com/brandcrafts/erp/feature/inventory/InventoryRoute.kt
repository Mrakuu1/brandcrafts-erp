package com.brandcrafts.erp.feature.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.ui.components.EmptyState

@Composable
fun InventoryRoute(
    onItemDetailsClick: (String) -> Unit,
    onCreateItemClick: () -> Unit,
    onEditItemClick: (String) -> Unit,
    onStockInClick: (String) -> Unit,
    onShowMessage: suspend (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser = LocalCurrentUser.current
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InventoryUiEffect.NavigateToItemDetails -> onItemDetailsClick(effect.itemId)
                is InventoryUiEffect.NavigateToEditItem -> onEditItemClick(effect.itemId)
                is InventoryUiEffect.NavigateToStockIn -> onStockInClick(effect.itemId)
                InventoryUiEffect.NavigateToCreateItem -> onCreateItemClick()
                is InventoryUiEffect.ShowMessage -> onShowMessage(context.getString(effect.messageRes))
            }
        }
    }

    when (currentUser) {
        is CurrentUserState.Authenticated -> InventoryScreen(
            uiState = uiState,
            isAdmin = currentUser.user.role == UserRole.ADMIN,
            onEvent = viewModel::onEvent,
            modifier = modifier,
        )
        CurrentUserState.Unauthenticated -> EmptyState(
            title = stringResource(R.string.inventory_session_unavailable_title),
            description = stringResource(R.string.inventory_session_unavailable_description),
            modifier = modifier,
        )
    }
}
