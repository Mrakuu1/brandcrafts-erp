package com.brandcrafts.erp.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.InventoryError
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.usecase.ObserveInventoryItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val observeInventoryItems: ObserveInventoryItemsUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<InventoryUiState>(InventoryUiState.Loading())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _effects = Channel<InventoryUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var observedItems: List<InventoryListItem> = emptyList()
    private var searchQuery: String = ""
    private var observationJob: Job? = null

    init {
        observeItems()
    }

    fun onEvent(event: InventoryUiEvent) {
        when (event) {
            is InventoryUiEvent.SearchQueryChanged -> {
                searchQuery = event.query
                updateFilteredState()
            }
            is InventoryUiEvent.ItemClicked -> sendEffect(InventoryUiEffect.NavigateToItemDetails(event.itemId))
            is InventoryUiEvent.EditItemClicked -> sendEffect(InventoryUiEffect.NavigateToEditItem(event.itemId))
            is InventoryUiEvent.StockInClicked -> sendEffect(InventoryUiEffect.NavigateToStockIn(event.itemId))
            InventoryUiEvent.AddItemClicked -> sendEffect(InventoryUiEffect.NavigateToCreateItem)
            InventoryUiEvent.FilterClicked -> sendEffect(InventoryUiEffect.ShowMessage(R.string.feature_coming_later))
            InventoryUiEvent.RetryClicked -> observeItems()
        }
    }

    private fun observeItems() {
        observationJob?.cancel()
        _uiState.value = InventoryUiState.Loading(searchQuery)
        observationJob = viewModelScope.launch {
            observeInventoryItems().collect { result ->
                when (result) {
                    is InventoryResult.Success -> {
                        observedItems = result.data.map { it.toInventoryListItem() }
                        updateFilteredState()
                    }
                    is InventoryResult.Error -> {
                        _uiState.value = InventoryUiState.Error(searchQuery, result.error.toInventoryErrorType())
                    }
                }
            }
        }
    }

    private fun updateFilteredState() {
        val normalizedQuery = searchQuery.trim()
        val filteredItems = observedItems.filter { item ->
            normalizedQuery.isBlank() ||
                item.name.contains(normalizedQuery, ignoreCase = true) ||
                item.sku.contains(normalizedQuery, ignoreCase = true) ||
                item.category.contains(normalizedQuery, ignoreCase = true)
        }
        _uiState.value = if (filteredItems.isEmpty()) {
            InventoryUiState.Empty(searchQuery)
        } else {
            InventoryUiState.Content(searchQuery, filteredItems)
        }
    }

    private fun sendEffect(effect: InventoryUiEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

private fun InventoryError.toInventoryErrorType(): InventoryErrorType = when (this) {
    InventoryError.NETWORK_UNAVAILABLE -> InventoryErrorType.NETWORK
    InventoryError.UNAUTHORIZED -> InventoryErrorType.UNAUTHORIZED
    InventoryError.VALIDATION_FAILED,
    InventoryError.ITEM_NOT_FOUND,
    InventoryError.UNKNOWN,
    -> InventoryErrorType.UNKNOWN
}
