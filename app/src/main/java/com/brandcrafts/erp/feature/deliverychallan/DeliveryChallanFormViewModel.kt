package com.brandcrafts.erp.feature.deliverychallan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.DeliveryChallanCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanDraftUpdateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanInvoiceCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanInvoiceLineRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanLine
import com.brandcrafts.erp.domain.model.DeliveryChallanSourceType
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.usecase.ObserveInventoryItemsUseCase
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.CreateDeliveryChallanFromInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.CreateIndependentDeliveryChallanUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.DeliveryChallanValidator
import com.brandcrafts.erp.domain.usecase.deliverychallan.GetDeliveryChallanUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.UpdateDraftDeliveryChallanUseCase
import com.brandcrafts.erp.domain.usecase.invoice.GetInvoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DeliveryChallanFormViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val observeContacts: ObserveContactsUseCase,
    private val observeInventoryItems: ObserveInventoryItemsUseCase,
    private val getDeliveryChallan: GetDeliveryChallanUseCase,
    private val getInvoice: GetInvoiceUseCase,
    private val createIndependentDeliveryChallan: CreateIndependentDeliveryChallanUseCase,
    private val createDeliveryChallanFromInvoice: CreateDeliveryChallanFromInvoiceUseCase,
    private val updateDraftDeliveryChallan: UpdateDraftDeliveryChallanUseCase,
) : ViewModel() {

    private val validator = DeliveryChallanValidator()
    private val sourceInvoiceQuantityByLineId = mutableMapOf<String, BigDecimal>()

    private val _state = MutableStateFlow(DeliveryChallanFormUiState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<DeliveryChallanFormUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        observeCustomerOptions()
        observeMaterialOptions()
        initialize()
    }

    fun onEvent(event: DeliveryChallanFormUiEvent) {
        when (event) {
            is DeliveryChallanFormUiEvent.CustomerChanged -> selectCustomer(event.id)
            is DeliveryChallanFormUiEvent.AddressChanged -> updateState { copy(deliveryAddress = event.value) }
            is DeliveryChallanFormUiEvent.DateChanged -> updateState { copy(dateMillis = event.value) }
            is DeliveryChallanFormUiEvent.VehicleChanged -> updateState { copy(vehicleNumber = event.value) }
            is DeliveryChallanFormUiEvent.DriverChanged -> updateState { copy(driverName = event.value) }
            is DeliveryChallanFormUiEvent.NotesChanged -> updateState { copy(notes = event.value) }
            DeliveryChallanFormUiEvent.AddLine -> addLine()
            is DeliveryChallanFormUiEvent.RemoveLine -> removeLine(event.id)
            is DeliveryChallanFormUiEvent.MaterialChanged -> selectMaterial(event.id, event.materialId)
            is DeliveryChallanFormUiEvent.LineChanged -> changeLine(event)
            DeliveryChallanFormUiEvent.Save -> save()
            DeliveryChallanFormUiEvent.Retry -> initialize()
        }
    }

    private fun observeCustomerOptions() {
        viewModelScope.launch {
            observeContacts().collect { result ->
                result.onSuccess { contacts ->
                    val customers = contacts
                        .asSequence()
                        .filter { it.active && it.type == ContactType.CUSTOMER }
                        .map {
                            DeliveryChallanCustomerOption(
                                id = it.id,
                                label = it.company.ifBlank { it.name },
                                deliveryAddress = it.address,
                            )
                        }
                        .toList()
                    updateState { copy(customerOptions = customers) }
                }.onFailure(::emitFailure)
            }
        }
    }

    private fun observeMaterialOptions() {
        viewModelScope.launch {
            observeInventoryItems().collect { result ->
                when (result) {
                    is InventoryResult.Success -> {
                        val materials = result.data
                            .asSequence()
                            .filter { it.active }
                            .map {
                                DeliveryChallanMaterialOption(
                                    id = it.id,
                                    name = it.name,
                                    unit = it.unit,
                                )
                            }
                            .toList()
                        updateState { copy(materialOptions = materials) }
                    }
                    is InventoryResult.Error -> Unit
                }
            }
        }
    }

    private fun initialize() {
        val challanId = savedStateHandle.get<String>(CHALLAN_ID_ARGUMENT)
        val invoiceId = savedStateHandle.get<String>(INVOICE_ID_ARGUMENT)
        when {
            !challanId.isNullOrBlank() -> loadDraft(challanId)
            !invoiceId.isNullOrBlank() -> loadInvoice(invoiceId)
            else -> updateState {
                copy(
                    content = DeliveryChallanFormContent.Ready,
                    mode = DeliveryChallanFormMode.INDEPENDENT_CREATE,
                    dateMillis = dateMillis ?: System.currentTimeMillis(),
                )
            }
        }
    }

    private fun loadDraft(challanId: String) {
        viewModelScope.launch {
            getDeliveryChallan(challanId).fold(
                onSuccess = { challan ->
                    if (challan.status != DeliveryChallanStatus.DRAFT) {
                        emitFailure(DeliveryChallanFailure(DeliveryChallanError.DraftOnlyUpdateRequired))
                        return@fold
                    }
                    sourceInvoiceQuantityByLineId.clear()
                    updateState {
                        copy(
                            content = DeliveryChallanFormContent.Ready,
                            mode = DeliveryChallanFormMode.EDIT_DRAFT,
                            challanId = challan.id,
                            selectedCustomerId = challan.customerId,
                            deliveryAddress = challan.deliveryAddress,
                            dateMillis = challan.dateMillis,
                            sourceInvoiceId = challan.sourceInvoiceId,
                            sourceInvoiceNumber = challan.sourceInvoiceNumber,
                            vehicleNumber = challan.vehicleNumber,
                            driverName = challan.driverName,
                            notes = challan.notes,
                            lines = challan.lines.map { line ->
                                EditableDeliveryChallanLine(
                                    localId = line.id,
                                    persistedLineId = line.id,
                                    sourceInvoiceLineId = null,
                                    materialId = line.materialId.takeIf(String::isNotBlank),
                                    description = line.description,
                                    quantity = line.quantity,
                                    unit = line.unit,
                                )
                            },
                            errors = DeliveryChallanFormErrors(),
                            isSaving = false,
                        )
                    }
                },
                onFailure = ::emitFailure,
            )
        }
    }

    private fun loadInvoice(invoiceId: String) {
        viewModelScope.launch {
            getInvoice(invoiceId).fold(
                onSuccess = { invoice ->
                    if (invoice.status != InvoiceStatus.ISSUED) {
                        emitFailure(DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource))
                        return@fold
                    }
                    sourceInvoiceQuantityByLineId.clear()
                    invoice.lines.forEach { sourceInvoiceQuantityByLineId[it.id] = it.quantity }
                    updateState {
                        copy(
                            content = DeliveryChallanFormContent.Ready,
                            mode = DeliveryChallanFormMode.INVOICE_CREATE,
                            challanId = null,
                            selectedCustomerId = invoice.customerId,
                            dateMillis = dateMillis ?: System.currentTimeMillis(),
                            sourceInvoiceId = invoice.id,
                            sourceInvoiceNumber = invoice.number,
                            lines = invoice.lines.map { line ->
                                EditableDeliveryChallanLine(
                                    localId = newLineId(),
                                    persistedLineId = null,
                                    sourceInvoiceLineId = line.id,
                                    materialId = line.materialId.takeIf(String::isNotBlank),
                                    description = line.description,
                                    quantity = line.quantity,
                                    unit = line.unit,
                                )
                            },
                            errors = DeliveryChallanFormErrors(),
                            isSaving = false,
                        )
                    }
                },
                onFailure = ::emitFailure,
            )
        }
    }

    private fun selectCustomer(customerId: String) {
        updateState {
            val customer = customerOptions.firstOrNull { it.id == customerId }
            copy(
                selectedCustomerId = customerId,
                deliveryAddress = customer?.deliveryAddress ?: deliveryAddress,
            )
        }
    }

    private fun addLine() {
        updateState {
            copy(
                lines = lines + EditableDeliveryChallanLine(
                    localId = newLineId(),
                    persistedLineId = null,
                    sourceInvoiceLineId = null,
                    materialId = null,
                    description = "",
                    quantity = null,
                    unit = "",
                ),
            )
        }
    }

    private fun removeLine(localId: String) {
        updateState { copy(lines = lines.filterNot { it.localId == localId }) }
    }

    private fun selectMaterial(localId: String, materialId: String) {
        updateLine(localId) { line ->
            state.value.materialOptions.firstOrNull { it.id == materialId }?.let { material ->
                line.copy(
                    materialId = material.id,
                    description = material.name,
                    unit = material.unit,
                )
            } ?: line
        }
    }

    private fun changeLine(event: DeliveryChallanFormUiEvent.LineChanged) {
        updateLine(event.id) { line ->
            line.copy(
                description = event.description ?: line.description,
                quantity = event.quantity ?: line.quantity,
                unit = event.unit ?: line.unit,
            )
        }
    }

    private fun save() {
        val snapshot = state.value
        if (snapshot.isSaving) return

        val validationFailure = validate(snapshot)
        if (validationFailure != null) {
            emitFailure(validationFailure)
            return
        }

        updateState { copy(isSaving = true, errors = DeliveryChallanFormErrors()) }
        viewModelScope.launch {
            val result = when (snapshot.mode) {
                DeliveryChallanFormMode.INDEPENDENT_CREATE -> {
                    createIndependentDeliveryChallan(snapshot.toIndependentCreateRequest())
                }
                DeliveryChallanFormMode.INVOICE_CREATE -> {
                    createDeliveryChallanFromInvoice(snapshot.toInvoiceCreateRequest())
                }
                DeliveryChallanFormMode.EDIT_DRAFT -> {
                    updateDraftDeliveryChallan(snapshot.toDraftUpdateRequest()).map { snapshot.challanId.orEmpty() }
                }
            }
            result.fold(
                onSuccess = { challanId ->
                    updateState { copy(isSaving = false) }
                    effectChannel.trySend(DeliveryChallanFormUiEffect.Saved(challanId))
                },
                onFailure = ::emitFailure,
            )
        }
    }

    private fun validate(state: DeliveryChallanFormUiState): Throwable? {
        val customerId = state.selectedCustomerId ?: return DeliveryChallanFailure(DeliveryChallanError.CustomerRequired)
        val dateMillis = state.dateMillis ?: return DeliveryChallanFailure(DeliveryChallanError.DeliveryDateRequired)

        if (state.mode == DeliveryChallanFormMode.INVOICE_CREATE) {
            if (state.sourceInvoiceId.isNullOrBlank() || state.sourceInvoiceNumber.isNullOrBlank()) {
                return DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
            }
            val sourceIds = state.lines.map { it.sourceInvoiceLineId }
            if (sourceIds.any { it.isNullOrBlank() } || sourceIds.filterNotNull().toSet().size != sourceIds.size) {
                return DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
            }
            state.lines.forEach { line ->
                val quantity = line.quantity ?: return DeliveryChallanFailure(DeliveryChallanError.InvalidQuantity)
                val available = sourceInvoiceQuantityByLineId[line.sourceInvoiceLineId]
                    ?: return DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                if (quantity > available) return DeliveryChallanFailure(DeliveryChallanError.InvoiceQuantityExceeded)
            }
        }

        val sourceType = if (state.mode == DeliveryChallanFormMode.INVOICE_CREATE || !state.sourceInvoiceId.isNullOrBlank()) {
            DeliveryChallanSourceType.INVOICE
        } else {
            DeliveryChallanSourceType.INDEPENDENT
        }
        return validator.validateDraft(
            customerId = customerId,
            deliveryAddress = state.deliveryAddress,
            dateMillis = dateMillis,
            sourceType = sourceType,
            sourceInvoiceId = state.sourceInvoiceId,
            sourceInvoiceNumber = state.sourceInvoiceNumber,
            // Validation needs a stable non-blank identity for newly added local lines;
            // only the persisted update request converts new line IDs to blank.
            lines = state.toDomainLines(forDraftUpdate = false),
        ).exceptionOrNull()
    }

    private fun DeliveryChallanFormUiState.toIndependentCreateRequest() = DeliveryChallanCreateRequest(
        customerId = requireNotNull(selectedCustomerId),
        deliveryAddress = deliveryAddress,
        dateMillis = requireNotNull(dateMillis),
        sourceType = DeliveryChallanSourceType.INDEPENDENT,
        sourceInvoiceId = null,
        sourceInvoiceNumber = null,
        vehicleNumber = vehicleNumber,
        driverName = driverName,
        notes = notes,
        lines = toDomainLines(forDraftUpdate = false),
    )

    private fun DeliveryChallanFormUiState.toInvoiceCreateRequest() = DeliveryChallanInvoiceCreateRequest(
        invoiceId = requireNotNull(sourceInvoiceId),
        deliveryAddress = deliveryAddress,
        dateMillis = requireNotNull(dateMillis),
        vehicleNumber = vehicleNumber,
        driverName = driverName,
        notes = notes,
        lines = lines.map {
            DeliveryChallanInvoiceLineRequest(
                sourceInvoiceLineId = requireNotNull(it.sourceInvoiceLineId),
                quantity = requireNotNull(it.quantity),
            )
        },
    )

    private fun DeliveryChallanFormUiState.toDraftUpdateRequest() = DeliveryChallanDraftUpdateRequest(
        challanId = requireNotNull(challanId),
        customerId = requireNotNull(selectedCustomerId),
        deliveryAddress = deliveryAddress,
        dateMillis = requireNotNull(dateMillis),
        sourceType = if (sourceInvoiceId.isNullOrBlank()) DeliveryChallanSourceType.INDEPENDENT else DeliveryChallanSourceType.INVOICE,
        sourceInvoiceId = sourceInvoiceId,
        sourceInvoiceNumber = sourceInvoiceNumber,
        vehicleNumber = vehicleNumber,
        driverName = driverName,
        notes = notes,
        lines = toDomainLines(forDraftUpdate = true),
    )

    private fun DeliveryChallanFormUiState.toDomainLines(forDraftUpdate: Boolean): List<DeliveryChallanLine> =
        lines.mapIndexed { index, line ->
            DeliveryChallanLine(
                // A blank ID tells the update data source to allocate a new persisted line ID.
                id = if (forDraftUpdate) line.persistedLineId.orEmpty() else line.localId,
                materialId = line.materialId.orEmpty(),
                description = line.description,
                quantity = line.quantity ?: BigDecimal.ZERO,
                unit = line.unit,
                sortOrder = index,
            )
        }

    private fun updateLine(
        localId: String,
        transform: (EditableDeliveryChallanLine) -> EditableDeliveryChallanLine,
    ) {
        updateState { copy(lines = lines.map { if (it.localId == localId) transform(it) else it }) }
    }

    private fun updateState(transform: DeliveryChallanFormUiState.() -> DeliveryChallanFormUiState) {
        _state.update { it.transform() }
    }

    private fun emitFailure(throwable: Throwable) {
        // A recoverable operation failure must leave the editable form intact.
        updateState { copy(content = DeliveryChallanFormContent.Ready, isSaving = false) }
        val message = DeliveryChallanPresentationErrorMapper.map(throwable)
        effectChannel.trySend(
            if (message.unauthorized) DeliveryChallanFormUiEffect.Unauthorized
            else DeliveryChallanFormUiEffect.ShowMessage(message.messageRes),
        )
    }

    private fun newLineId(): String = UUID.randomUUID().toString()

    private companion object {
        const val CHALLAN_ID_ARGUMENT = "challanId"
        const val INVOICE_ID_ARGUMENT = "invoiceId"
    }
}
