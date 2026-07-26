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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import androidx.core.content.FileProvider
import android.content.Intent
import java.io.File

@Composable
fun PurchaseOrderDetailsRoute(
    onBack: () -> Unit,
    onEditPurchaseOrder: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PurchaseOrderDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnEdit by rememberUpdatedState(onEditPurchaseOrder)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)
    var confirmation by remember { mutableStateOf<PurchaseOrderDetailsConfirmation?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PurchaseOrderDetailsUiEffect.NavigateEdit -> currentOnEdit(effect.id)
                PurchaseOrderDetailsUiEffect.NavigateBack -> currentOnBack()
                is PurchaseOrderDetailsUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.messageRes))
                }
                is PurchaseOrderDetailsUiEffect.PreviewPdf -> {
                    openPurchaseOrderPdf(context, effect.cacheFileName, preview = true) {
                        snackbarHostState.showSnackbar(currentResources.getString(R.string.purchase_order_pdf_preview_error))
                    }
                }
                is PurchaseOrderDetailsUiEffect.SharePdf -> {
                    openPurchaseOrderPdf(context, effect.cacheFileName, preview = false) {
                        snackbarHostState.showSnackbar(currentResources.getString(R.string.purchase_order_pdf_share_error))
                    }
                }
                PurchaseOrderDetailsUiEffect.ConfirmApproval -> {
                    confirmation = PurchaseOrderDetailsConfirmation.Approval
                }
                PurchaseOrderDetailsUiEffect.ConfirmCancellation -> {
                    confirmation = PurchaseOrderDetailsConfirmation.Cancellation
                }
                PurchaseOrderDetailsUiEffect.Unauthorized -> {
                    currentOnUnauthorized(currentResources.getString(R.string.purchase_order_error_unauthorized))
                }
            }
        }
    }

    PurchaseOrderDetailsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    when (confirmation) {
        PurchaseOrderDetailsConfirmation.Approval -> PurchaseOrderDetailsConfirmationDialog(
            title = stringResource(R.string.purchase_order_approval_confirm_title),
            message = stringResource(R.string.purchase_order_approval_confirm_message),
            confirmLabel = stringResource(R.string.purchase_order_approve),
            confirmEnabled = !state.approving && !state.cancelling,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(PurchaseOrderDetailsUiEvent.ApproveConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        PurchaseOrderDetailsConfirmation.Cancellation -> PurchaseOrderDetailsConfirmationDialog(
            title = stringResource(R.string.purchase_order_cancel_confirm_title),
            message = stringResource(R.string.purchase_order_cancel_confirm_message),
            confirmLabel = stringResource(R.string.purchase_order_cancel),
            confirmEnabled = !state.approving && !state.cancelling,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(PurchaseOrderDetailsUiEvent.CancelConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        null -> Unit
    }
}

@Composable
private fun PurchaseOrderDetailsConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
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
                androidx.compose.material3.Text(stringResource(R.string.cancel))
            }
        },
    )
}

private enum class PurchaseOrderDetailsConfirmation { Approval, Cancellation }

private suspend fun openPurchaseOrderPdf(
    context: android.content.Context,
    cacheFileName: String,
    preview: Boolean,
    onFailure: suspend () -> Unit,
) {
    val file = File(File(context.cacheDir, "pdf"), cacheFileName)
    if (file.name != cacheFileName || !file.isFile) {
        onFailure()
        return
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = if (preview) {
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf")
    } else {
        Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
    }.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(if (preview) intent else Intent.createChooser(intent, null))
    } catch (_: Throwable) {
        onFailure()
    }
}
