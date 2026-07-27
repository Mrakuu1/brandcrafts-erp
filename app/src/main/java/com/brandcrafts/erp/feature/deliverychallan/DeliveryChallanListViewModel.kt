package com.brandcrafts.erp.feature.deliverychallan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.DeliveryChallanDispatchRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.CancelDraftDeliveryChallanUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.DispatchDeliveryChallanUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.ObserveDeliveryChallansUseCase
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
class DeliveryChallanListViewModel @Inject constructor(
    private val observeDeliveryChallans: ObserveDeliveryChallansUseCase,
    private val observeContacts: ObserveContactsUseCase,
    private val dispatchDeliveryChallan: DispatchDeliveryChallanUseCase,
    private val cancelDraftDeliveryChallan: CancelDraftDeliveryChallanUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(DeliveryChallanListUiState())
    val state = _state.asStateFlow()

    private val effectChannel = Channel<DeliveryChallanListUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var allRows: List<DeliveryChallanListItem> = emptyList()
    private var customerNames: Map<String, String> = emptyMap()
    private var challanObservation: Job? = null
    private var pendingDispatchId: String? = null
    private var pendingCancellationId: String? = null

    init {
        observeCustomers()
        observeChallans(initialLoad = true)
    }

    fun onEvent(event: DeliveryChallanListUiEvent) {
        when (event) {
            DeliveryChallanListUiEvent.Refresh -> observeChallans(initialLoad = false)
            DeliveryChallanListUiEvent.Retry -> observeChallans(initialLoad = allRows.isEmpty())
            is DeliveryChallanListUiEvent.SearchChanged -> {
                updateState { copy(searchQuery = event.query) }
                publishRows()
            }
            is DeliveryChallanListUiEvent.StatusChanged -> {
                updateState { copy(statusFilter = event.status) }
                publishRows()
            }
            DeliveryChallanListUiEvent.CreateIndependentClicked -> {
                if (state.value.canCreateIndependent) {
                    effectChannel.trySend(DeliveryChallanListUiEffect.NavigateCreateIndependent)
                } else {
                    sendUnauthorized()
                }
            }
            DeliveryChallanListUiEvent.CreateFromInvoiceClicked -> {
                if (state.value.canCreateFromInvoice) {
                    effectChannel.trySend(DeliveryChallanListUiEffect.NavigateCreateFromInvoice)
                } else {
                    sendUnauthorized()
                }
            }
            is DeliveryChallanListUiEvent.DetailsClicked -> {
                effectChannel.trySend(DeliveryChallanListUiEffect.NavigateDetails(event.id))
            }
            is DeliveryChallanListUiEvent.EditClicked -> requestEdit(event.id)
            is DeliveryChallanListUiEvent.DispatchClicked -> requestDispatch(event.id)
            DeliveryChallanListUiEvent.DispatchConfirmed -> dispatch()
            is DeliveryChallanListUiEvent.CancelClicked -> requestCancellation(event.id)
            DeliveryChallanListUiEvent.CancelConfirmed -> cancel()
        }
    }

    private fun observeCustomers() {
        viewModelScope.launch {
            observeContacts().collect { result ->
                result.onSuccess { contacts ->
                    customerNames = contacts
                        .asSequence()
                        .filter { it.type == ContactType.CUSTOMER }
                        .associate { it.id to it.company.ifBlank { it.name } }
                    rebuildRowsWithCustomerNames()
                }
            }
        }
    }

    private fun observeChallans(initialLoad: Boolean) {
        challanObservation?.cancel()
        updateState {
            copy(
                content = if (initialLoad && allRows.isEmpty()) {
                    DeliveryChallanListContent.Loading
                } else {
                    content
                },
                isRefreshing = !initialLoad && allRows.isNotEmpty(),
            )
        }
        challanObservation = viewModelScope.launch {
            try {
                observeDeliveryChallans().collect { result ->
                    result.fold(
                        onSuccess = { summaries ->
                            val canManageDrafts = isActiveAdmin()
                            allRows = summaries.map { summary ->
                                DeliveryChallanListItem(
                                    id = summary.id,
                                    number = summary.number,
                                    customerId = summary.customerId,
                                    customerName = customerNames[summary.customerId],
                                    dateMillis = summary.dateMillis,
                                    sourceType = summary.sourceType,
                                    sourceInvoiceNumber = summary.sourceInvoiceNumber,
                                    status = summary.status,
                                    canEdit = canManageDrafts && summary.status == DeliveryChallanStatus.DRAFT,
                                    canDispatch = canManageDrafts && summary.status == DeliveryChallanStatus.DRAFT,
                                    canCancel = canManageDrafts && summary.status == DeliveryChallanStatus.DRAFT,
                                )
                            }
                            publishRows()
                        },
                        onFailure = { handleObservationFailure() },
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                handleObservationFailure()
            }
        }
    }

    private fun rebuildRowsWithCustomerNames() {
        allRows = allRows.map { row ->
            row.copy(customerName = customerNames[row.customerId] ?: row.customerName)
        }
        publishRows()
    }

    private fun publishRows() {
        val current = state.value
        val visibleRows = allRows.filter { row ->
            (current.statusFilter == null || row.status == current.statusFilter) &&
                (current.searchQuery.isBlank() ||
                    row.number.contains(current.searchQuery, ignoreCase = true) ||
                    row.customerName?.contains(current.searchQuery, ignoreCase = true) == true)
        }
        updateState {
            copy(
                content = if (visibleRows.isEmpty()) {
                    DeliveryChallanListContent.Empty
                } else {
                    DeliveryChallanListContent.Loaded
                },
                rows = visibleRows,
                canCreateIndependent = isActiveAdmin(),
                canCreateFromInvoice = isActiveAdmin(),
                isRefreshing = false,
            )
        }
    }

    private fun requestEdit(challanId: String) {
        if (allRows.firstOrNull { it.id == challanId }?.canEdit == true) {
            effectChannel.trySend(DeliveryChallanListUiEffect.NavigateEditDraft(challanId))
        } else {
            sendUnauthorized()
        }
    }

    private fun requestDispatch(challanId: String) {
        if (allRows.firstOrNull { it.id == challanId }?.canDispatch == true) {
            pendingDispatchId = challanId
            effectChannel.trySend(DeliveryChallanListUiEffect.ConfirmDispatch(challanId))
        } else {
            sendUnauthorized()
        }
    }

    private fun requestCancellation(challanId: String) {
        if (allRows.firstOrNull { it.id == challanId }?.canCancel == true) {
            pendingCancellationId = challanId
            effectChannel.trySend(DeliveryChallanListUiEffect.ConfirmCancellation(challanId))
        } else {
            sendUnauthorized()
        }
    }

    private fun dispatch() {
        val challanId = pendingDispatchId ?: return
        if (state.value.actionInProgress != null) return
        pendingDispatchId = null
        updateState { copy(actionInProgress = DeliveryChallanListOperation.Dispatch(challanId)) }
        viewModelScope.launch {
            dispatchDeliveryChallan(DeliveryChallanDispatchRequest(challanId)).fold(
                onSuccess = {
                    updateState { copy(actionInProgress = null) }
                    effectChannel.trySend(
                        DeliveryChallanListUiEffect.ShowMessage(R.string.delivery_challan_dispatched_success),
                    )
                },
                onFailure = { handleOperationFailure(it) },
            )
        }
    }

    private fun cancel() {
        val challanId = pendingCancellationId ?: return
        if (state.value.actionInProgress != null) return
        pendingCancellationId = null
        updateState { copy(actionInProgress = DeliveryChallanListOperation.Cancel(challanId)) }
        viewModelScope.launch {
            cancelDraftDeliveryChallan(challanId).fold(
                onSuccess = {
                    updateState { copy(actionInProgress = null) }
                    effectChannel.trySend(
                        DeliveryChallanListUiEffect.ShowMessage(R.string.delivery_challan_cancelled_success),
                    )
                },
                onFailure = { handleOperationFailure(it) },
            )
        }
    }

    private fun handleObservationFailure() {
        updateState { copy(content = DeliveryChallanListContent.Error, isRefreshing = false) }
    }

    private fun handleOperationFailure(throwable: Throwable) {
        updateState { copy(actionInProgress = null) }
        val message = DeliveryChallanPresentationErrorMapper.map(throwable)
        effectChannel.trySend(
            if (message.unauthorized) DeliveryChallanListUiEffect.Unauthorized
            else DeliveryChallanListUiEffect.ShowMessage(message.messageRes),
        )
    }

    private fun sendUnauthorized() {
        effectChannel.trySend(DeliveryChallanListUiEffect.Unauthorized)
    }

    private fun isActiveAdmin(): Boolean =
        ((sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user)?.let {
            it.active && it.role == UserRole.ADMIN
        } == true

    private fun updateState(transform: DeliveryChallanListUiState.() -> DeliveryChallanListUiState) {
        _state.update { it.transform() }
    }
}
