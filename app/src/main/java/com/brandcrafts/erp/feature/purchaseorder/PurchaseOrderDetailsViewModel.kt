package com.brandcrafts.erp.feature.purchaseorder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.core.result.PurchaseOrderError
import com.brandcrafts.erp.core.result.PurchaseOrderFailure
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.PurchaseOrder
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.domain.usecase.contact.GetContactUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.ApprovePurchaseOrderUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.CancelPurchaseOrderUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.GetPurchaseOrderUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.GeneratePurchaseOrderPdfUseCase
import com.brandcrafts.erp.domain.model.PurchaseOrderPdfError
import com.brandcrafts.erp.domain.model.PurchaseOrderPdfFailure
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
class PurchaseOrderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPurchaseOrder: GetPurchaseOrderUseCase,
    private val getContact: GetContactUseCase,
    private val approvePurchaseOrder: ApprovePurchaseOrderUseCase,
    private val cancelPurchaseOrder: CancelPurchaseOrderUseCase,
    private val generatePurchaseOrderPdf: GeneratePurchaseOrderPdfUseCase,
) : ViewModel() {
    private val purchaseOrderId = savedStateHandle.get<String>(PURCHASE_ORDER_ID_ARGUMENT)
        ?.trim()
        ?.takeIf(String::isNotEmpty)

    private val _state = MutableStateFlow(PurchaseOrderDetailsUiState())
    val state = _state.asStateFlow()

    private val effectsChannel = Channel<PurchaseOrderDetailsUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var loadJob: Job? = null
    private var pendingApproval = false
    private var pendingCancellation = false

    init {
        load()
    }

    fun onEvent(event: PurchaseOrderDetailsUiEvent) {
        when (event) {
            PurchaseOrderDetailsUiEvent.Load,
            PurchaseOrderDetailsUiEvent.Retry -> load()
            PurchaseOrderDetailsUiEvent.EditClicked -> edit()
            PurchaseOrderDetailsUiEvent.ApproveClicked -> requestApproval()
            PurchaseOrderDetailsUiEvent.ApproveConfirmed -> approve()
            PurchaseOrderDetailsUiEvent.CancelClicked -> requestCancellation()
            PurchaseOrderDetailsUiEvent.CancelConfirmed -> cancel()
            PurchaseOrderDetailsUiEvent.PreviewPdfClicked -> generatePdf(preview = true)
            PurchaseOrderDetailsUiEvent.SharePdfClicked -> generatePdf(preview = false)
            PurchaseOrderDetailsUiEvent.ErrorDismissed -> update {
                copy(error = false, approvedCancellationUnavailable = false)
            }
            PurchaseOrderDetailsUiEvent.BackClicked -> sendEffect(PurchaseOrderDetailsUiEffect.NavigateBack)
        }
    }

    private fun load() {
        val id = purchaseOrderId
        if (id == null) {
            setLoadError(PurchaseOrderError.PurchaseOrderNotFound)
            return
        }
        loadJob?.cancel()
        update { copy(loading = true, error = false, approvedCancellationUnavailable = false) }
        loadJob = viewModelScope.launch {
            try {
                getPurchaseOrder(id).fold(
                    onSuccess = { order -> mapPurchaseOrder(order) },
                    onFailure = ::setLoadError,
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                setLoadError(exception)
            }
        }
    }

    private suspend fun mapPurchaseOrder(order: PurchaseOrder) {
        val supplier = when (val result = getContact(order.supplierId)) {
            is ContactResult.Success -> result.data
                .takeIf { it.type == ContactType.SUPPLIER }
                ?.let { PurchaseOrderSupplierOption(it.id, it.name, it.company) }
            is ContactResult.Error -> null
        }
        val isDraft = order.status == PurchaseOrderStatus.DRAFT
        val details = PurchaseOrderDetailsUi(
            id = order.id,
            number = order.number,
            supplier = supplier,
            dateMillis = order.dateMillis,
            expectedDeliveryDateMillis = order.expectedDeliveryDateMillis,
            status = order.status,
            lines = order.lines.map { line ->
                EditablePurchaseOrderLine(
                    lineId = line.id,
                    materialId = line.materialId,
                    description = line.description,
                    quantity = line.quantity.toPlainString(),
                    unit = line.unit,
                    unitPrice = line.unitPrice.toPlainString(),
                    lineTotal = line.lineTotal,
                )
            },
            total = order.total,
            remarks = order.remarks,
            canEdit = isDraft,
            canApprove = isDraft,
            canCancel = isDraft,
        )
        update { copy(loading = false, details = details, error = false) }
    }

    private fun edit() {
        val details = _state.value.details
        when {
            details == null -> sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            details.id.isBlank() -> sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            details.status != PurchaseOrderStatus.DRAFT -> sendMessage(PurchaseOrderError.DraftOnlyUpdateRequired)
            isOperating() -> Unit
            else -> sendEffect(PurchaseOrderDetailsUiEffect.NavigateEdit(details.id))
        }
    }

    private fun requestApproval() {
        val details = _state.value.details
        when {
            details == null -> sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            details.status != PurchaseOrderStatus.DRAFT -> sendMessage(PurchaseOrderError.InvalidStatusTransition)
            isOperating() -> Unit
            else -> {
                pendingApproval = true
                sendEffect(PurchaseOrderDetailsUiEffect.ConfirmApproval)
            }
        }
    }

    private fun approve() {
        val details = _state.value.details
        if (!pendingApproval || details == null) return
        pendingApproval = false
        if (details.id.isBlank()) {
            sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            return
        }
        if (details.status != PurchaseOrderStatus.DRAFT) {
            sendMessage(PurchaseOrderError.InvalidStatusTransition)
            return
        }
        if (isOperating()) return
        update { copy(approving = true) }
        viewModelScope.launch {
            try {
                approvePurchaseOrder(details.id).fold(
                    onSuccess = {
                        update { copy(approving = false) }
                        sendEffect(PurchaseOrderDetailsUiEffect.ShowMessage(R.string.purchase_order_approved_success))
                        load()
                    },
                    onFailure = { throwable ->
                        update { copy(approving = false) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                update { copy(approving = false) }
                handleError(exception)
            }
        }
    }

    private fun requestCancellation() {
        val details = _state.value.details
        when {
            details == null -> sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            details.status == PurchaseOrderStatus.APPROVED -> {
                update { copy(approvedCancellationUnavailable = true) }
                sendMessage(PurchaseOrderError.StockReferenceValidationUnavailable)
            }
            details.status == PurchaseOrderStatus.CANCELLED -> sendMessage(PurchaseOrderError.InvalidStatusTransition)
            isOperating() -> Unit
            else -> {
                pendingCancellation = true
                sendEffect(PurchaseOrderDetailsUiEffect.ConfirmCancellation)
            }
        }
    }

    private fun cancel() {
        val details = _state.value.details
        if (!pendingCancellation || details == null) return
        pendingCancellation = false
        if (details.id.isBlank()) {
            sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            return
        }
        if (details.status == PurchaseOrderStatus.APPROVED) {
            update { copy(approvedCancellationUnavailable = true) }
            sendMessage(PurchaseOrderError.StockReferenceValidationUnavailable)
            return
        }
        if (details.status != PurchaseOrderStatus.DRAFT) {
            sendMessage(PurchaseOrderError.InvalidStatusTransition)
            return
        }
        if (isOperating()) return
        update { copy(cancelling = true) }
        viewModelScope.launch {
            try {
                cancelPurchaseOrder(details.id).fold(
                    onSuccess = {
                        update { copy(cancelling = false) }
                        sendEffect(PurchaseOrderDetailsUiEffect.ShowMessage(R.string.purchase_order_cancelled_success))
                        load()
                    },
                    onFailure = { throwable ->
                        update { copy(cancelling = false) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                update { copy(cancelling = false) }
                handleError(exception)
            }
        }
    }

    private fun isOperating(): Boolean =
        _state.value.approving || _state.value.cancelling || _state.value.pdfGenerating || pendingApproval || pendingCancellation

    private fun generatePdf(preview: Boolean) {
        val id = _state.value.details?.id?.takeIf(String::isNotBlank) ?: run { sendMessage(PurchaseOrderError.PurchaseOrderNotFound); return }
        if (isOperating()) return
        update { copy(pdfGenerating = true) }
        viewModelScope.launch {
            try {
                generatePurchaseOrderPdf(id).fold(
                    onSuccess = { pdf ->
                        update { copy(pdfGenerating = false) }
                        sendEffect(if (preview) PurchaseOrderDetailsUiEffect.PreviewPdf(pdf.cacheFileName) else PurchaseOrderDetailsUiEffect.SharePdf(pdf.cacheFileName))
                    },
                    onFailure = { failure -> update { copy(pdfGenerating = false) }; handlePdfError(failure) },
                )
            } catch (exception: CancellationException) { throw exception
            } catch (exception: Throwable) { update { copy(pdfGenerating = false) }; handlePdfError(exception) }
        }
    }

    private fun handlePdfError(throwable: Throwable) {
        val resource = when ((throwable as? PurchaseOrderPdfFailure)?.error) {
            PurchaseOrderPdfError.CompanyConfigurationIncomplete -> R.string.purchase_order_pdf_company_configuration_error
            PurchaseOrderPdfError.SupplierUnavailable -> R.string.purchase_order_pdf_supplier_error
            PurchaseOrderPdfError.PurchaseOrderUnavailable -> R.string.purchase_order_error_not_found
            else -> R.string.purchase_order_pdf_generation_error
        }
        sendEffect(PurchaseOrderDetailsUiEffect.ShowMessage(resource))
    }

    private fun setLoadError(throwable: Throwable) {
        update { copy(loading = false, approving = false, cancelling = false, error = true) }
        handleError(throwable, emitMessage = false)
    }

    private fun setLoadError(error: PurchaseOrderError) {
        setLoadError(PurchaseOrderFailure(error))
    }

    private fun handleError(throwable: Throwable, emitMessage: Boolean = true) {
        val error = (throwable as? PurchaseOrderFailure)?.error ?: PurchaseOrderError.Unknown
        val message = error.toPurchaseOrderMessage()
        if (message.unauthorized) {
            sendEffect(PurchaseOrderDetailsUiEffect.Unauthorized)
        } else if (emitMessage) {
            sendEffect(PurchaseOrderDetailsUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendMessage(error: PurchaseOrderError) {
        val message = error.toPurchaseOrderMessage()
        if (message.unauthorized) {
            sendEffect(PurchaseOrderDetailsUiEffect.Unauthorized)
        } else {
            sendEffect(PurchaseOrderDetailsUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun update(transform: PurchaseOrderDetailsUiState.() -> PurchaseOrderDetailsUiState) {
        _state.update(transform)
    }

    private fun sendEffect(effect: PurchaseOrderDetailsUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }

    private companion object {
        const val PURCHASE_ORDER_ID_ARGUMENT = "purchaseOrderId"
    }
}
