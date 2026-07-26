package com.brandcrafts.erp.feature.purchaseorder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.core.result.PurchaseOrderError
import com.brandcrafts.erp.core.result.PurchaseOrderFailure
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.PurchaseOrder
import com.brandcrafts.erp.domain.model.PurchaseOrderDraft
import com.brandcrafts.erp.domain.model.PurchaseOrderDraftLine
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.domain.usecase.ObserveInventoryItemsUseCase
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.CreatePurchaseOrderUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.GetPurchaseOrderUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.PurchaseOrderCalculationLine
import com.brandcrafts.erp.domain.usecase.purchaseorder.PurchaseOrderCalculator
import com.brandcrafts.erp.domain.usecase.purchaseorder.UpdatePurchaseOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PurchaseOrderFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeContacts: ObserveContactsUseCase,
    private val observeInventoryItems: ObserveInventoryItemsUseCase,
    private val getPurchaseOrder: GetPurchaseOrderUseCase,
    private val createPurchaseOrder: CreatePurchaseOrderUseCase,
    private val updatePurchaseOrder: UpdatePurchaseOrderUseCase,
    private val calculator: PurchaseOrderCalculator,
) : ViewModel() {
    private val purchaseOrderId = savedStateHandle.get<String>(PURCHASE_ORDER_ID_ARGUMENT)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    private val mode = if (savedStateHandle.get<String>(MODE_ARGUMENT) == EDIT_MODE || purchaseOrderId != null) {
        PurchaseOrderFormMode.Edit(purchaseOrderId.orEmpty())
    } else {
        PurchaseOrderFormMode.Create
    }

    private val _state = MutableStateFlow(PurchaseOrderFormUiState(mode = mode))
    val state = _state.asStateFlow()

    private val effectsChannel = Channel<PurchaseOrderFormUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var contactsJob: Job? = null
    private var inventoryJob: Job? = null
    private var purchaseOrderJob: Job? = null
    private var suppliersReady = false
    private var inventoryReady = false
    private var purchaseOrderReady = mode is PurchaseOrderFormMode.Create

    init {
        initialize()
    }

    fun onEvent(event: PurchaseOrderFormUiEvent) {
        when (event) {
            PurchaseOrderFormUiEvent.Load,
            PurchaseOrderFormUiEvent.Retry -> initialize()
            is PurchaseOrderFormUiEvent.SupplierSelected -> selectSupplier(event.id)
            is PurchaseOrderFormUiEvent.DateSelected -> update { copy(dateMillis = event.value, dateError = null, error = false) }
            is PurchaseOrderFormUiEvent.ExpectedDeliverySelected -> update { copy(expectedDeliveryDateMillis = event.value, error = false) }
            PurchaseOrderFormUiEvent.ExpectedDeliveryCleared -> update { copy(expectedDeliveryDateMillis = null, error = false) }
            PurchaseOrderFormUiEvent.AddLine -> addLine()
            is PurchaseOrderFormUiEvent.RemoveLine -> removeLine(event.localId)
            is PurchaseOrderFormUiEvent.InventorySelected -> selectInventory(event.localId, event.itemId)
            is PurchaseOrderFormUiEvent.DescriptionChanged -> changeLine(event.localId) { it.copy(description = event.value) }
            is PurchaseOrderFormUiEvent.QuantityChanged -> changeLine(event.localId) { it.copy(quantity = event.value) }
            is PurchaseOrderFormUiEvent.UnitChanged -> changeLine(event.localId) { it.copy(unit = event.value) }
            is PurchaseOrderFormUiEvent.UnitPriceChanged -> changeLine(event.localId) { it.copy(unitPrice = event.value) }
            PurchaseOrderFormUiEvent.Save -> save()
            PurchaseOrderFormUiEvent.ErrorDismissed -> update { copy(error = false) }
        }
    }

    private fun initialize() {
        if (_state.value.saving) return
        contactsJob?.cancel()
        inventoryJob?.cancel()
        purchaseOrderJob?.cancel()
        suppliersReady = false
        inventoryReady = false
        purchaseOrderReady = mode is PurchaseOrderFormMode.Create
        update { copy(loading = true, error = false) }
        observeSuppliers()
        observeInventory()
        when (val formMode = mode) {
            PurchaseOrderFormMode.Create -> finishLoadingWhenReady()
            is PurchaseOrderFormMode.Edit -> {
                if (formMode.purchaseOrderId.isBlank()) {
                    setLoadError(PurchaseOrderError.PurchaseOrderNotFound)
                } else {
                    loadPurchaseOrder(formMode.purchaseOrderId)
                }
            }
        }
    }

    private fun observeSuppliers() {
        contactsJob = viewModelScope.launch {
            try {
                observeContacts().collect { result ->
                    result.fold(
                        onSuccess = { contacts ->
                            val suppliers = contacts
                                .asSequence()
                                .filter { it.active && it.type == ContactType.SUPPLIER }
                                .map { PurchaseOrderSupplierOption(it.id, it.name, it.company) }
                                .toList()
                            update { copy(suppliers = suppliers) }
                            suppliersReady = true
                            finishLoadingWhenReady()
                        },
                        onFailure = { setLoadError(PurchaseOrderError.Unknown) },
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(PurchaseOrderError.Unknown)
            }
        }
    }

    private fun observeInventory() {
        inventoryJob = viewModelScope.launch {
            try {
                observeInventoryItems().collect { result ->
                    when (result) {
                        is InventoryResult.Success -> {
                            val inventory = result.data
                                .asSequence()
                                .filter { it.active }
                                .map { PurchaseOrderInventoryOption(it.id, it.name, it.unit) }
                                .toList()
                            update { copy(inventory = inventory) }
                            inventoryReady = true
                            finishLoadingWhenReady()
                        }
                        is InventoryResult.Error -> setLoadError(PurchaseOrderError.Unknown)
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(PurchaseOrderError.Unknown)
            }
        }
    }

    private fun loadPurchaseOrder(id: String) {
        purchaseOrderJob = viewModelScope.launch {
            try {
                getPurchaseOrder(id).fold(
                    onSuccess = ::restorePurchaseOrder,
                    onFailure = ::setLoadError,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(exception)
            }
        }
    }

    private fun restorePurchaseOrder(order: PurchaseOrder) {
        if (order.status != PurchaseOrderStatus.DRAFT) {
            purchaseOrderReady = true
            update { copy(loading = false, editingAllowed = false, number = order.number, error = true) }
            sendMessage(PurchaseOrderError.DraftOnlyUpdateRequired)
            return
        }
        val restoredLines = order.lines.map { line ->
            recalculateLine(
                EditablePurchaseOrderLine(
                    lineId = line.id,
                    materialId = line.materialId,
                    description = line.description,
                    quantity = line.quantity.toPlainString(),
                    unit = line.unit,
                    unitPrice = line.unitPrice.toPlainString(),
                ),
            )
        }
        purchaseOrderReady = true
        update {
            copy(
                supplierId = order.supplierId,
                dateMillis = order.dateMillis,
                expectedDeliveryDateMillis = order.expectedDeliveryDateMillis,
                lines = restoredLines,
                total = totalFor(restoredLines),
                number = order.number,
                editingAllowed = true,
                supplierError = null,
                dateError = if (order.dateMillis == null) PurchaseOrderFieldError.REQUIRED else null,
                error = false,
            )
        }
        finishLoadingWhenReady()
    }

    private fun finishLoadingWhenReady() {
        if (suppliersReady && inventoryReady && purchaseOrderReady && !_state.value.error) {
            update { copy(loading = false) }
        }
    }

    private fun setLoadError(throwable: Throwable) {
        update { copy(loading = false, saving = false, error = true) }
        handleError(throwable, emitMessage = false)
    }

    private fun setLoadError(error: PurchaseOrderError) {
        setLoadError(PurchaseOrderFailure(error))
    }

    private fun selectSupplier(id: String) {
        val selected = _state.value.suppliers.any { it.id == id }
        update {
            copy(
                supplierId = id,
                supplierError = if (selected) null else PurchaseOrderFieldError.OUT_OF_RANGE,
                error = false,
            )
        }
    }

    private fun addLine() {
        update { copy(lines = lines + EditablePurchaseOrderLine(), error = false) }
    }

    private fun removeLine(localId: String) {
        val lines = _state.value.lines.filterNot { it.localId == localId }
        update { copy(lines = lines, total = totalFor(lines), error = false) }
    }

    private fun selectInventory(localId: String, itemId: String) {
        val item = _state.value.inventory.firstOrNull { it.id == itemId }
        changeLine(localId) { line ->
            if (item == null) {
                line.copy(materialId = null, descriptionError = PurchaseOrderFieldError.OUT_OF_RANGE)
            } else {
                line.copy(
                    materialId = item.id,
                    description = item.name,
                    unit = item.unit,
                    descriptionError = null,
                )
            }
        }
    }

    private fun changeLine(localId: String, transform: (EditablePurchaseOrderLine) -> EditablePurchaseOrderLine) {
        val lines = _state.value.lines.map { line ->
            if (line.localId == localId) recalculateLine(transform(line)) else line
        }
        update { copy(lines = lines, total = totalFor(lines), error = false) }
    }

    private fun recalculateLine(line: EditablePurchaseOrderLine): EditablePurchaseOrderLine {
        val quantity = line.quantity.toPositiveDecimal()
        val price = line.unitPrice.toNonNegativeDecimal()
        val descriptionError = when {
            line.materialId.isNullOrBlank() -> PurchaseOrderFieldError.REQUIRED
            line.description.trim().isBlank() -> PurchaseOrderFieldError.REQUIRED
            else -> null
        }
        val unitError = if (line.unit.trim().isBlank()) PurchaseOrderFieldError.REQUIRED else null
        if (descriptionError != null || unitError != null || quantity.error != null || price.error != null) {
            return line.copy(
                descriptionError = descriptionError,
                quantityError = quantity.error,
                unitError = unitError,
                unitPriceError = price.error,
                lineTotal = null,
            )
        }
        return try {
            line.copy(
                descriptionError = null,
                quantityError = null,
                unitError = null,
                unitPriceError = null,
                lineTotal = calculator.lineTotal(requireNotNull(quantity.value), requireNotNull(price.value)),
            )
        } catch (exception: IllegalArgumentException) {
            line.copy(quantityError = PurchaseOrderFieldError.MALFORMED, lineTotal = null)
        }
    }

    private fun totalFor(lines: List<EditablePurchaseOrderLine>): BigDecimal? {
        val calculations = lines.mapNotNull { line ->
            val quantity = line.quantity.toPositiveDecimal().value
            val price = line.unitPrice.toNonNegativeDecimal().value
            if (line.materialId.isNullOrBlank() || line.description.trim().isBlank() || line.unit.trim().isBlank() || quantity == null || price == null) {
                null
            } else {
                PurchaseOrderCalculationLine(quantity, price)
            }
        }
        return calculations.takeIf { it.isNotEmpty() }?.let(calculator::total)
    }

    private fun save() {
        val current = _state.value
        if (current.saving || current.loading || !current.editingAllowed) return
        val validated = validateForSave(current)
        _state.update { validated }
        if (validated.supplierError != null || validated.dateError != null || validated.lines.isEmpty() || validated.lines.any(::hasLineErrors)) {
            update { copy(error = true) }
            return
        }
        val draft = validated.toDraft() ?: run {
            update { copy(error = true) }
            return
        }
        update { copy(saving = true, error = false) }
        viewModelScope.launch {
            try {
                val result = when (val formMode = validated.mode) {
                    PurchaseOrderFormMode.Create -> createPurchaseOrder(draft)
                    is PurchaseOrderFormMode.Edit -> {
                        if (formMode.purchaseOrderId.isBlank() || !validated.editingAllowed) {
                            Result.failure(PurchaseOrderFailure(PurchaseOrderError.DraftOnlyUpdateRequired))
                        } else {
                            updatePurchaseOrder(formMode.purchaseOrderId, draft).map { formMode.purchaseOrderId }
                        }
                    }
                }
                result.fold(
                    onSuccess = { id ->
                        update { copy(saving = false) }
                        sendEffect(
                            PurchaseOrderFormUiEffect.ShowMessage(
                                if (validated.mode is PurchaseOrderFormMode.Create) R.string.purchase_order_created_success else R.string.purchase_order_updated_success,
                            ),
                        )
                        sendEffect(PurchaseOrderFormUiEffect.Saved(id))
                    },
                    onFailure = { throwable ->
                        update { copy(saving = false) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                update { copy(saving = false) }
                handleError(exception)
            }
        }
    }

    private fun validateForSave(state: PurchaseOrderFormUiState): PurchaseOrderFormUiState {
        val lines = state.lines.map(::recalculateLine)
        val supplierError = when {
            state.supplierId == null -> PurchaseOrderFieldError.REQUIRED
            state.suppliers.none { it.id == state.supplierId } -> PurchaseOrderFieldError.OUT_OF_RANGE
            else -> null
        }
        return state.copy(
            lines = lines,
            total = totalFor(lines),
            supplierError = supplierError,
            dateError = if (state.dateMillis == null || state.dateMillis <= 0L) PurchaseOrderFieldError.REQUIRED else null,
        )
    }

    private fun hasLineErrors(line: EditablePurchaseOrderLine): Boolean =
        line.descriptionError != null ||
            line.quantityError != null ||
            line.unitError != null ||
            line.unitPriceError != null ||
            line.lineTotal == null

    private fun PurchaseOrderFormUiState.toDraft(): PurchaseOrderDraft? = try {
        PurchaseOrderDraft(
            supplierId = requireNotNull(supplierId),
            dateMillis = requireNotNull(dateMillis),
            expectedDeliveryDateMillis = expectedDeliveryDateMillis,
            supplierReferenceNumber = "",
            remarks = "",
            lines = lines.map { line ->
                PurchaseOrderDraftLine(
                    id = line.lineId,
                    materialId = requireNotNull(line.materialId),
                    description = line.description.trim(),
                    quantity = BigDecimal(line.quantity.trim()),
                    unit = line.unit.trim(),
                    unitPrice = BigDecimal(line.unitPrice.trim()),
                )
            },
        )
    } catch (exception: IllegalArgumentException) {
        null
    }

    private fun String.toPositiveDecimal(): DecimalValidation {
        val value = trim()
        if (value.isBlank()) return DecimalValidation(error = PurchaseOrderFieldError.REQUIRED)
        val decimal = value.toBigDecimalOrNull() ?: return DecimalValidation(error = PurchaseOrderFieldError.MALFORMED)
        return if (decimal > BigDecimal.ZERO) DecimalValidation(value = decimal) else DecimalValidation(error = PurchaseOrderFieldError.OUT_OF_RANGE)
    }

    private fun String.toNonNegativeDecimal(): DecimalValidation {
        val value = trim()
        if (value.isBlank()) return DecimalValidation(error = PurchaseOrderFieldError.REQUIRED)
        val decimal = value.toBigDecimalOrNull() ?: return DecimalValidation(error = PurchaseOrderFieldError.MALFORMED)
        return if (decimal >= BigDecimal.ZERO) DecimalValidation(value = decimal) else DecimalValidation(error = PurchaseOrderFieldError.OUT_OF_RANGE)
    }

    private fun handleError(throwable: Throwable, emitMessage: Boolean = true) {
        val error = (throwable as? PurchaseOrderFailure)?.error ?: PurchaseOrderError.Unknown
        val message = error.toPurchaseOrderMessage()
        if (message.unauthorized) {
            sendEffect(PurchaseOrderFormUiEffect.Unauthorized)
        } else if (emitMessage) {
            sendEffect(PurchaseOrderFormUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendMessage(error: PurchaseOrderError) {
        val message = error.toPurchaseOrderMessage()
        if (message.unauthorized) {
            sendEffect(PurchaseOrderFormUiEffect.Unauthorized)
        } else {
            sendEffect(PurchaseOrderFormUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun update(transform: PurchaseOrderFormUiState.() -> PurchaseOrderFormUiState) {
        _state.update(transform)
    }

    private fun sendEffect(effect: PurchaseOrderFormUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }

    private data class DecimalValidation(
        val value: BigDecimal? = null,
        val error: PurchaseOrderFieldError? = null,
    )

    private companion object {
        const val MODE_ARGUMENT = "mode"
        const val PURCHASE_ORDER_ID_ARGUMENT = "purchaseOrderId"
        const val EDIT_MODE = "edit"
    }
}
