package com.brandcrafts.erp.feature.employee

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun EmployeeManagementRoute(
    onUnauthorized: (String) -> Unit,
    onCreateEmployee: () -> Unit,
    onEditEmployee: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EmployeeManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentResources by rememberUpdatedState(resources)
    val currentOnUnauthorized by rememberUpdatedState(onUnauthorized)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is EmployeeManagementUiEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(
                        message = currentResources.getString(effect.messageRes),
                    )
                }

                is EmployeeManagementUiEffect.UnauthorizedAccess -> {
                    currentOnUnauthorized(
                        currentResources.getString(effect.messageRes),
                    )
                }
                EmployeeManagementUiEffect.OpenEmployeeCreate -> onCreateEmployee()
                is EmployeeManagementUiEffect.OpenEmployeeEdit -> onEditEmployee(effect.uid)
            }
        }
    }


    EmployeeManagementScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}
