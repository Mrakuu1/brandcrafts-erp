package com.brandcrafts.erp.feature.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun StockInRoute(onNavigateBack: () -> Unit, onSaved: () -> Unit, viewModel: StockInViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.effects.collect { if (it == StockInUiEffect.Saved) onSaved() else onNavigateBack() } }
    StockInScreen(state, viewModel::onEvent)
}
