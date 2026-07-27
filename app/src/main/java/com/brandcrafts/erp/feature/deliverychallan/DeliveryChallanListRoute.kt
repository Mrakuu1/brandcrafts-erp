package com.brandcrafts.erp.feature.deliverychallan

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
fun DeliveryChallanListRoute(
    onCreate: () -> Unit,
    onCreateFromInvoice: () -> Unit,
    onDetails: (String) -> Unit,
    onEdit: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeliveryChallanListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnCreate by rememberUpdatedState(onCreate)
    val currentOnCreateFromInvoice by rememberUpdatedState(onCreateFromInvoice)
    val currentOnDetails by rememberUpdatedState(onDetails)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)
    var confirmation by remember { mutableStateOf<DeliveryChallanListConfirmation?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DeliveryChallanListUiEffect.NavigateCreateIndependent -> currentOnCreate()
                DeliveryChallanListUiEffect.NavigateCreateFromInvoice -> currentOnCreateFromInvoice()
                is DeliveryChallanListUiEffect.NavigateDetails -> currentOnDetails(effect.id)
                is DeliveryChallanListUiEffect.NavigateEditDraft -> currentOnEdit(effect.id)
                is DeliveryChallanListUiEffect.ConfirmDispatch -> {
                    confirmation = DeliveryChallanListConfirmation.Dispatch(effect.id)
                }
                is DeliveryChallanListUiEffect.ConfirmCancellation -> {
                    confirmation = DeliveryChallanListConfirmation.Cancel(effect.id)
                }
                is DeliveryChallanListUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.messageRes))
                }
                DeliveryChallanListUiEffect.Unauthorized -> {
                    currentOnUnauthorized(
                        currentResources.getString(R.string.delivery_challan_error_unauthorized),
                    )
                }
            }
        }
    }

    DeliveryChallanListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    when (val activeConfirmation = confirmation) {
        is DeliveryChallanListConfirmation.Dispatch -> DeliveryChallanConfirmationDialog(
            title = stringResource(R.string.delivery_challan_dispatch_confirmation_title),
            message = stringResource(R.string.delivery_challan_dispatch_confirmation_message),
            confirmLabel = stringResource(R.string.delivery_challan_dispatch),
            confirmEnabled = state.actionInProgress == null,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(DeliveryChallanListUiEvent.DispatchConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        is DeliveryChallanListConfirmation.Cancel -> DeliveryChallanConfirmationDialog(
            title = stringResource(R.string.delivery_challan_cancel_confirmation_title),
            message = stringResource(R.string.delivery_challan_cancel_confirmation_message),
            confirmLabel = stringResource(R.string.delivery_challan_cancel),
            confirmEnabled = state.actionInProgress == null,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(DeliveryChallanListUiEvent.CancelConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        null -> Unit
    }
}

@Composable
private fun DeliveryChallanConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private sealed interface DeliveryChallanListConfirmation {
    data class Dispatch(val challanId: String) : DeliveryChallanListConfirmation
    data class Cancel(val challanId: String) : DeliveryChallanListConfirmation
}
