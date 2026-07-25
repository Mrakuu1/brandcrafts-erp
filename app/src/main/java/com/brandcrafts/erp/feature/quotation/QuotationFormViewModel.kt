package com.brandcrafts.erp.feature.quotation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationDraft
import com.brandcrafts.erp.domain.model.QuotationDraftLine
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.ObserveInventoryItemsUseCase
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.quotation.CreateQuotationUseCase
import com.brandcrafts.erp.domain.usecase.quotation.GetQuotationUseCase
import com.brandcrafts.erp.domain.usecase.quotation.QuotationCalculationLine
import com.brandcrafts.erp.domain.usecase.quotation.QuotationCalculator
import com.brandcrafts.erp.domain.usecase.quotation.QuotationTotals
import com.brandcrafts.erp.domain.usecase.quotation.UpdateQuotationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class QuotationFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeContacts: ObserveContactsUseCase,
    private val observeInventory: ObserveInventoryItemsUseCase,
    private val getQuotation: GetQuotationUseCase,
    private val createQuotation: CreateQuotationUseCase,
    private val updateQuotation: UpdateQuotationUseCase,
    private val calculator: QuotationCalculator,
    private val session: SessionManager,
) : ViewModel() {
    private val savedQuotationId = savedStateHandle.get<String>(QUOTATION_ID_ARGUMENT)?.trim()?.takeIf(String::isNotEmpty)
    private val mode = if (savedStateHandle.get<String>(MODE_ARGUMENT) == EDIT_MODE || savedQuotationId != null) {
        QuotationFormMode.EDIT
    } else {
        QuotationFormMode.CREATE
    }
    private val quotationId = savedQuotationId
    private val _state = MutableStateFlow(QuotationFormUiState(mode = mode, quotationId = quotationId))
    val state = _state.asStateFlow()

    private val channel = Channel<QuotationFormUiEffect>(Channel.BUFFERED)
    val effects = channel.receiveAsFlow()

    private var contactsJob: Job? = null
    private var inventoryJob: Job? = null
    private var loadJob: Job? = null
    private var contactsReady = false
    private var inventoryReady = false
    private var quotationReady = mode == QuotationFormMode.CREATE
    private var blockedEffectSent = false

    init {
        initialize()
    }

    fun onEvent(event: QuotationFormUiEvent) {
        when (event) {
            is QuotationFormUiEvent.CustomerSelected -> selectCustomer(event.id)
            is QuotationFormUiEvent.ValidUntilChanged -> update {
                copy(validUntilMillis = event.value, validUntilError = null, error = null)
            }
            is QuotationFormUiEvent.NotesChanged -> update { copy(notes = event.value, error = null) }
            QuotationFormUiEvent.AddLine -> update { copy(lines = lines + EditableQuotationLine(), totals = totalsFor(lines)) }
            is QuotationFormUiEvent.RemoveLine -> removeLine(event.id)
            is QuotationFormUiEvent.InventorySelected -> selectInventory(event)
            is QuotationFormUiEvent.LineChanged -> changeLine(event)
            QuotationFormUiEvent.Save -> save()
            QuotationFormUiEvent.Retry -> retryInitialization()
            QuotationFormUiEvent.Back -> emit(QuotationFormUiEffect.NavigateBack)
        }
    }

    private fun initialize() {
        cancelInitializationJobs()
        val user = activeAdmin() ?: run {
            update { copy(loading = false, error = QuotationFormError.UNAUTHORIZED, blocked = true) }
            emit(QuotationFormUiEffect.Unauthorized)
            return
        }
        contactsReady = false
        inventoryReady = false
        quotationReady = mode == QuotationFormMode.CREATE
        update { copy(loading = true, error = null, blocked = false) }
        observeOptions()
        if (mode == QuotationFormMode.EDIT) {
            val id = quotationId
            if (id == null) {
                setLoadError()
            } else {
                loadQuotation(id, user.uid)
            }
        } else {
            finishLoadingWhenReady()
        }
    }

    private fun retryInitialization() {
        if (_state.value.saving) return
        initialize()
    }

    private fun observeOptions() {
        contactsJob = viewModelScope.launch {
            observeContacts().collect { result ->
                result.fold(
                    onSuccess = { contacts ->
                        update {
                            copy(
                                customerOptions = contacts
                                    .asSequence()
                                    .filter { it.active && it.type == ContactType.CUSTOMER }
                                    .map { contact ->
                                        QuotationCustomerOption(
                                            id = contact.id,
                                            label = if (contact.company.isBlank()) contact.name else "${contact.name} · ${contact.company}",
                                        )
                                    }
                                    .toList(),
                            )
                        }
                        contactsReady = true
                        finishLoadingWhenReady()
                    },
                    onFailure = { setLoadError() },
                )
            }
        }
        inventoryJob = viewModelScope.launch {
            observeInventory().collect { result ->
                when (result) {
                    is InventoryResult.Success -> {
                        update {
                            copy(
                                inventoryOptions = result.data
                                    .asSequence()
                                    .filter { it.active }
                                    .map { QuotationInventoryOption(it.id, it.name, it.unit) }
                                    .toList(),
                            )
                        }
                        inventoryReady = true
                        finishLoadingWhenReady()
                    }
                    is InventoryResult.Error -> setLoadError()
                }
            }
        }
    }

    private fun loadQuotation(id: String, @Suppress("UNUSED_PARAMETER") userId: String) {
        loadJob = viewModelScope.launch {
            getQuotation(id).fold(
                onSuccess = { quotation -> restoreQuotation(quotation) },
                onFailure = { setLoadError() },
            )
        }
    }

    private fun restoreQuotation(quotation: Quotation) {
        if (quotation.status != com.brandcrafts.erp.domain.model.QuotationStatus.DRAFT) {
            quotationReady = true
            update {
                copy(
                    loadedStatus = quotation.status,
                    loading = false,
                    blocked = true,
                    error = QuotationFormError.NON_DRAFT,
                )
            }
            if (!blockedEffectSent) {
                blockedEffectSent = true
                emit(QuotationFormUiEffect.EditingBlocked)
            }
            return
        }
        val restoredLines = quotation.lines.map { line ->
            recalculateLine(
                EditableQuotationLine(
                    lineId = line.id,
                    materialId = line.materialId,
                    description = line.description,
                    unit = line.unit,
                    quantity = line.quantity.toPlainString(),
                    unitPrice = line.unitPrice.toPlainString(),
                    discountPercent = line.discount.toPlainString(),
                    taxPercent = line.tax.toPlainString(),
                ),
            )
        }
        quotationReady = true
        update {
            copy(
                quotationId = quotation.id,
                loadedStatus = quotation.status,
                customerId = quotation.contactId,
                validUntilMillis = quotation.validUntilMillis,
                notes = quotation.remarks,
                lines = restoredLines,
                totals = totalsFor(restoredLines),
                error = null,
            )
        }
        finishLoadingWhenReady()
    }

    private fun finishLoadingWhenReady() {
        if (contactsReady && inventoryReady && quotationReady && _state.value.error == null) {
            update { copy(loading = false) }
        }
    }

    private fun setLoadError() {
        update { copy(loading = false, error = QuotationFormError.LOAD) }
    }

    private fun selectCustomer(id: String) {
        val isCustomer = _state.value.customerOptions.any { it.id == id }
        update {
            copy(
                customerId = id,
                customerError = if (isCustomer) null else QuotationFieldError.OUT_OF_RANGE,
                error = null,
            )
        }
    }

    private fun selectInventory(event: QuotationFormUiEvent.InventorySelected) {
        val item = _state.value.inventoryOptions.firstOrNull { it.id == event.materialId }
        val lines = _state.value.lines.map { line ->
            if (line.localId != event.id) line else recalculateLine(
                if (item == null) {
                    line.copy(materialId = event.materialId, inventoryError = QuotationFieldError.OUT_OF_RANGE)
                } else {
                    line.copy(
                        materialId = item.id,
                        description = item.name,
                        unit = item.unit,
                        inventoryError = null,
                    )
                },
            )
        }
        update { copy(lines = lines, totals = totalsFor(lines), error = null) }
    }

    private fun changeLine(event: QuotationFormUiEvent.LineChanged) {
        val lines = _state.value.lines.map { line ->
            if (line.localId != event.id) line else recalculateLine(
                line.copy(
                    quantity = event.quantity ?: line.quantity,
                    unitPrice = event.unitPrice ?: line.unitPrice,
                    discountPercent = event.discount ?: line.discountPercent,
                    taxPercent = event.tax ?: line.taxPercent,
                ),
            )
        }
        update { copy(lines = lines, totals = totalsFor(lines), error = null) }
    }

    private fun removeLine(localId: String) {
        if (_state.value.lines.size <= 1) return
        val lines = _state.value.lines.filterNot { it.localId == localId }
        update { copy(lines = lines, totals = totalsFor(lines), error = null) }
    }

    private fun recalculateLine(line: EditableQuotationLine): EditableQuotationLine {
        val quantity = line.quantity.toPositiveDecimal()
        val unitPrice = line.unitPrice.toNonNegativeDecimal()
        val discount = line.discountPercent.toPercentageDecimal()
        val tax = line.taxPercent.toPercentageDecimal()
        val inventoryError = when {
            line.materialId == null -> QuotationFieldError.REQUIRED
            line.inventoryError != null -> line.inventoryError
            else -> null
        }
        if (inventoryError != null || quantity.error != null || unitPrice.error != null || discount.error != null || tax.error != null) {
            return line.copy(
                inventoryError = inventoryError,
                quantityError = quantity.error,
                unitPriceError = unitPrice.error,
                discountError = discount.error,
                taxError = tax.error,
                subtotal = null,
                discount = null,
                taxable = null,
                tax = null,
                total = null,
            )
        }
        return runCatching {
            calculator.line(
                QuotationCalculationLine(
                    quantity = requireNotNull(quantity.value),
                    unitPrice = requireNotNull(unitPrice.value),
                    discountPercent = requireNotNull(discount.value),
                    taxPercent = requireNotNull(tax.value),
                ),
            )
        }.fold(
            onSuccess = { totals ->
                line.copy(
                    inventoryError = null,
                    quantityError = null,
                    unitPriceError = null,
                    discountError = null,
                    taxError = null,
                    subtotal = totals.subtotal,
                    discount = totals.discount,
                    taxable = totals.taxable,
                    tax = totals.tax,
                    total = totals.total,
                )
            },
            onFailure = {
                line.copy(
                    quantityError = QuotationFieldError.MALFORMED,
                    subtotal = null,
                    discount = null,
                    taxable = null,
                    tax = null,
                    total = null,
                )
            },
        )
    }

    private fun totalsFor(lines: List<EditableQuotationLine>): QuotationTotals? {
        val validLines = lines.filter { it.isValid }.map {
            QuotationCalculationLine(
                quantity = BigDecimal(it.quantity.trim()),
                unitPrice = BigDecimal(it.unitPrice.trim()),
                discountPercent = BigDecimal(it.discountPercent.trim()),
                taxPercent = BigDecimal(it.taxPercent.trim()),
            )
        }
        return validLines.takeIf { it.isNotEmpty() }?.let(calculator::totals)
    }

    private fun save() {
        val current = _state.value
        if (current.saving || current.loading || current.blocked || activeAdmin() == null) return
        val validated = validateForSave(current)
        _state.value = validated
        if (validated.customerError != null || validated.validUntilError != null || validated.lines.any { !it.isValid }) {
            update { copy(error = QuotationFormError.VALIDATION) }
            return
        }
        val draft = validated.toDraft() ?: run {
            update { copy(error = QuotationFormError.VALIDATION) }
            return
        }
        update { copy(saving = true, error = null) }
        viewModelScope.launch {
            if (activeAdmin() == null) {
                update { copy(saving = false, error = QuotationFormError.UNAUTHORIZED) }
                emit(QuotationFormUiEffect.Unauthorized)
                return@launch
            }
            val result = when (validated.mode) {
                QuotationFormMode.CREATE -> createQuotation(draft)
                QuotationFormMode.EDIT -> {
                    val id = validated.quotationId
                    if (id == null || validated.loadedStatus != com.brandcrafts.erp.domain.model.QuotationStatus.DRAFT) {
                        Result.failure(IllegalStateException())
                    } else {
                        updateQuotation(id, draft).map { id }
                    }
                }
            }
            result.fold(
                onSuccess = { id -> emit(QuotationFormUiEffect.Saved(id)) },
                onFailure = { update { copy(saving = false, error = QuotationFormError.SAVE) } },
            )
        }
    }

    private fun validateForSave(state: QuotationFormUiState): QuotationFormUiState {
        val lines = state.lines.map(::recalculateLine)
        val customerError = when {
            state.customerId == null -> QuotationFieldError.REQUIRED
            state.customerOptions.none { it.id == state.customerId } -> QuotationFieldError.OUT_OF_RANGE
            else -> null
        }
        val validUntilError = if (state.validUntilMillis == null) QuotationFieldError.REQUIRED else null
        return state.copy(
            lines = lines,
            totals = totalsFor(lines),
            customerError = customerError,
            validUntilError = validUntilError,
        )
    }

    private fun QuotationFormUiState.toDraft(): QuotationDraft? = runCatching {
        QuotationDraft(
            contactId = requireNotNull(customerId),
            validUntilMillis = requireNotNull(validUntilMillis),
            remarks = notes.trim(),
            lines = lines.map { line ->
                QuotationDraftLine(
                    id = line.lineId,
                    materialId = requireNotNull(line.materialId),
                    description = line.description,
                    quantity = BigDecimal(line.quantity.trim()),
                    unit = line.unit,
                    unitPrice = BigDecimal(line.unitPrice.trim()),
                    discountPercent = BigDecimal(line.discountPercent.trim()),
                    taxPercent = BigDecimal(line.taxPercent.trim()),
                )
            },
        )
    }.getOrNull()

    private fun String.toPositiveDecimal(): DecimalValidation {
        val value = trim()
        if (value.isBlank()) return DecimalValidation(error = QuotationFieldError.REQUIRED)
        val decimal = value.toBigDecimalOrNull() ?: return DecimalValidation(error = QuotationFieldError.MALFORMED)
        return if (decimal > BigDecimal.ZERO) DecimalValidation(decimal) else DecimalValidation(error = QuotationFieldError.OUT_OF_RANGE)
    }

    private fun String.toNonNegativeDecimal(): DecimalValidation {
        val value = trim()
        if (value.isBlank()) return DecimalValidation(error = QuotationFieldError.REQUIRED)
        val decimal = value.toBigDecimalOrNull() ?: return DecimalValidation(error = QuotationFieldError.MALFORMED)
        return if (decimal >= BigDecimal.ZERO) DecimalValidation(decimal) else DecimalValidation(error = QuotationFieldError.OUT_OF_RANGE)
    }

    private fun String.toPercentageDecimal(): DecimalValidation {
        val value = trim()
        if (value.isBlank()) return DecimalValidation(error = QuotationFieldError.REQUIRED)
        val decimal = value.toBigDecimalOrNull() ?: return DecimalValidation(error = QuotationFieldError.MALFORMED)
        return if (decimal >= BigDecimal.ZERO && decimal <= HUNDRED) DecimalValidation(decimal) else DecimalValidation(error = QuotationFieldError.OUT_OF_RANGE)
    }

    private fun activeAdmin() = (session.currentUser.value as? CurrentUserState.Authenticated)
        ?.user
        ?.takeIf { it.active && it.role == UserRole.ADMIN }

    private fun cancelInitializationJobs() {
        contactsJob?.cancel()
        inventoryJob?.cancel()
        loadJob?.cancel()
    }

    private fun update(transform: QuotationFormUiState.() -> QuotationFormUiState) {
        _state.value = _state.value.transform()
    }

    private fun emit(effect: QuotationFormUiEffect) {
        viewModelScope.launch { channel.send(effect) }
    }

    private data class DecimalValidation(
        val value: BigDecimal? = null,
        val error: QuotationFieldError? = null,
    )

    private companion object {
        const val MODE_ARGUMENT = "mode"
        const val QUOTATION_ID_ARGUMENT = "quotationId"
        const val EDIT_MODE = "edit"
        val HUNDRED = BigDecimal("100")
    }
}
