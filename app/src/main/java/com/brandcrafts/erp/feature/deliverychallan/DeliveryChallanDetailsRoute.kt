package com.brandcrafts.erp.feature.deliverychallan

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
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
import androidx.core.content.FileProvider
import com.brandcrafts.erp.R
import java.io.File
import kotlinx.coroutines.CancellationException

@Composable
fun DeliveryChallanDetailsRoute(
    onBack: () -> Unit,
    onEditDraft: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeliveryChallanDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnEditDraft by rememberUpdatedState(onEditDraft)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)
    var confirmation by remember { mutableStateOf<DeliveryChallanDetailsConfirmation?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                DeliveryChallanDetailsUiEffect.Back -> currentOnBack()
                is DeliveryChallanDetailsUiEffect.EditDraft -> currentOnEditDraft(effect.id)
                DeliveryChallanDetailsUiEffect.ConfirmDispatch -> {
                    confirmation = DeliveryChallanDetailsConfirmation.Dispatch
                }
                DeliveryChallanDetailsUiEffect.ConfirmCancel -> {
                    confirmation = DeliveryChallanDetailsConfirmation.Cancel
                }
                is DeliveryChallanDetailsUiEffect.PreviewPdf -> {
                    openDeliveryChallanPdf(context, effect.cacheFileName, preview = true) {
                        snackbarHostState.showSnackbar(
                            currentResources.getString(R.string.delivery_challan_pdf_preview_error),
                        )
                    }
                }
                is DeliveryChallanDetailsUiEffect.SharePdf -> {
                    openDeliveryChallanPdf(context, effect.cacheFileName, preview = false) {
                        snackbarHostState.showSnackbar(
                            currentResources.getString(R.string.delivery_challan_pdf_share_error),
                        )
                    }
                }
                is DeliveryChallanDetailsUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.id))
                }
                DeliveryChallanDetailsUiEffect.Unauthorized -> {
                    currentOnUnauthorized(
                        currentResources.getString(R.string.delivery_challan_error_unauthorized),
                    )
                }
            }
        }
    }

    DeliveryChallanDetailsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )

    when (confirmation) {
        DeliveryChallanDetailsConfirmation.Dispatch -> DeliveryChallanDetailsConfirmationDialog(
            title = stringResource(R.string.delivery_challan_dispatch_confirmation_title),
            message = stringResource(R.string.delivery_challan_dispatch_confirmation_message),
            confirmLabel = stringResource(R.string.delivery_challan_dispatch),
            confirmEnabled = !state.isOperating,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(DeliveryChallanDetailsUiEvent.ConfirmDispatch)
            },
            onDismiss = { confirmation = null },
        )
        DeliveryChallanDetailsConfirmation.Cancel -> DeliveryChallanDetailsConfirmationDialog(
            title = stringResource(R.string.delivery_challan_cancel_confirmation_title),
            message = stringResource(R.string.delivery_challan_cancel_confirmation_message),
            confirmLabel = stringResource(R.string.delivery_challan_cancel),
            confirmEnabled = !state.isOperating,
            onConfirm = {
                confirmation = null
                viewModel.onEvent(DeliveryChallanDetailsUiEvent.ConfirmCancel)
            },
            onDismiss = { confirmation = null },
        )
        null -> Unit
    }
}

@Composable
private fun DeliveryChallanDetailsConfirmationDialog(
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

private enum class DeliveryChallanDetailsConfirmation { Dispatch, Cancel }

private suspend fun openDeliveryChallanPdf(
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
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(if (preview) Intent.ACTION_VIEW else Intent.ACTION_SEND).apply {
            if (preview) {
                setDataAndType(uri, "application/pdf")
            } else {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri(cacheFileName, uri)
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
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
