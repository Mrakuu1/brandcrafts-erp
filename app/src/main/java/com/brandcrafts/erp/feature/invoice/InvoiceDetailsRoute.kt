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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R
import androidx.core.content.FileProvider
import android.content.ClipData
import android.content.Intent
import android.content.ActivityNotFoundException
import java.io.File
import kotlinx.coroutines.CancellationException

@Composable
fun InvoiceDetailsRoute(
    onBack: () -> Unit,
    onEditInvoice: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvoiceDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnEdit by rememberUpdatedState(onEditInvoice)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)
    var confirmation by remember { mutableStateOf<InvoiceDetailsConfirmation?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                InvoiceDetailsUiEffect.NavigateBack -> currentOnBack()
                is InvoiceDetailsUiEffect.NavigateEditDraft -> currentOnEdit(effect.invoiceId)
                InvoiceDetailsUiEffect.ConfirmIssue -> confirmation = InvoiceDetailsConfirmation.Issue
                InvoiceDetailsUiEffect.ConfirmCancellation -> confirmation = InvoiceDetailsConfirmation.Cancel
                is InvoiceDetailsUiEffect.PreviewPdf -> {
                    openInvoicePdf(context, effect.cacheFileName, preview = true) {
                        snackbarHostState.showSnackbar(currentResources.getString(R.string.invoice_pdf_preview_error))
                    }
                }
                is InvoiceDetailsUiEffect.SharePdf -> {
                    openInvoicePdf(context, effect.cacheFileName, preview = false) {
                        snackbarHostState.showSnackbar(currentResources.getString(R.string.invoice_pdf_share_error))
                    }
                }
                is InvoiceDetailsUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.messageRes))
                }
                InvoiceDetailsUiEffect.Unauthorized -> {
                    currentOnUnauthorized(currentResources.getString(R.string.invoice_error_unauthorized))
                }
            }
        }
    }

    InvoiceDetailsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    when (confirmation) {
        InvoiceDetailsConfirmation.Issue -> InvoiceDetailsConfirmationDialog(
            title = stringResource(R.string.invoice_issue_confirmation_title),
            message = stringResource(R.string.invoice_issue_confirmation_message),
            confirmLabel = stringResource(R.string.invoice_issue),
            confirmEnabled = state.operationInProgress == null,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(InvoiceDetailsUiEvent.IssueConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        InvoiceDetailsConfirmation.Cancel -> InvoiceDetailsConfirmationDialog(
            title = stringResource(R.string.invoice_cancel_confirmation_title),
            message = stringResource(R.string.invoice_cancel_confirmation_message),
            confirmLabel = stringResource(R.string.invoice_cancel),
            confirmEnabled = state.operationInProgress == null,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(InvoiceDetailsUiEvent.CancelConfirmed)
            },
            onDismiss = { confirmation = null },
        )
        null -> Unit
    }
}

@Composable
private fun InvoiceDetailsConfirmationDialog(
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

private enum class InvoiceDetailsConfirmation { Issue, Cancel }

private suspend fun openInvoicePdf(
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
    val intent = Intent(if (preview) Intent.ACTION_VIEW else Intent.ACTION_SEND).apply {
        if (preview) {
            setDataAndType(uri, "application/pdf")
        } else {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(cacheFileName, uri)
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(if (preview) intent else Intent.createChooser(intent, null))
    } catch (_: ActivityNotFoundException) {
        onFailure()
    } catch (_: SecurityException) {
        onFailure()
    } catch (_: IllegalArgumentException) {
        onFailure()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Throwable) {
        onFailure()
    }
}
