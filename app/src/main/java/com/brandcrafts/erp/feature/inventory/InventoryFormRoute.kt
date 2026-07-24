package com.brandcrafts.erp.feature.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.ui.components.LoadingView

@Composable
fun InventoryFormRoute(
    onNavigateBack: () -> Unit,
    onItemSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryFormViewModel = hiltViewModel(),
) {
    val currentUser = LocalCurrentUser.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (currentUser !is CurrentUserState.Authenticated || currentUser.user.role != UserRole.ADMIN) {
        LaunchedEffect(Unit) { onNavigateBack() }
        LoadingView(modifier = modifier)
        return
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                InventoryFormUiEffect.NavigateBack -> onNavigateBack()
                InventoryFormUiEffect.ItemSaved -> onItemSaved()
            }
        }
    }

    InventoryFormScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
