package com.brandcrafts.erp.feature.deliverychallan

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
fun DeliveryChallanFormRoute(
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeliveryChallanFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnBack by rememberUpdatedState(onBack)
    val currentOnSaved by rememberUpdatedState(onSaved)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DeliveryChallanFormUiEffect.Saved -> currentOnSaved(effect.id)
                is DeliveryChallanFormUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(currentResources.getString(effect.id))
                }
                DeliveryChallanFormUiEffect.Unauthorized -> {
                    currentOnUnauthorized(
                        currentResources.getString(R.string.delivery_challan_error_unauthorized),
                    )
                }
            }
        }
    }

    DeliveryChallanFormScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onBack = currentOnBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
