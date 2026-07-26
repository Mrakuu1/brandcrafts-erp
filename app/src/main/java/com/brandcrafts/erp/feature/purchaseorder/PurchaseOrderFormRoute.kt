package com.brandcrafts.erp.feature.purchaseorder

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R

@Composable
fun PurchaseOrderFormRoute(
    onBack: () -> Unit,
    onPurchaseOrderSaved: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnSaved by rememberUpdatedState(onPurchaseOrderSaved)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PurchaseOrderFormUiEffect.Saved -> currentOnSaved(effect.id)
                PurchaseOrderFormUiEffect.NavigateBack -> currentOnBack()
                is PurchaseOrderFormUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.messageRes))
                }
                PurchaseOrderFormUiEffect.Unauthorized -> {
                    currentOnUnauthorized(currentResources.getString(R.string.purchase_order_error_unauthorized))
                }
            }
        }
    }

    PurchaseOrderFormScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = currentOnBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
