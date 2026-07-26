package com.brandcrafts.erp.feature.purchaseorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.PurchaseOrderError
import com.brandcrafts.erp.core.result.PurchaseOrderFailure
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.PurchaseOrder
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.ApprovePurchaseOrderUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.CancelPurchaseOrderUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.ObservePurchaseOrdersUseCase
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
class PurchaseOrderViewModel @Inject constructor(
    private val observePurchaseOrders: ObservePurchaseOrdersUseCase,
    private val observeContacts: ObserveContactsUseCase,
    private val approvePurchaseOrder: ApprovePurchaseOrderUseCase,
    private val cancelPurchaseOrder: CancelPurchaseOrderUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(PurchaseOrderUiState())
    val state = _state.asStateFlow()

    private val effectsChannel = Channel<PurchaseOrderUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    private var purchaseOrdersJob: Job? = null
    private var contactsJob: Job? = null
    private var purchaseOrders: List<PurchaseOrder> = emptyList()
    private var supplierNamesById: Map<String, String> = emptyMap()
    private var pendingApprovalId: String? = null
    private var pendingCancellationId: String? = null

    init {
        observe()
    }

    fun onEvent(event: PurchaseOrderUiEvent) {
        when (event) {
            PurchaseOrderUiEvent.Load,
            PurchaseOrderUiEvent.Refresh,
            PurchaseOrderUiEvent.Retry -> observe()
            is PurchaseOrderUiEvent.SearchChanged -> {
                _state.update { it.copy(query = event.value) }
                publishFiltered()
            }
            is PurchaseOrderUiEvent.StatusChanged -> {
                _state.update { it.copy(status = event.value) }
                publishFiltered()
            }
            PurchaseOrderUiEvent.CreateClicked -> sendEffect(PurchaseOrderUiEffect.NavigateCreate)
            is PurchaseOrderUiEvent.Clicked -> openDetails(event.id)
            is PurchaseOrderUiEvent.EditClicked -> edit(event.id)
            is PurchaseOrderUiEvent.ApproveClicked -> requestApproval(event.id)
            PurchaseOrderUiEvent.ApproveConfirmed -> approvePendingPurchaseOrder()
            is PurchaseOrderUiEvent.CancelClicked -> requestCancellation(event.id)
            PurchaseOrderUiEvent.CancelConfirmed -> cancelPendingPurchaseOrder()
            PurchaseOrderUiEvent.ErrorDismissed -> _state.update { it.copy(error = false) }
        }
    }

    private fun observe() {
        val hasExistingOrders = purchaseOrders.isNotEmpty()
        purchaseOrdersJob?.cancel()
        contactsJob?.cancel()
        _state.update {
            it.copy(
                loading = !hasExistingOrders,
                refreshing = hasExistingOrders,
                error = false,
            )
        }
        purchaseOrdersJob = viewModelScope.launch {
            try {
                observePurchaseOrders().collect { result ->
                    result.fold(
                        onSuccess = { orders ->
                            purchaseOrders = orders
                            _state.update { it.copy(loading = false, refreshing = false, error = false) }
                            publishFiltered()
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
                        supplierNamesById = contacts
                            .asSequence()
                            .filter { it.type == ContactType.SUPPLIER }
                            .associate { contact ->
                                contact.id to contact.company.ifBlank { contact.name }
                            }
                        publishFiltered()
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            }
        }
    }

    private fun handleObservationFailure(throwable: Throwable) {
        _state.update { it.copy(loading = false, refreshing = false, error = true) }
        handleError(throwable, emitMessage = false)
    }

    private fun publishFiltered() {
        val current = _state.value
        val query = current.query.trim()
        val visibleOrders = purchaseOrders
            .asSequence()
            .filter { order ->
                (current.status == null || order.status == current.status) &&
                    (query.isBlank() || order.number.contains(query, ignoreCase = true))
            }
            .map { order -> order.toListItem() }
            .toList()
        _state.update {
            it.copy(
                orders = visibleOrders,
                loading = false,
                refreshing = false,
            )
        }
    }

    private fun PurchaseOrder.toListItem(): PurchaseOrderListItemUi = PurchaseOrderListItemUi(
        id = id,
        number = number,
        supplierName = supplierNamesById[supplierId],
        dateMillis = dateMillis,
        expectedDeliveryDateMillis = expectedDeliveryDateMillis,
        total = total,
        status = status,
        canEdit = status == PurchaseOrderStatus.DRAFT,
        canApprove = status == PurchaseOrderStatus.DRAFT,
        canCancel = status == PurchaseOrderStatus.DRAFT,
    )

    private fun openDetails(id: String) {
        if (id.isBlank()) {
            sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            return
        }
        sendEffect(PurchaseOrderUiEffect.NavigateDetails(id))
    }

    private fun edit(id: String) {
        val order = purchaseOrders.firstOrNull { it.id == id }
        if (order == null) {
            sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
        } else if (order.status != PurchaseOrderStatus.DRAFT) {
            sendMessage(PurchaseOrderError.DraftOnlyUpdateRequired)
        } else {
            sendEffect(PurchaseOrderUiEffect.NavigateEdit(id))
        }
    }

    private fun requestApproval(id: String) {
        val order = purchaseOrders.firstOrNull { it.id == id }
        when {
            order == null -> sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            order.status != PurchaseOrderStatus.DRAFT -> sendMessage(PurchaseOrderError.InvalidStatusTransition)
            isOperating(id) -> Unit
            else -> {
                pendingApprovalId = id
                sendEffect(PurchaseOrderUiEffect.ConfirmApproval(id))
            }
        }
    }

    private fun approvePendingPurchaseOrder() {
        val id = pendingApprovalId ?: return
        val order = purchaseOrders.firstOrNull { it.id == id }
        pendingApprovalId = null
        if (order == null) {
            sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            return
        }
        if (order.status != PurchaseOrderStatus.DRAFT) {
            sendMessage(PurchaseOrderError.InvalidStatusTransition)
            return
        }
        if (isOperating(id)) return
        _state.update { it.copy(approvingId = id) }
        viewModelScope.launch {
            try {
                approvePurchaseOrder(id).fold(
                    onSuccess = {
                        _state.update { it.copy(approvingId = null) }
                        sendEffect(PurchaseOrderUiEffect.ShowMessage(R.string.purchase_order_approved_success))
                    },
                    onFailure = { throwable ->
                        _state.update { it.copy(approvingId = null) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _state.update { it.copy(approvingId = null) }
                handleError(exception)
            }
        }
    }

    private fun requestCancellation(id: String) {
        val order = purchaseOrders.firstOrNull { it.id == id }
        when {
            order == null -> sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            order.status == PurchaseOrderStatus.APPROVED -> sendMessage(PurchaseOrderError.StockReferenceValidationUnavailable)
            order.status == PurchaseOrderStatus.CANCELLED -> sendMessage(PurchaseOrderError.InvalidStatusTransition)
            isOperating(id) -> Unit
            else -> {
                pendingCancellationId = id
                sendEffect(PurchaseOrderUiEffect.ConfirmCancellation(id))
            }
        }
    }

    private fun cancelPendingPurchaseOrder() {
        val id = pendingCancellationId ?: return
        val order = purchaseOrders.firstOrNull { it.id == id }
        pendingCancellationId = null
        if (order == null) {
            sendMessage(PurchaseOrderError.PurchaseOrderNotFound)
            return
        }
        if (order.status == PurchaseOrderStatus.APPROVED) {
            sendMessage(PurchaseOrderError.StockReferenceValidationUnavailable)
            return
        }
        if (order.status != PurchaseOrderStatus.DRAFT) {
            sendMessage(PurchaseOrderError.InvalidStatusTransition)
            return
        }
        if (isOperating(id)) return
        _state.update { it.copy(cancellingId = id) }
        viewModelScope.launch {
            try {
                cancelPurchaseOrder(id).fold(
                    onSuccess = {
                        _state.update { it.copy(cancellingId = null) }
                        sendEffect(PurchaseOrderUiEffect.ShowMessage(R.string.purchase_order_cancelled_success))
                    },
                    onFailure = { throwable ->
                        _state.update { it.copy(cancellingId = null) }
                        handleError(throwable)
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                _state.update { it.copy(cancellingId = null) }
                handleError(exception)
            }
        }
    }

    private fun isOperating(id: String): Boolean =
        _state.value.approvingId == id ||
            _state.value.cancellingId == id ||
            pendingApprovalId == id ||
            pendingCancellationId == id

    private fun handleError(throwable: Throwable, emitMessage: Boolean = true) {
        val error = (throwable as? PurchaseOrderFailure)?.error ?: PurchaseOrderError.Unknown
        val message = error.toPurchaseOrderMessage()
        if (message.unauthorized) {
            sendEffect(PurchaseOrderUiEffect.Unauthorized)
        } else if (emitMessage) {
            sendEffect(PurchaseOrderUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendMessage(error: PurchaseOrderError) {
        val message = error.toPurchaseOrderMessage()
        if (message.unauthorized) {
            sendEffect(PurchaseOrderUiEffect.Unauthorized)
        } else {
            sendEffect(PurchaseOrderUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendEffect(effect: PurchaseOrderUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }
}
