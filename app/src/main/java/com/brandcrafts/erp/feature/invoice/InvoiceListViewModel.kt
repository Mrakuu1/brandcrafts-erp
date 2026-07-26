package com.brandcrafts.erp.feature.invoice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.model.InvoiceSummary
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.invoice.CancelInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.invoice.IssueInvoiceUseCase
import com.brandcrafts.erp.domain.usecase.invoice.ObserveInvoicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
class InvoiceListViewModel @Inject constructor(
    private val observeInvoices: ObserveInvoicesUseCase,
    private val observeContacts: ObserveContactsUseCase,
    private val issueInvoice: IssueInvoiceUseCase,
    private val cancelInvoice: CancelInvoiceUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow(InvoiceListUiState())
    val state = _state.asStateFlow()

    private val effectsChannel = Channel<InvoiceListUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var invoicesJob: Job? = null
    private var contactsJob: Job? = null
    private var invoices: List<InvoiceSummary> = emptyList()
    private var customerNamesById: Map<String, String> = emptyMap()
    private var invoiceObservationFailed = false
    private var pendingIssueId: String? = null
    private var pendingCancellationId: String? = null

    init {
        observe()
    }

    fun onEvent(event: InvoiceListUiEvent) {
        when (event) {
            InvoiceListUiEvent.Refresh,
            InvoiceListUiEvent.Retry -> observe()
            is InvoiceListUiEvent.SearchChanged -> {
                _state.update { it.copy(searchQuery = event.query) }
                publishRows()
            }
            is InvoiceListUiEvent.DocumentStatusFilterChanged -> {
                _state.update { it.copy(documentStatusFilter = event.filter) }
                publishRows()
            }
            is InvoiceListUiEvent.PaymentStatusFilterChanged -> {
                _state.update { it.copy(paymentStatusFilter = event.filter) }
                publishRows()
            }
            InvoiceListUiEvent.CreateClicked -> requestCreate()
            is InvoiceListUiEvent.DetailsClicked -> openDetails(event.invoiceId)
            is InvoiceListUiEvent.EditClicked -> requestEdit(event.invoiceId)
            is InvoiceListUiEvent.IssueClicked -> requestIssue(event.invoiceId)
            InvoiceListUiEvent.IssueConfirmed -> issuePendingInvoice()
            is InvoiceListUiEvent.CancelClicked -> requestCancellation(event.invoiceId)
            InvoiceListUiEvent.CancelConfirmed -> cancelPendingInvoice()
            is InvoiceListUiEvent.RecordPaymentClicked -> requestRecordPayment(event.invoiceId)
        }
    }

    private fun observe() {
        val hasInvoices = invoices.isNotEmpty()
        invoicesJob?.cancel()
        contactsJob?.cancel()
        invoiceObservationFailed = false
        _state.update {
            it.copy(
                content = if (hasInvoices) it.content else InvoiceListContent.Loading,
                canCreate = canManageInvoices(),
                isRefreshing = hasInvoices,
            )
        }
        invoicesJob = viewModelScope.launch {
            try {
                observeInvoices().collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            invoices = summaries
                            invoiceObservationFailed = false
                            publishRows()
                        },
                        onFailure = ::handleObservationFailure,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                handleObservationFailure(exception)
            }
        }
        contactsJob = viewModelScope.launch {
            try {
                observeContacts().collect { result ->
                    result.onSuccess { contacts ->
                        customerNamesById = contacts
                            .asSequence()
                            .filter { it.type == ContactType.CUSTOMER }
                            .associate { contact ->
                                contact.id to contact.company.ifBlank { contact.name }
                            }
                        publishRows()
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            }
        }
    }

    private fun handleObservationFailure(throwable: Throwable) {
        invoiceObservationFailed = true
        _state.update {
            it.copy(
                content = InvoiceListContent.Error(),
                isRefreshing = false,
            )
        }
        handleError(throwable, emitMessage = false)
    }

    private fun publishRows() {
        val current = _state.value
        val query = current.searchQuery.trim()
        val rows = invoices
            .asSequence()
            .filter { summary ->
                val statusMatches = when (val filter = current.documentStatusFilter) {
                    InvoiceDocumentStatusFilter.All -> true
                    is InvoiceDocumentStatusFilter.Status -> summary.status == filter.value
                }
                val paymentMatches = when (val filter = current.paymentStatusFilter) {
                    InvoicePaymentStatusFilter.All -> true
                    is InvoicePaymentStatusFilter.Status -> summary.paymentStatus == filter.value
                }
                val customerName = customerNamesById[summary.customerId] ?: summary.customerName
                statusMatches && paymentMatches && (
                    query.isBlank() ||
                        summary.number.contains(query, ignoreCase = true) ||
                        customerName?.contains(query, ignoreCase = true) == true
                    )
            }
            .map { summary: InvoiceSummary -> summary.toListItem() }
            .toList()

        _state.update {
            it.copy(
                content = if (invoiceObservationFailed) {
                    InvoiceListContent.Error()
                } else if (rows.isEmpty()) {
                    InvoiceListContent.Empty
                } else {
                    InvoiceListContent.Loaded
                },
                rows = rows,
                canCreate = canManageInvoices(),
                isRefreshing = false,
            )
        }
    }

    private fun InvoiceSummary.toListItem(): InvoiceListItem {
        val canManage = canManageInvoices()
        val canEdit = canManage && status == InvoiceStatus.DRAFT
        val canIssue = canManage && status == InvoiceStatus.DRAFT
        val canCancel = canManage && when (status) {
            InvoiceStatus.DRAFT -> paidAmount.signum() == 0
            InvoiceStatus.ISSUED -> paidAmount.signum() == 0
            InvoiceStatus.CANCELLED -> false
        }
        val canRecordPayment = canManage &&
            status == InvoiceStatus.ISSUED &&
            outstandingAmount.signum() > 0
        return InvoiceListItem(
            id = id,
            invoiceNumber = number,
            customerId = customerId,
            customerName = customerNamesById[customerId] ?: customerName,
            invoiceDateMillis = invoiceDateMillis,
            dueDateMillis = dueDateMillis,
            status = status,
            paymentStatus = paymentStatus,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            outstandingAmount = outstandingAmount,
            isOverdue = isOverdue(System.currentTimeMillis()),
            canEdit = canEdit,
            canIssue = canIssue,
            canCancel = canCancel,
            canRecordPayment = canRecordPayment,
        )
    }

    private fun requestCreate() {
        if (!canManageInvoices()) {
            sendMessage(InvoiceError.AdminAccessRequired)
            return
        }
        sendEffect(InvoiceListUiEffect.NavigateCreate)
    }

    private fun openDetails(invoiceId: String) {
        if (invoiceId.isBlank() || invoices.none { it.id == invoiceId }) {
            sendMessage(InvoiceError.InvoiceNotFound)
        } else {
            sendEffect(InvoiceListUiEffect.NavigateDetails(invoiceId))
        }
    }

    private fun requestEdit(invoiceId: String) {
        val invoice = invoiceById(invoiceId) ?: return
        when {
            !canManageInvoices() -> sendMessage(InvoiceError.AdminAccessRequired)
            invoice.status != InvoiceStatus.DRAFT -> sendMessage(InvoiceError.DraftOnlyUpdateRequired)
            else -> sendEffect(InvoiceListUiEffect.NavigateEditDraft(invoiceId))
        }
    }

    private fun requestIssue(invoiceId: String) {
        val invoice = invoiceById(invoiceId) ?: return
        when {
            !canManageInvoices() -> sendMessage(InvoiceError.AdminAccessRequired)
            invoice.status != InvoiceStatus.DRAFT -> sendMessage(InvoiceError.InvalidStatusTransition)
            isOperating(invoiceId) -> Unit
            else -> {
                pendingIssueId = invoiceId
                sendEffect(InvoiceListUiEffect.ConfirmIssue(invoiceId))
            }
        }
    }

    private fun issuePendingInvoice() {
        val invoiceId = pendingIssueId ?: return
        pendingIssueId = null
        val invoice = invoiceById(invoiceId) ?: return
        if (!canManageInvoices()) {
            sendMessage(InvoiceError.AdminAccessRequired)
        } else if (invoice.status != InvoiceStatus.DRAFT) {
            sendMessage(InvoiceError.InvalidStatusTransition)
        } else if (!isOperating(invoiceId)) {
            performOperation(invoiceId, InvoiceListAction.ISSUE) { issueInvoice(invoiceId) }
        }
    }

    private fun requestCancellation(invoiceId: String) {
        val invoice = invoiceById(invoiceId) ?: return
        when {
            !canManageInvoices() -> sendMessage(InvoiceError.AdminAccessRequired)
            invoice.status == InvoiceStatus.CANCELLED -> sendMessage(InvoiceError.InvalidStatusTransition)
            invoice.status !in setOf(InvoiceStatus.DRAFT, InvoiceStatus.ISSUED) -> sendMessage(InvoiceError.InvalidStatusTransition)
            invoice.paidAmount.signum() != 0 -> sendMessage(InvoiceError.InvoiceHasRecordedPayments)
            isOperating(invoiceId) -> Unit
            else -> {
                pendingCancellationId = invoiceId
                sendEffect(InvoiceListUiEffect.ConfirmCancellation(invoiceId))
            }
        }
    }

    private fun cancelPendingInvoice() {
        val invoiceId = pendingCancellationId ?: return
        pendingCancellationId = null
        val invoice = invoiceById(invoiceId) ?: return
        if (!canManageInvoices()) {
            sendMessage(InvoiceError.AdminAccessRequired)
        } else if (invoice.status == InvoiceStatus.CANCELLED || invoice.paidAmount.signum() != 0) {
            sendMessage(if (invoice.paidAmount.signum() != 0) InvoiceError.InvoiceHasRecordedPayments else InvoiceError.InvalidStatusTransition)
        } else if (!isOperating(invoiceId)) {
            performOperation(invoiceId, InvoiceListAction.CANCEL) { cancelInvoice(invoiceId) }
        }
    }

    private fun requestRecordPayment(invoiceId: String) {
        val invoice = invoiceById(invoiceId) ?: return
        when {
            !canManageInvoices() -> sendMessage(InvoiceError.AdminAccessRequired)
            invoice.status != InvoiceStatus.ISSUED || invoice.outstandingAmount.signum() <= 0 -> sendMessage(InvoiceError.PaymentNotAllowedForCurrentStatus)
            else -> sendEffect(InvoiceListUiEffect.NavigateRecordPayment(invoiceId))
        }
    }

    private fun performOperation(
        invoiceId: String,
        action: InvoiceListAction,
        operation: suspend () -> Result<Unit>,
    ) {
        _state.update { it.copy(actionInProgress = InvoiceListOperation(invoiceId, action)) }
        viewModelScope.launch {
            try {
                operation().fold(
                    onSuccess = {
                        _state.update { it.copy(actionInProgress = null) }
                        sendEffect(
                            InvoiceListUiEffect.ShowMessage(
                                if (action == InvoiceListAction.ISSUE) {
                                    R.string.invoice_issued_success
                                } else {
                                    R.string.invoice_cancelled_success
                                },
                            ),
                        )
                    },
                    onFailure = { throwable ->
                        _state.update { it.copy(actionInProgress = null) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _state.update { it.copy(actionInProgress = null) }
                handleError(exception)
            }
        }
    }

    private fun invoiceById(invoiceId: String): InvoiceSummary? =
        invoices.firstOrNull { it.id == invoiceId }
            ?: run {
                sendMessage(InvoiceError.InvoiceNotFound)
                null
            }

    private fun isOperating(invoiceId: String): Boolean =
        _state.value.actionInProgress?.invoiceId == invoiceId ||
            pendingIssueId == invoiceId ||
            pendingCancellationId == invoiceId

    private fun canManageInvoices(): Boolean =
        ((sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user)
            ?.let { it.active && it.role == UserRole.ADMIN }
            ?: false

    private fun handleError(throwable: Throwable, emitMessage: Boolean = true) {
        val message = InvoicePresentationErrorMapper.map(throwable)
        if (message.unauthorized) {
            sendEffect(InvoiceListUiEffect.Unauthorized)
        } else if (emitMessage) {
            sendEffect(InvoiceListUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendMessage(error: InvoiceError) {
        val message = error.toPresentationMessage()
        if (message.unauthorized) {
            sendEffect(InvoiceListUiEffect.Unauthorized)
        } else {
            sendEffect(InvoiceListUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendEffect(effect: InvoiceListUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }
}
