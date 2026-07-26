package com.brandcrafts.erp.feature.invoice

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
fun InvoiceFormRoute(
    onBack: () -> Unit,
    onInvoiceSaved: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InvoiceFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnSaved by rememberUpdatedState(onInvoiceSaved)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InvoiceFormUiEffect.Saved -> currentOnSaved(effect.invoiceId)
                InvoiceFormUiEffect.NavigateBack -> currentOnBack()
                InvoiceFormUiEffect.EditingBlocked -> {
                    snackbarHostState.showSnackbar(
                        currentResources.getString(R.string.invoice_error_draft_only_edit),
                    )
                }
                is InvoiceFormUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.messageRes))
                }
                InvoiceFormUiEffect.Unauthorized -> {
                    currentOnUnauthorized(currentResources.getString(R.string.invoice_error_unauthorized))
                }
            }
        }
    }

    InvoiceFormScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = currentOnBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
