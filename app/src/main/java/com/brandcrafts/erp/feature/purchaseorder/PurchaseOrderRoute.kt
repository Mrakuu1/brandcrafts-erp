package com.brandcrafts.erp.feature.purchaseorder

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R

@Composable
fun PurchaseOrderRoute(
    onCreatePurchaseOrder: () -> Unit,
    onOpenPurchaseOrder: (String) -> Unit,
    onEditPurchaseOrder: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)
    var confirmation by remember { mutableStateOf<PurchaseOrderConfirmation?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PurchaseOrderUiEffect.NavigateCreate -> onCreatePurchaseOrder()
                is PurchaseOrderUiEffect.NavigateDetails -> onOpenPurchaseOrder(effect.id)
                is PurchaseOrderUiEffect.NavigateEdit -> onEditPurchaseOrder(effect.id)
                is PurchaseOrderUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.messageRes))
                }
                is PurchaseOrderUiEffect.ConfirmApproval -> {
                    confirmation = PurchaseOrderConfirmation.Approval(effect.id)
                }
                is PurchaseOrderUiEffect.ConfirmCancellation -> {
                    confirmation = PurchaseOrderConfirmation.Cancellation(effect.id)
                }
                PurchaseOrderUiEffect.Unauthorized -> {
                    currentOnUnauthorized(currentResources.getString(R.string.purchase_order_error_unauthorized))
                }
            }
        }
    }

    PurchaseOrderScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    when (confirmation) {
        is PurchaseOrderConfirmation.Approval -> PurchaseOrderConfirmationDialog(
            title = stringResource(R.string.purchase_order_approval_confirm_title),
            message = stringResource(R.string.purchase_order_approval_confirm_message),
            confirmLabel = stringResource(R.string.purchase_order_approve),
            dismissLabel = stringResource(R.string.cancel),
            confirmEnabled = state.approvingId == null && state.cancellingId == null,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(PurchaseOrderUiEvent.ApproveConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        is PurchaseOrderConfirmation.Cancellation -> PurchaseOrderConfirmationDialog(
            title = stringResource(R.string.purchase_order_cancel_confirm_title),
            message = stringResource(R.string.purchase_order_cancel_confirm_message),
            confirmLabel = stringResource(R.string.purchase_order_cancel),
            dismissLabel = stringResource(R.string.cancel),
            confirmEnabled = state.approvingId == null && state.cancellingId == null,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(PurchaseOrderUiEvent.CancelConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        null -> Unit
    }
}

@Composable
private fun PurchaseOrderConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(title) },
        text = { androidx.compose.material3.Text(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                androidx.compose.material3.Text(confirmLabel)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(dismissLabel)
            }
        },
    )
}

private sealed interface PurchaseOrderConfirmation {
    data class Approval(val id: String) : PurchaseOrderConfirmation
    data class Cancellation(val id: String) : PurchaseOrderConfirmation
}
