package com.brandcrafts.erp.feature.quotation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun QuotationFormRoute(
    onSaved: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onUnauthorized: () -> Unit,
    onEditingBlocked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuotationFormViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is QuotationFormUiEffect.Saved -> onSaved(effect.id)
                QuotationFormUiEffect.NavigateBack -> onNavigateBack()
                QuotationFormUiEffect.EditingBlocked -> onEditingBlocked()
                QuotationFormUiEffect.Unauthorized -> onUnauthorized()
            }
        }
    }

    QuotationFormScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}
