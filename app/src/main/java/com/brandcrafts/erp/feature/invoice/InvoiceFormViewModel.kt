package com.brandcrafts.erp.feature.invoice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.InvoiceCreateRequest
import com.brandcrafts.erp.domain.model.InvoiceDraftUpdateLine
import com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest
import com.brandcrafts.erp.domain.model.InvoiceLine
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.ObserveInventoryItemsUseCase
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.invoice.CreateInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.invoice.GetInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.invoice.InvoiceCalculator
import com.brandcrafts.erp.domain.usecase.invoice.UpdateInvoiceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.util.UUID
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
class InvoiceFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeContacts: ObserveContactsUseCase,
    private val observeInventory: ObserveInventoryItemsUseCase,
    private val getInvoice: GetInvoiceUseCase,
    private val createInvoice: CreateInvoiceUseCase,
    private val updateInvoice: UpdateInvoiceUseCase,
    private val calculator: InvoiceCalculator,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val editInvoiceId = savedStateHandle.get<String>(INVOICE_ID_ARGUMENT)
        ?.trim()
        ?.takeIf(String::isNotEmpty)
    private val mode: InvoiceFormMode = editInvoiceId
        ?.let(InvoiceFormMode::EditDraft)
        ?: InvoiceFormMode.Create

    private val _state = MutableStateFlow(
        InvoiceFormUiState(
            mode = mode,
            isLoading = true,
            lines = if (mode is InvoiceFormMode.Create) listOf(newLine()) else emptyList(),
        ),
    )
    val state = _state.asStateFlow()

    private val effectsChannel = Channel<InvoiceFormUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var contactsJob: Job? = null
    private var inventoryJob: Job? = null
    private var invoiceJob: Job? = null
    private var contactsReady = false
    private var inventoryReady = false
    private var invoiceReady = mode is InvoiceFormMode.Create
    private var blockedEffectSent = false

    init {
        initialize()
    }

    fun onEvent(event: InvoiceFormUiEvent) {
        when (event) {
            InvoiceFormUiEvent.Retry -> initialize()
            is InvoiceFormUiEvent.CustomerSelected -> selectCustomer(event.customerId)
            is InvoiceFormUiEvent.InvoiceDateChanged -> updateDate(event.invoiceDateMillis)
            is InvoiceFormUiEvent.DueDateChanged -> updateDueDate(event.dueDateMillis)
            is InvoiceFormUiEvent.RemarksChanged -> update { copy(remarks = event.remarks, formError = null) }
            InvoiceFormUiEvent.AddLine -> addLine()
            is InvoiceFormUiEvent.RemoveLine -> removeLine(event.localId)
            is InvoiceFormUiEvent.MaterialSelected -> selectMaterial(event.localId, event.materialId)
            is InvoiceFormUiEvent.LineChanged -> changeLine(event)
            InvoiceFormUiEvent.Save -> save()
            InvoiceFormUiEvent.Back -> sendEffect(InvoiceFormUiEffect.NavigateBack)
        }
    }

    private fun initialize() {
        if (!canManageInvoices()) {
            update {
                copy(
                    isLoading = false,
                    isEditingBlocked = true,
                    formError = InvoiceFormError.EDITING_BLOCKED,
                )
            }
            sendEffect(InvoiceFormUiEffect.Unauthorized)
            return
        }
        contactsJob?.cancel()
        inventoryJob?.cancel()
        invoiceJob?.cancel()
        contactsReady = false
        inventoryReady = false
        invoiceReady = mode is InvoiceFormMode.Create
        blockedEffectSent = false
        update {
            copy(
                isLoading = true,
                isSaving = false,
                isEditingBlocked = false,
                formError = null,
            )
        }
        observeOptions()
        if (mode is InvoiceFormMode.EditDraft) {
            loadInvoice(mode.invoiceId)
        } else {
            finishLoadingWhenReady()
        }
    }

    private fun observeOptions() {
        contactsJob = viewModelScope.launch {
            try {
                observeContacts().collect { result ->
                    result.fold(
                        onSuccess = { contacts ->
                            update {
                                copy(
                                    customerOptions = contacts
                                        .asSequence()
                                        .filter { it.type == ContactType.CUSTOMER && it.active }
                                        .map { contact ->
                                            InvoiceCustomerOption(
                                                id = contact.id,
                                                label = contact.company.ifBlank { contact.name },
                                            )
                                        }
                                        .toList(),
                                )
                            }
                            contactsReady = true
                            finishLoadingWhenReady()
                        },
                        onFailure = { setLoadError(it) },
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(exception)
            }
        }
        inventoryJob = viewModelScope.launch {
            try {
                observeInventory().collect { result ->
                    when (result) {
                        is InventoryResult.Success -> {
                            update {
                                copy(
                                    materialOptions = result.data
                                        .asSequence()
                                        .filter { it.active }
                                        .map { InvoiceMaterialOption(it.id, it.name, it.unit) }
                                        .toList(),
                                )
                            }
                            inventoryReady = true
                            finishLoadingWhenReady()
                        }
                        is InventoryResult.Error -> setLoadError()
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(exception)
            }
        }
    }

    private fun loadInvoice(invoiceId: String) {
        invoiceJob = viewModelScope.launch {
            try {
                getInvoice(invoiceId).fold(
                    onSuccess = { invoice ->
                        if (invoice.status != InvoiceStatus.DRAFT) {
                            invoiceReady = true
                            update {
                                copy(
                                    isLoading = false,
                                    isEditingBlocked = true,
                                    formError = InvoiceFormError.EDITING_BLOCKED,
                                )
                            }
                            if (!blockedEffectSent) {
                                blockedEffectSent = true
                                sendEffect(InvoiceFormUiEffect.EditingBlocked)
                            }
                        } else {
                            invoiceReady = true
                            val restoredLines = invoice.lines.map { line ->
                                calculateLine(
                                    EditableInvoiceLine(
                                        localId = line.id,
                                        persistedLineId = line.id,
                                        materialId = line.materialId,
                                        description = line.description,
                                        quantity = line.quantity,
                                        unit = line.unit,
                                        unitPrice = line.unitPrice,
                                        discountPercent = line.discountPercent,
                                        taxPercent = line.taxPercent,
                                        lineSubtotal = null,
                                        lineDiscount = null,
                                        taxableAmount = null,
                                        lineTax = null,
                                        lineTotal = null,
                                    ),
                                )
                            }
                            update {
                                copy(
                                    invoiceNumber = invoice.number,
                                    customerId = invoice.customerId,
                                    invoiceDateMillis = invoice.invoiceDateMillis,
                                    dueDateMillis = invoice.dueDateMillis,
                                    lines = restoredLines,
                                    remarks = invoice.remarks,
                                    totals = totalsFor(restoredLines),
                                    formError = null,
                                )
                            }
                            finishLoadingWhenReady()
                        }
                    },
                    onFailure = ::setLoadError,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(exception)
            }
        }
    }

    private fun finishLoadingWhenReady() {
        if (contactsReady && inventoryReady && invoiceReady && !_state.value.isEditingBlocked) {
            update { copy(isLoading = false, formError = null) }
        }
    }

    private fun setLoadError(throwable: Throwable? = null) {
        update { copy(isLoading = false, formError = InvoiceFormError.LOAD) }
        if (throwable != null) handleError(throwable) else {
            sendEffect(InvoiceFormUiEffect.ShowMessage(R.string.invoice_error_generic))
        }
    }

    private fun selectCustomer(customerId: String) {
        update {
            copy(
                customerId = customerId.takeIf { id -> customerOptions.any { it.id == id } },
                errors = errors.copy(
                    customer = if (customerOptions.any { it.id == customerId }) null else InvoiceFieldError.OUT_OF_RANGE,
                ),
                formError = null,
            )
        }
    }

    private fun updateDate(invoiceDateMillis: Long?) {
        update {
            copy(
                invoiceDateMillis = invoiceDateMillis,
                errors = errors.copy(invoiceDate = null, dueDate = null),
                formError = null,
            )
        }
    }

    private fun updateDueDate(dueDateMillis: Long?) {
        update {
            copy(
                dueDateMillis = dueDateMillis,
                errors = errors.copy(dueDate = null),
                formError = null,
            )
        }
    }

    private fun addLine() {
        if (_state.value.isSaving || _state.value.isEditingBlocked) return
        val lines = _state.value.lines + newLine()
        update { copy(lines = lines, totals = totalsFor(lines), formError = null) }
    }

    private fun removeLine(localId: String) {
        if (_state.value.isSaving || _state.value.isEditingBlocked) return
        val lines = _state.value.lines.filterNot { it.localId == localId }
        update { copy(lines = lines, totals = totalsFor(lines), formError = null) }
    }

    private fun selectMaterial(localId: String, materialId: String) {
        val material = _state.value.materialOptions.firstOrNull { it.id == materialId }
        val lines = _state.value.lines.map { line ->
            if (line.localId != localId) {
                line
            } else if (material == null) {
                calculateLine(line.copy(materialId = null))
            } else {
                calculateLine(
                    line.copy(
                        materialId = material.id,
                        description = material.name,
                        unit = material.unit,
                    ),
                )
            }
        }
        update { copy(lines = lines, totals = totalsFor(lines), formError = null) }
    }

    private fun changeLine(event: InvoiceFormUiEvent.LineChanged) {
        val lines = _state.value.lines.map { line ->
            if (line.localId != event.localId) line else calculateLine(
                line.copy(
                    description = event.description ?: line.description,
                    quantity = event.quantity ?: line.quantity,
                    unit = event.unit ?: line.unit,
                    unitPrice = event.unitPrice ?: line.unitPrice,
                    discountPercent = event.discountPercent ?: line.discountPercent,
                    taxPercent = event.taxPercent ?: line.taxPercent,
                ),
            )
        }
        update { copy(lines = lines, totals = totalsFor(lines), formError = null) }
    }

    private fun calculateLine(line: EditableInvoiceLine): EditableInvoiceLine {
        val errors = InvoiceLineFieldErrors(
            material = if (line.materialId == null) InvoiceFieldError.REQUIRED else null,
            description = if (line.description.isBlank()) InvoiceFieldError.REQUIRED else null,
            quantity = when {
                line.quantity == null -> InvoiceFieldError.REQUIRED
                line.quantity <= BigDecimal.ZERO -> InvoiceFieldError.INVALID_QUANTITY
                else -> null
            },
            unit = if (line.unit.isBlank()) InvoiceFieldError.INVALID_UNIT else null,
            unitPrice = when {
                line.unitPrice == null -> InvoiceFieldError.REQUIRED
                line.unitPrice < BigDecimal.ZERO -> InvoiceFieldError.INVALID_UNIT_PRICE
                else -> null
            },
            discountPercent = percentageError(line.discountPercent),
            taxPercent = percentageError(line.taxPercent),
        )
        if (errors.hasError()) {
            return line.copy(
                lineSubtotal = null,
                lineDiscount = null,
                taxableAmount = null,
                lineTax = null,
                lineTotal = null,
                errors = errors,
            )
        }
        val calculated = calculator.calculateLine(
            InvoiceLine(
                id = line.persistedLineId ?: line.localId,
                materialId = requireNotNull(line.materialId),
                description = line.description.trim(),
                quantity = requireNotNull(line.quantity),
                unit = line.unit.trim(),
                unitPrice = requireNotNull(line.unitPrice),
                discountPercent = requireNotNull(line.discountPercent),
                taxPercent = requireNotNull(line.taxPercent),
                lineSubtotal = BigDecimal.ZERO,
                lineDiscount = BigDecimal.ZERO,
                taxableAmount = BigDecimal.ZERO,
                lineTax = BigDecimal.ZERO,
                lineTotal = BigDecimal.ZERO,
                sortOrder = 0,
            ),
        )
        return line.copy(
            description = calculated.description,
            unit = calculated.unit,
            lineSubtotal = calculated.lineSubtotal,
            lineDiscount = calculated.lineDiscount,
            taxableAmount = calculated.taxableAmount,
            lineTax = calculated.lineTax,
            lineTotal = calculated.lineTotal,
            errors = InvoiceLineFieldErrors(),
        )
    }

    private fun totalsFor(lines: List<EditableInvoiceLine>): InvoiceFormTotals? {
        if (lines.isEmpty() || lines.any { it.errors.hasError() || it.lineTotal == null }) return null
        val calculated = calculator.calculate(lines.mapIndexed { index, line ->
            InvoiceLine(
                id = line.persistedLineId ?: line.localId,
                materialId = requireNotNull(line.materialId),
                description = line.description,
                quantity = requireNotNull(line.quantity),
                unit = line.unit,
                unitPrice = requireNotNull(line.unitPrice),
                discountPercent = requireNotNull(line.discountPercent),
                taxPercent = requireNotNull(line.taxPercent),
                lineSubtotal = BigDecimal.ZERO,
                lineDiscount = BigDecimal.ZERO,
                taxableAmount = BigDecimal.ZERO,
                lineTax = BigDecimal.ZERO,
                lineTotal = BigDecimal.ZERO,
                sortOrder = index,
            )
        }).getOrNull() ?: return null
        return InvoiceFormTotals(
            subtotal = calculated.subtotal,
            discountTotal = calculated.discountTotal,
            taxTotal = calculated.taxTotal,
            grandTotal = calculated.grandTotal,
        )
    }

    private fun save() {
        val current = _state.value
        if (current.isLoading || current.isSaving || current.isEditingBlocked) return
        if (!canManageInvoices()) {
            sendEffect(InvoiceFormUiEffect.Unauthorized)
            return
        }
        val validated = validate(current)
        _state.value = validated
        if (validated.errors.hasError() || validated.lines.any { it.errors.hasError() } || validated.totals == null) {
            return
        }
        update { copy(isSaving = true, formError = null) }
        viewModelScope.launch {
            try {
                val result = when (val formMode = validated.mode) {
                    InvoiceFormMode.Create -> createInvoice(validated.toCreateRequest())
                    is InvoiceFormMode.EditDraft -> updateInvoice(validated.toUpdateRequest(formMode.invoiceId))
                        .map { formMode.invoiceId }
                }
                result.fold(
                    onSuccess = { invoiceId -> sendEffect(InvoiceFormUiEffect.Saved(invoiceId)) },
                    onFailure = { throwable ->
                        update { copy(isSaving = false, formError = InvoiceFormError.SAVE) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                update { copy(isSaving = false, formError = InvoiceFormError.SAVE) }
                handleError(exception)
            }
        }
    }

    private fun validate(state: InvoiceFormUiState): InvoiceFormUiState {
        val lines = state.lines.map(::calculateLine)
        val invoiceDateError = when {
            state.invoiceDateMillis == null -> InvoiceFieldError.REQUIRED
            state.invoiceDateMillis <= 0L -> InvoiceFieldError.INVALID_DATE
            else -> null
        }
        val dueDateError = when {
            state.dueDateMillis == null -> null
            state.dueDateMillis <= 0L -> InvoiceFieldError.INVALID_DATE
            state.invoiceDateMillis != null && state.dueDateMillis < state.invoiceDateMillis ->
                InvoiceFieldError.DUE_DATE_BEFORE_INVOICE_DATE
            else -> null
        }
        val customerError = when {
            state.customerId == null -> InvoiceFieldError.REQUIRED
            state.customerOptions.none { it.id == state.customerId } -> InvoiceFieldError.OUT_OF_RANGE
            else -> null
        }
        return state.copy(
            lines = lines,
            totals = totalsFor(lines),
            errors = InvoiceFormFieldErrors(
                customer = customerError,
                invoiceDate = invoiceDateError,
                dueDate = dueDateError,
                items = if (lines.isEmpty()) InvoiceFieldError.REQUIRED else null,
                lines = lines.associate { it.localId to it.errors },
            ),
        )
    }

    private fun InvoiceFormUiState.toCreateRequest(): InvoiceCreateRequest = InvoiceCreateRequest(
        customerId = requireNotNull(customerId),
        invoiceDateMillis = requireNotNull(invoiceDateMillis),
        dueDateMillis = dueDateMillis,
        lines = lines.toDomainLines(),
        remarks = remarks.trim(),
    )

    private fun InvoiceFormUiState.toUpdateRequest(invoiceId: String): InvoiceDraftUpdateRequest =
        InvoiceDraftUpdateRequest(
            invoiceId = invoiceId,
            customerId = requireNotNull(customerId),
            invoiceDateMillis = requireNotNull(invoiceDateMillis),
            dueDateMillis = dueDateMillis,
            lines = lines.map { line ->
                InvoiceDraftUpdateLine(
                    persistedLineId = line.persistedLineId,
                    materialId = requireNotNull(line.materialId),
                    description = line.description.trim(),
                    quantity = requireNotNull(line.quantity),
                    unit = line.unit.trim(),
                    unitPrice = requireNotNull(line.unitPrice),
                    discountPercent = requireNotNull(line.discountPercent),
                    taxPercent = requireNotNull(line.taxPercent),
                )
            },
            remarks = remarks.trim(),
        )

    private fun List<EditableInvoiceLine>.toDomainLines(): List<InvoiceLine> =
        mapIndexed { index, line ->
            InvoiceLine(
                id = line.persistedLineId ?: line.localId,
                materialId = requireNotNull(line.materialId),
                description = line.description.trim(),
                quantity = requireNotNull(line.quantity),
                unit = line.unit.trim(),
                unitPrice = requireNotNull(line.unitPrice),
                discountPercent = requireNotNull(line.discountPercent),
                taxPercent = requireNotNull(line.taxPercent),
                lineSubtotal = requireNotNull(line.lineSubtotal),
                lineDiscount = requireNotNull(line.lineDiscount),
                taxableAmount = requireNotNull(line.taxableAmount),
                lineTax = requireNotNull(line.lineTax),
                lineTotal = requireNotNull(line.lineTotal),
                sortOrder = index,
            )
        }

    private fun percentageError(value: BigDecimal?): InvoiceFieldError? = when {
        value == null -> InvoiceFieldError.REQUIRED
        value < BigDecimal.ZERO || value > HUNDRED -> InvoiceFieldError.OUT_OF_RANGE
        else -> null
    }

    private fun InvoiceLineFieldErrors.hasError(): Boolean =
        material != null || description != null || quantity != null || unit != null ||
            unitPrice != null || discountPercent != null || taxPercent != null

    private fun InvoiceFormFieldErrors.hasError(): Boolean =
        customer != null || invoiceDate != null || dueDate != null || items != null ||
            lines.values.any { it.hasError() }

    private fun canManageInvoices(): Boolean =
        ((sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user)
            ?.let { it.active && it.role == UserRole.ADMIN }
            ?: false

    private fun handleError(throwable: Throwable) {
        val message = InvoicePresentationErrorMapper.map(throwable)
        if (message.unauthorized) {
            sendEffect(InvoiceFormUiEffect.Unauthorized)
        } else {
            sendEffect(InvoiceFormUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun update(transform: InvoiceFormUiState.() -> InvoiceFormUiState) {
        _state.update(transform)
    }

    private fun sendEffect(effect: InvoiceFormUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }

    private companion object {
        const val INVOICE_ID_ARGUMENT = "invoiceId"
        val HUNDRED = BigDecimal("100")

        fun newLine(): EditableInvoiceLine = EditableInvoiceLine(
            localId = UUID.randomUUID().toString(),
            persistedLineId = null,
            materialId = null,
            description = "",
            quantity = null,
            unit = "",
            unitPrice = null,
            discountPercent = BigDecimal.ZERO,
            taxPercent = BigDecimal.ZERO,
            lineSubtotal = null,
            lineDiscount = null,
            taxableAmount = null,
            lineTax = null,
            lineTotal = null,
        )
    }
}
