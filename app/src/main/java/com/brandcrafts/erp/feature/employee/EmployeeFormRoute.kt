package com.brandcrafts.erp.feature.employee

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EmployeeFormRoute(
    onNavigateBack: () -> Unit,
    onEmployeeSaved: (EmployeeFormMode) -> Unit,
    viewModel: EmployeeFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                EmployeeFormUiEffect.NavigateBack -> onNavigateBack()
                is EmployeeFormUiEffect.EmployeeSaved -> onEmployeeSaved(effect.mode)
            }
        }
    }
    EmployeeFormScreen(state, viewModel::onEvent)
}
