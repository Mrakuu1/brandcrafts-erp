package com.brandcrafts.erp.feature.invoice

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.ContactError
import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.Invoice
import com.brandcrafts.erp.domain.model.InvoicePdfError
import com.brandcrafts.erp.domain.model.InvoicePdfFailure
import com.brandcrafts.erp.domain.model.InvoicePaymentRequest
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.contact.GetContactUseCase
import com.brandcrafts.erp.domain.usecase.invoice.CancelInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.invoice.GetInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.invoice.GenerateInvoicePdfUseCase
import com.brandcrafts.erp.domain.usecase.invoice.IssueInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.invoice.RecordInvoicePaymentUseCase
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
class InvoiceDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getInvoice: GetInvoiceUseCase,
    private val getContact: GetContactUseCase,
    private val issueInvoice: IssueInvoiceUseCase,
    private val cancelInvoice: CancelInvoiceUseCase,
    private val recordInvoicePayment: RecordInvoicePaymentUseCase,
    private val generateInvoicePdf: GenerateInvoicePdfUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val invoiceId = savedStateHandle.get<String>(INVOICE_ID_ARGUMENT)
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    private val _state = MutableStateFlow(InvoiceDetailsUiState())
    val state = _state.asStateFlow()

    private val effectsChannel = Channel<InvoiceDetailsUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var loadJob: Job? = null
    private var pendingIssue = false
    private var pendingCancellation = false
    private var currentInvoice: Invoice? = null

    init {
        load()
    }

    fun onEvent(event: InvoiceDetailsUiEvent) {
        when (event) {
            InvoiceDetailsUiEvent.Retry -> load()
            InvoiceDetailsUiEvent.Back -> sendEffect(InvoiceDetailsUiEffect.NavigateBack)
            InvoiceDetailsUiEvent.EditClicked -> requestEdit()
            InvoiceDetailsUiEvent.IssueClicked -> requestIssue()
            InvoiceDetailsUiEvent.IssueConfirmed -> issue()
            InvoiceDetailsUiEvent.CancelClicked -> requestCancellation()
            InvoiceDetailsUiEvent.CancelConfirmed -> cancel()
            InvoiceDetailsUiEvent.RecordPaymentClicked -> openPaymentEntry()
            InvoiceDetailsUiEvent.PreviewPdfClicked -> generatePdf(preview = true)
            InvoiceDetailsUiEvent.SharePdfClicked -> generatePdf(preview = false)
            is InvoiceDetailsUiEvent.PaymentAmountChanged -> updatePaymentAmount(event.amountInput)
            InvoiceDetailsUiEvent.PaymentSubmitClicked -> submitPayment()
            InvoiceDetailsUiEvent.PaymentDismissed -> {
                if (_state.value.operationInProgress != InvoiceDetailsOperation.RECORD_PAYMENT) {
                    _state.update { it.copy(paymentEntry = null) }
                }
            }
        }
    }

    private fun load() {
        val id = invoiceId
        if (id == null) {
            _state.update { it.copy(content = InvoiceDetailsContent.Error()) }
            handleError(InvoiceFailure(InvoiceError.InvoiceNotFound), emitMessage = false)
            return
        }
        if (_state.value.operationInProgress != null) return
        loadJob?.cancel()
        _state.update { it.copy(content = InvoiceDetailsContent.Loading, paymentEntry = null) }
        loadJob = viewModelScope.launch {
            try {
                getInvoice(id).fold(
                    onSuccess = { invoice -> loadCustomerAndPublish(invoice) },
                    onFailure = { throwable -> setLoadError(throwable) },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(exception)
            }
        }
    }

    private suspend fun loadCustomerAndPublish(invoice: Invoice) {
        val customer = when (val result = getContact(invoice.customerId)) {
            is ContactResult.Success -> {
                val contact = result.data
                if (contact.type != ContactType.CUSTOMER) {
                    throw InvoiceFailure(InvoiceError.ContactIsNotCustomer)
                }
                InvoiceCustomerOption(
                    id = contact.id,
                    label = contact.company.ifBlank { contact.name },
                )
            }
            is ContactResult.Error -> throw InvoiceFailure(result.error.toInvoiceError())
        }
        currentInvoice = invoice
        _state.update {
            it.copy(
                content = InvoiceDetailsContent.Loaded,
                invoice = invoice.toDetailsModel(customer),
                canEdit = canManageInvoices() && invoice.status == InvoiceStatus.DRAFT,
                canIssue = canManageInvoices() && invoice.status == InvoiceStatus.DRAFT,
                canCancel = canManageInvoices() && invoice.status in setOf(InvoiceStatus.DRAFT, InvoiceStatus.ISSUED) && invoice.paidAmount.signum() == 0,
                canRecordPayment = canManageInvoices() && invoice.status == InvoiceStatus.ISSUED && invoice.outstandingAmount.signum() > 0,
                paymentEntry = null,
            )
        }
    }

    private fun requestEdit() {
        val invoice = currentInvoice ?: return missingInvoice()
        when {
            !canManageInvoices() -> unauthorized()
            invoice.status != InvoiceStatus.DRAFT -> sendMessage(InvoiceError.DraftOnlyUpdateRequired)
            else -> sendEffect(InvoiceDetailsUiEffect.NavigateEditDraft(invoice.id))
        }
    }

    private fun requestIssue() {
        val invoice = currentInvoice ?: return missingInvoice()
        when {
            !canManageInvoices() -> unauthorized()
            invoice.status != InvoiceStatus.DRAFT -> sendMessage(InvoiceError.InvalidStatusTransition)
            isOperating() -> Unit
            else -> {
                pendingIssue = true
                sendEffect(InvoiceDetailsUiEffect.ConfirmIssue)
            }
        }
    }

    private fun issue() {
        val invoice = currentInvoice ?: return missingInvoice()
        if (!pendingIssue) return
        pendingIssue = false
        if (_state.value.operationInProgress != null) return
        when {
            !canManageInvoices() -> unauthorized()
            invoice.status != InvoiceStatus.DRAFT -> sendMessage(InvoiceError.InvalidStatusTransition)
            else -> performOperation(InvoiceDetailsOperation.ISSUE) { issueInvoice(invoice.id) }
        }
    }

    private fun requestCancellation() {
        val invoice = currentInvoice ?: return missingInvoice()
        when {
            !canManageInvoices() -> unauthorized()
            invoice.status == InvoiceStatus.CANCELLED -> sendMessage(InvoiceError.InvalidStatusTransition)
            invoice.status !in setOf(InvoiceStatus.DRAFT, InvoiceStatus.ISSUED) -> sendMessage(InvoiceError.InvalidStatusTransition)
            invoice.paidAmount.signum() != 0 -> sendMessage(InvoiceError.InvoiceHasRecordedPayments)
            isOperating() -> Unit
            else -> {
                pendingCancellation = true
                sendEffect(InvoiceDetailsUiEffect.ConfirmCancellation)
            }
        }
    }

    private fun cancel() {
        val invoice = currentInvoice ?: return missingInvoice()
        if (!pendingCancellation) return
        pendingCancellation = false
        if (_state.value.operationInProgress != null) return
        when {
            !canManageInvoices() -> unauthorized()
            invoice.status == InvoiceStatus.CANCELLED -> sendMessage(InvoiceError.InvalidStatusTransition)
            invoice.paidAmount.signum() != 0 -> sendMessage(InvoiceError.InvoiceHasRecordedPayments)
            else -> performOperation(InvoiceDetailsOperation.CANCEL) { cancelInvoice(invoice.id) }
        }
    }

    private fun openPaymentEntry() {
        val invoice = currentInvoice ?: return missingInvoice()
        when {
            !canManageInvoices() -> unauthorized()
            invoice.status != InvoiceStatus.ISSUED || invoice.outstandingAmount.signum() <= 0 ->
                sendMessage(InvoiceError.PaymentNotAllowedForCurrentStatus)
            isOperating() -> Unit
            else -> _state.update {
                it.copy(
                    paymentEntry = InvoicePaymentEntryUiState(
                        invoiceId = invoice.id,
                        grandTotal = invoice.grandTotal,
                        paidAmount = invoice.paidAmount,
                        outstandingAmount = invoice.outstandingAmount,
                        paymentStatus = invoice.paymentStatus,
                    ),
                )
            }
        }
    }

    private fun updatePaymentAmount(amountInput: String) {
        val entry = _state.value.paymentEntry ?: return
        if (entry.isSaving) return
        val normalizedInput = amountInput.trim()
        val amount = normalizedInput.takeIf(String::isNotEmpty)?.toBigDecimalOrNull()
        val error = when {
            normalizedInput.isEmpty() -> InvoicePaymentEntryError.REQUIRED
            amount == null -> InvoicePaymentEntryError.INVALID_AMOUNT
            amount <= BigDecimal.ZERO -> InvoicePaymentEntryError.INVALID_AMOUNT
            amount > entry.outstandingAmount -> InvoicePaymentEntryError.EXCEEDS_OUTSTANDING
            else -> null
        }
        _state.update {
            it.copy(
                paymentEntry = entry.copy(
                    amountInput = amountInput,
                    amount = amount,
                    amountError = error,
                ),
            )
        }
    }

    private fun submitPayment() {
        val invoice = currentInvoice ?: return missingInvoice()
        val entry = _state.value.paymentEntry ?: return
        if (isOperating() || entry.isSaving) return
        val amountError = when {
            entry.amount == null -> InvoicePaymentEntryError.REQUIRED
            entry.amount <= BigDecimal.ZERO -> InvoicePaymentEntryError.INVALID_AMOUNT
            entry.amount > entry.outstandingAmount -> InvoicePaymentEntryError.EXCEEDS_OUTSTANDING
            else -> null
        }
        if (amountError != null) {
            _state.update { it.copy(paymentEntry = entry.copy(amountError = amountError)) }
            return
        }
        when {
            !canManageInvoices() -> unauthorized()
            invoice.status != InvoiceStatus.ISSUED || invoice.outstandingAmount.signum() <= 0 ->
                sendMessage(InvoiceError.PaymentNotAllowedForCurrentStatus)
            else -> {
                val paymentAmount = requireNotNull(entry.amount)
                _state.update {
                    it.copy(
                        operationInProgress = InvoiceDetailsOperation.RECORD_PAYMENT,
                        paymentEntry = entry.copy(isSaving = true, amountError = null),
                    )
                }
                viewModelScope.launch {
                    try {
                        recordInvoicePayment(InvoicePaymentRequest(invoice.id, paymentAmount)).fold(
                            onSuccess = {
                                _state.update {
                                    it.copy(
                                        operationInProgress = null,
                                        paymentEntry = null,
                                    )
                                }
                                sendEffect(InvoiceDetailsUiEffect.ShowMessage(R.string.invoice_payment_recorded_success))
                                load()
                            },
                            onFailure = { throwable ->
                                _state.update {
                                    it.copy(
                                        operationInProgress = null,
                                        paymentEntry = entry.copy(isSaving = false),
                                    )
                                }
                                handleError(throwable)
                            },
                        )
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Throwable) {
                        _state.update {
                            it.copy(
                                operationInProgress = null,
                                paymentEntry = entry.copy(isSaving = false),
                            )
                        }
                        handleError(exception)
                    }
                }
            }
        }
    }

    private fun performOperation(
        operation: InvoiceDetailsOperation,
        action: suspend () -> Result<Unit>,
    ) {
        _state.update { it.copy(operationInProgress = operation) }
        viewModelScope.launch {
            try {
                action().fold(
                    onSuccess = {
                        _state.update { it.copy(operationInProgress = null) }
                        sendEffect(
                            InvoiceDetailsUiEffect.ShowMessage(
                                if (operation == InvoiceDetailsOperation.ISSUE) {
                                    R.string.invoice_issued_success
                                } else {
                                    R.string.invoice_cancelled_success
                                },
                            ),
                        )
                        load()
                    },
                    onFailure = { throwable ->
                        _state.update { it.copy(operationInProgress = null) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _state.update { it.copy(operationInProgress = null) }
                handleError(exception)
            }
        }
    }

    private fun generatePdf(preview: Boolean) {
        val invoice = currentInvoice ?: return missingInvoice()
        if (isOperating()) return
        _state.update { it.copy(isPdfGenerating = true) }
        viewModelScope.launch {
            try {
                generateInvoicePdf(invoice.id).fold(
                    onSuccess = { pdf ->
                        _state.update { it.copy(isPdfGenerating = false) }
                        sendEffect(
                            if (preview) {
                                InvoiceDetailsUiEffect.PreviewPdf(pdf.cacheFileName)
                            } else {
                                InvoiceDetailsUiEffect.SharePdf(pdf.cacheFileName)
                            },
                        )
                    },
                    onFailure = { throwable ->
                        _state.update { it.copy(isPdfGenerating = false) }
                        handlePdfError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _state.update { it.copy(isPdfGenerating = false) }
                handlePdfError(exception)
            }
        }
    }

    private fun handlePdfError(throwable: Throwable) {
        val messageRes = when ((throwable as? InvoicePdfFailure)?.error) {
            InvoicePdfError.CompanyConfigurationIncomplete -> R.string.invoice_pdf_company_configuration_error
            InvoicePdfError.CustomerUnavailable -> R.string.invoice_error_customer_not_found
            InvoicePdfError.InvoiceUnavailable -> R.string.invoice_error_not_found
            else -> R.string.invoice_pdf_generation_error
        }
        sendEffect(InvoiceDetailsUiEffect.ShowMessage(messageRes))
    }

    private fun Invoice.toDetailsModel(customer: InvoiceCustomerOption): InvoiceDetailsModel =
        InvoiceDetailsModel(
            id = id,
            invoiceNumber = number,
            customer = customer,
            invoiceDateMillis = invoiceDateMillis,
            dueDateMillis = dueDateMillis,
            status = status,
            paymentStatus = paymentStatus,
            lines = lines.map { line ->
                InvoiceDetailsLine(
                    id = line.id,
                    materialId = line.materialId,
                    description = line.description,
                    quantity = line.quantity,
                    unit = line.unit,
                    unitPrice = line.unitPrice,
                    discountPercent = line.discountPercent,
                    taxPercent = line.taxPercent,
                    lineSubtotal = line.lineSubtotal,
                    lineDiscount = line.lineDiscount,
                    taxableAmount = line.taxableAmount,
                    lineTax = line.lineTax,
                    lineTotal = line.lineTotal,
                    sortOrder = line.sortOrder,
                )
            },
            subtotal = subtotal,
            discountTotal = discountTotal,
            taxTotal = taxTotal,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            outstandingAmount = outstandingAmount,
            isOverdue = isOverdue(System.currentTimeMillis()),
            remarks = remarks,
            createdAtMillis = createdAtMillis,
            updatedAtMillis = updatedAtMillis,
            issuedAtMillis = issuedAtMillis,
            cancelledAtMillis = cancelledAtMillis,
        )

    private fun ContactError.toInvoiceError(): InvoiceError = when (this) {
        ContactError.CONTACT_NOT_FOUND -> InvoiceError.CustomerNotFound
        ContactError.UNAUTHORIZED -> InvoiceError.PermissionDenied
        ContactError.NETWORK_UNAVAILABLE -> InvoiceError.FirestoreUnavailable
        else -> InvoiceError.RepositoryUnavailable
    }

    private fun setLoadError(throwable: Throwable) {
        currentInvoice = null
        _state.update {
            it.copy(
                content = InvoiceDetailsContent.Error(),
                invoice = null,
                canEdit = false,
                canIssue = false,
                canCancel = false,
                canRecordPayment = false,
                paymentEntry = null,
            )
        }
        handleError(throwable, emitMessage = false)
    }

    private fun missingInvoice() {
        handleError(InvoiceFailure(InvoiceError.InvoiceNotFound))
    }

    private fun unauthorized() {
        sendEffect(InvoiceDetailsUiEffect.Unauthorized)
    }

    private fun isOperating(): Boolean =
        _state.value.operationInProgress != null || _state.value.isPdfGenerating || pendingIssue || pendingCancellation

    private fun canManageInvoices(): Boolean =
        ((sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user)
            ?.let { it.active && it.role == UserRole.ADMIN }
            ?: false

    private fun handleError(throwable: Throwable, emitMessage: Boolean = true) {
        val message = InvoicePresentationErrorMapper.map(throwable)
        if (message.unauthorized) {
            sendEffect(InvoiceDetailsUiEffect.Unauthorized)
        } else if (emitMessage) {
            sendEffect(InvoiceDetailsUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendMessage(error: InvoiceError) {
        val message = error.toPresentationMessage()
        if (message.unauthorized) {
            sendEffect(InvoiceDetailsUiEffect.Unauthorized)
        } else {
            sendEffect(InvoiceDetailsUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendEffect(effect: InvoiceDetailsUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }

    private companion object {
        const val INVOICE_ID_ARGUMENT = "invoiceId"
    }
}
