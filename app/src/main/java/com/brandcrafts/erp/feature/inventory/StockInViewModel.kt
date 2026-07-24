package com.brandcrafts.erp.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.core.result.StockError
import com.brandcrafts.erp.core.result.StockResult
import com.brandcrafts.erp.domain.model.StockInInput
import com.brandcrafts.erp.domain.usecase.GetInventoryItemUseCase
import com.brandcrafts.erp.domain.usecase.StockInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StockInViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getInventoryItem: GetInventoryItemUseCase,
    private val stockIn: StockInUseCase,
) : ViewModel() {
    private val materialId: String = checkNotNull(savedStateHandle["materialId"])
    private val _uiState = MutableStateFlow(StockInUiState(materialId = materialId))
    val uiState: StateFlow<StockInUiState> = _uiState.asStateFlow()
    private val _effects = Channel<StockInUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init { loadMaterial() }

    fun onEvent(event: StockInUiEvent) = when (event) {
        is StockInUiEvent.QuantityChanged -> update { copy(quantity = event.value, quantityError = null, errorMessage = null) }
        is StockInUiEvent.ReferenceChanged -> update { copy(referenceId = event.value, errorMessage = null) }
        is StockInUiEvent.RemarksChanged -> update { copy(remarks = event.value, errorMessage = null) }
        StockInUiEvent.SaveClicked -> save()
        StockInUiEvent.CancelClicked -> effect(StockInUiEffect.NavigateBack)
        StockInUiEvent.RetryClicked -> loadMaterial()
    }

    private fun loadMaterial() {
        update { copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = getInventoryItem(materialId)) {
                is InventoryResult.Success -> {
                    if (!result.data.active) update { copy(isLoading = false, errorMessage = R.string.stock_in_error_inactive) }
                    else update { copy(materialName = result.data.name, unit = result.data.unit, isLoading = false) }
                }
                is InventoryResult.Error -> update { copy(isLoading = false, errorMessage = R.string.stock_in_error_material) }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val quantity = state.quantity.toDoubleOrNull()
        if (quantity == null || quantity <= 0) { update { copy(quantityError = R.string.stock_in_quantity_invalid) }; return }
        update { copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            when (val result = stockIn(StockInInput(materialId, quantity, state.referenceId, state.remarks))) {
                is StockResult.Success -> effect(StockInUiEffect.Saved)
                is StockResult.Error -> update { copy(isSaving = false, errorMessage = result.error.messageRes()) }
            }
        }
    }

    private fun update(block: StockInUiState.() -> StockInUiState) { _uiState.value = _uiState.value.block() }
    private fun effect(value: StockInUiEffect) { viewModelScope.launch { _effects.send(value) } }
}

private fun StockError.messageRes(): Int = when (this) {
    StockError.UNAUTHORIZED -> R.string.stock_in_error_unauthorized
    StockError.MATERIAL_NOT_FOUND -> R.string.stock_in_error_material
    StockError.MATERIAL_INACTIVE -> R.string.stock_in_error_inactive
    StockError.VALIDATION_FAILED -> R.string.stock_in_error_validation
    StockError.NETWORK_UNAVAILABLE -> R.string.stock_in_error_network
    StockError.UNKNOWN -> R.string.stock_in_error_unknown
}
