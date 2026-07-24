package com.brandcrafts.erp.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.InventoryError
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.model.InventoryItem
import com.brandcrafts.erp.domain.model.InventoryItemInput
import com.brandcrafts.erp.domain.model.InventoryItemUpdate
import com.brandcrafts.erp.domain.usecase.CreateInventoryItemUseCase
import com.brandcrafts.erp.domain.usecase.GetInventoryItemUseCase
import com.brandcrafts.erp.domain.usecase.UpdateInventoryItemUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class InventoryFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getInventoryItem: GetInventoryItemUseCase,
    private val createInventoryItem: CreateInventoryItemUseCase,
    private val updateInventoryItem: UpdateInventoryItemUseCase,
) : ViewModel() {
    private val itemId: String? = savedStateHandle[INVENTORY_ITEM_ID_ARGUMENT]
    private val mode = if (itemId == null) InventoryFormMode.CREATE else InventoryFormMode.EDIT

    private val _uiState = MutableStateFlow(InventoryFormUiState(mode = mode, itemId = itemId))
    val uiState: StateFlow<InventoryFormUiState> = _uiState.asStateFlow()

    private val _effects = Channel<InventoryFormUiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        if (mode == InventoryFormMode.EDIT) loadItem(requireNotNull(itemId))
    }

    fun onEvent(event: InventoryFormUiEvent) {
        when (event) {
            is InventoryFormUiEvent.NameChanged -> update { copy(name = event.value, errors = errors.copy(name = null), saveError = null) }
            is InventoryFormUiEvent.SkuChanged -> update { copy(sku = event.value, errors = errors.copy(sku = null), saveError = null) }
            is InventoryFormUiEvent.CategoryChanged -> update { copy(category = event.value, errors = errors.copy(category = null), saveError = null) }
            is InventoryFormUiEvent.UnitChanged -> update { copy(unit = event.value, errors = errors.copy(unit = null), saveError = null) }
            is InventoryFormUiEvent.AvailableQuantityChanged -> update { copy(availableQuantity = event.value, errors = errors.copy(availableQuantity = null), saveError = null) }
            is InventoryFormUiEvent.MinimumQuantityChanged -> update { copy(minimumQuantity = event.value, errors = errors.copy(minimumQuantity = null), saveError = null) }
            is InventoryFormUiEvent.PurchasePriceChanged -> update { copy(purchasePrice = event.value, errors = errors.copy(purchasePrice = null), saveError = null) }
            is InventoryFormUiEvent.SellingPriceChanged -> update { copy(sellingPrice = event.value, errors = errors.copy(sellingPrice = null), saveError = null) }
            is InventoryFormUiEvent.DescriptionChanged -> update { copy(description = event.value, saveError = null) }
            is InventoryFormUiEvent.ActiveChanged -> update { copy(active = event.value, saveError = null) }
            InventoryFormUiEvent.SaveClicked -> save()
            InventoryFormUiEvent.CancelClicked -> sendEffect(InventoryFormUiEffect.NavigateBack)
            InventoryFormUiEvent.RetryLoadClicked -> itemId?.let(::loadItem)
        }
    }

    private fun loadItem(id: String) {
        update { copy(isInitialLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = getInventoryItem(id)) {
                is InventoryResult.Success -> _uiState.value = result.data.toFormState()
                is InventoryResult.Error -> update {
                    copy(isInitialLoading = false, loadError = result.error.toFormErrorType())
                }
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isInitialLoading) return

        val errors = state.validate()
        if (errors != InventoryFormFieldErrors()) {
            update { copy(errors = errors, saveError = null) }
            return
        }

        update { copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val currentState = _uiState.value
            val result = when (currentState.mode) {
                InventoryFormMode.CREATE -> createInventoryItem(currentState.toCreateInput())
                InventoryFormMode.EDIT -> updateInventoryItem(currentState.toUpdateInput())
            }
            when (result) {
                is InventoryResult.Success -> sendEffect(InventoryFormUiEffect.ItemSaved)
                is InventoryResult.Error -> update {
                    copy(isSaving = false, saveError = result.error.toFormErrorType())
                }
            }
        }
    }

    private fun update(transform: InventoryFormUiState.() -> InventoryFormUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun sendEffect(effect: InventoryFormUiEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun InventoryFormUiState.validate(): InventoryFormFieldErrors = InventoryFormFieldErrors(
        name = if (name.isBlank()) R.string.inventory_form_name_required else null,
        sku = if (sku.isBlank()) R.string.inventory_form_sku_required else null,
        category = if (category.isBlank()) R.string.inventory_form_category_required else null,
        unit = if (unit.isBlank()) R.string.inventory_form_unit_required else null,
        availableQuantity = availableQuantity.toNonNegativeError(),
        minimumQuantity = minimumQuantity.toNonNegativeError(),
        purchasePrice = purchasePrice.toNonNegativeError(),
        sellingPrice = sellingPrice.toNonNegativeError(),
    )

    private fun String.toNonNegativeError(): Int? = if (toDoubleOrNull()?.let { it >= 0 } == true) {
        null
    } else {
        R.string.inventory_form_non_negative_number
    }

    private fun InventoryFormUiState.toCreateInput(): InventoryItemInput = InventoryItemInput(
        name = name, sku = sku, category = category, unit = unit,
        availableQuantity = availableQuantity.toDouble(), minimumQuantity = minimumQuantity.toDouble(),
        purchasePrice = purchasePrice.toDouble(), sellingPrice = sellingPrice.toDouble(),
        description = description, active = active,
    )

    private fun InventoryFormUiState.toUpdateInput(): InventoryItemUpdate = InventoryItemUpdate(
        id = requireNotNull(itemId), name = name, sku = sku, category = category, unit = unit,
        availableQuantity = availableQuantity.toDouble(), minimumQuantity = minimumQuantity.toDouble(),
        purchasePrice = purchasePrice.toDouble(), sellingPrice = sellingPrice.toDouble(),
        description = description, active = active,
    )

    private fun InventoryItem.toFormState(): InventoryFormUiState = InventoryFormUiState(
        mode = InventoryFormMode.EDIT, itemId = id, name = name, sku = sku, category = category, unit = unit,
        availableQuantity = availableQuantity.toString(), minimumQuantity = minimumQuantity.toString(),
        purchasePrice = purchasePrice.toString(), sellingPrice = sellingPrice.toString(),
        description = description, active = active, isInitialLoading = false,
    )

    private fun InventoryError.toFormErrorType(): InventoryFormErrorType = when (this) {
        InventoryError.NETWORK_UNAVAILABLE -> InventoryFormErrorType.NETWORK
        InventoryError.UNAUTHORIZED -> InventoryFormErrorType.UNAUTHORIZED
        InventoryError.VALIDATION_FAILED -> InventoryFormErrorType.DUPLICATE_SKU
        InventoryError.ITEM_NOT_FOUND -> InventoryFormErrorType.NOT_FOUND
        InventoryError.UNKNOWN -> InventoryFormErrorType.UNKNOWN
    }

    private companion object {
        const val INVENTORY_ITEM_ID_ARGUMENT = "itemId"
    }
}
