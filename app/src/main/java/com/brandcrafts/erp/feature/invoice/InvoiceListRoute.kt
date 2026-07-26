package com.brandcrafts.erp.feature.invoice

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
fun InvoiceListRoute(
    onCreateInvoice: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    onEditInvoice: (String) -> Unit,
    onRecordPayment: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvoiceListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnCreate by rememberUpdatedState(onCreateInvoice)
    val currentOnOpen by rememberUpdatedState(onOpenInvoice)
    val currentOnEdit by rememberUpdatedState(onEditInvoice)
    val currentOnRecordPayment by rememberUpdatedState(onRecordPayment)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)
    var confirmation by remember { mutableStateOf<InvoiceListConfirmation?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                InvoiceListUiEffect.NavigateCreate -> currentOnCreate()
                is InvoiceListUiEffect.NavigateDetails -> currentOnOpen(effect.invoiceId)
                is InvoiceListUiEffect.NavigateEditDraft -> currentOnEdit(effect.invoiceId)
                is InvoiceListUiEffect.NavigateRecordPayment -> currentOnRecordPayment(effect.invoiceId)
                is InvoiceListUiEffect.ConfirmIssue -> confirmation = InvoiceListConfirmation.Issue(effect.invoiceId)
                is InvoiceListUiEffect.ConfirmCancellation -> confirmation = InvoiceListConfirmation.Cancel(effect.invoiceId)
                is InvoiceListUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.messageRes))
                }
                InvoiceListUiEffect.Unauthorized -> {
                    currentOnUnauthorized(currentResources.getString(R.string.invoice_error_unauthorized))
                }
            }
        }
    }

    InvoiceListScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    when (confirmation) {
        is InvoiceListConfirmation.Issue -> InvoiceListConfirmationDialog(
            title = stringResource(R.string.invoice_issue_confirmation_title),
            message = stringResource(R.string.invoice_issue_confirmation_message),
            confirmLabel = stringResource(R.string.invoice_issue),
            onConfirm = {
                confirmation = null
                viewModel.onEvent(InvoiceListUiEvent.IssueConfirmed)
            },
            onDismiss = { confirmation = null },
            confirmEnabled = state.actionInProgress == null,
        )
        is InvoiceListConfirmation.Cancel -> InvoiceListConfirmationDialog(
            title = stringResource(R.string.invoice_cancel_confirmation_title),
            message = stringResource(R.string.invoice_cancel_confirmation_message),
            confirmLabel = stringResource(R.string.invoice_cancel),
            onConfirm = {
                confirmation = null
                viewModel.onEvent(InvoiceListUiEvent.CancelConfirmed)
            },
            onDismiss = { confirmation = null },
            confirmEnabled = state.actionInProgress == null,
        )
        null -> Unit
    }
}

@Composable
private fun InvoiceListConfirmationDialog(
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

private sealed interface InvoiceListConfirmation {
    data class Issue(val invoiceId: String) : InvoiceListConfirmation
    data class Cancel(val invoiceId: String) : InvoiceListConfirmation
}
