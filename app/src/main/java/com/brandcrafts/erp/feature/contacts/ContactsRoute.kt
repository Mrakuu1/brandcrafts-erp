package com.brandcrafts.erp.feature.contacts

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.brandcrafts.erp.R

@Composable
fun ContactsRoute(
    onAddCustomerClick: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onEditCustomerClick: (String) -> Unit,
    onEditSupplierClick: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val unavailableMessage = stringResource(R.string.feature_coming_later)

    LaunchedEffect(viewModel, unavailableMessage) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ContactsUiEffect.RequestAddCustomer -> onAddCustomerClick()
                ContactsUiEffect.RequestAddSupplier -> onAddSupplierClick()
                is ContactsUiEffect.RequestEditCustomer -> onEditCustomerClick(effect.id)
                is ContactsUiEffect.RequestEditSupplier -> onEditSupplierClick(effect.id)
                ContactsUiEffect.ShowUnavailableFeature -> snackbarHostState.showSnackbar(unavailableMessage)
            }
        }
    }

    ContactsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        snackbarHostState = snackbarHostState,
    )
}
