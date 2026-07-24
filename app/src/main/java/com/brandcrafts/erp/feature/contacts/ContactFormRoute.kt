package com.brandcrafts.erp.feature.contacts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ContactFormRoute(
    onNavigateBack: () -> Unit,
    onContactSaved: (ContactFormMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactFormViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ContactFormUiEffect.NavigateBack -> onNavigateBack()
                is ContactFormUiEffect.ContactSaved -> onContactSaved(effect.mode)
            }
        }
    }

    ContactFormScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
