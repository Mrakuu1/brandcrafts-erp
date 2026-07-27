package com.brandcrafts.erp.feature.deliverychallan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.domain.model.DeliveryChallanDispatchRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanPdfError
import com.brandcrafts.erp.domain.model.DeliveryChallanPdfFailure
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.deliverychallan.CancelDraftDeliveryChallanUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.DispatchDeliveryChallanUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.GenerateDeliveryChallanPdfUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.GetDeliveryChallanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DeliveryChallanDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDeliveryChallan: GetDeliveryChallanUseCase,
    private val dispatchDeliveryChallan: DispatchDeliveryChallanUseCase,
    private val cancelDraftDeliveryChallan: CancelDraftDeliveryChallanUseCase,
    private val generateDeliveryChallanPdf: GenerateDeliveryChallanPdfUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val challanId = savedStateHandle.get<String>(CHALLAN_ID_ARGUMENT).orEmpty()
    private val _state = MutableStateFlow(DeliveryChallanDetailsUiState())
    val state = _state.asStateFlow()
    private val effectChannel = Channel<DeliveryChallanDetailsUiEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: DeliveryChallanDetailsUiEvent) {
        when (event) {
            DeliveryChallanDetailsUiEvent.Retry -> load()
            DeliveryChallanDetailsUiEvent.Back -> effectChannel.trySend(DeliveryChallanDetailsUiEffect.Back)
            DeliveryChallanDetailsUiEvent.Edit -> {
                if (state.value.canEdit) effectChannel.trySend(DeliveryChallanDetailsUiEffect.EditDraft(challanId))
                else sendUnauthorized()
            }
            DeliveryChallanDetailsUiEvent.Dispatch -> {
                if (state.value.canDispatch) effectChannel.trySend(DeliveryChallanDetailsUiEffect.ConfirmDispatch)
                else sendUnauthorized()
            }
            DeliveryChallanDetailsUiEvent.ConfirmDispatch -> operate {
                dispatchDeliveryChallan(DeliveryChallanDispatchRequest(challanId))
            }
            DeliveryChallanDetailsUiEvent.Cancel -> {
                if (state.value.canCancel) effectChannel.trySend(DeliveryChallanDetailsUiEffect.ConfirmCancel)
                else sendUnauthorized()
            }
            DeliveryChallanDetailsUiEvent.ConfirmCancel -> operate {
                cancelDraftDeliveryChallan(challanId)
            }
            DeliveryChallanDetailsUiEvent.PreviewPdf -> generatePdf(preview = true)
            DeliveryChallanDetailsUiEvent.SharePdf -> generatePdf(preview = false)
        }
    }

    private fun load() {
        if (challanId.isBlank()) {
            _state.value = DeliveryChallanDetailsUiState(content = DeliveryChallanDetailsContent.Error)
            return
        }
        viewModelScope.launch {
            getDeliveryChallan(challanId).fold(
                onSuccess = { challan ->
                    val canManage = isActiveAdmin() && challan.status == DeliveryChallanStatus.DRAFT
                    _state.value = DeliveryChallanDetailsUiState(
                        content = DeliveryChallanDetailsContent.Loaded,
                        challan = DeliveryChallanDetailsModel(
                            id = challan.id,
                            number = challan.number,
                            customer = DeliveryChallanCustomerOption(
                                id = challan.customerId,
                                label = challan.customerId,
                                deliveryAddress = challan.deliveryAddress,
                            ),
                            deliveryAddress = challan.deliveryAddress,
                            dateMillis = challan.dateMillis,
                            sourceType = challan.sourceType,
                            sourceInvoiceNumber = challan.sourceInvoiceNumber,
                            vehicleNumber = challan.vehicleNumber,
                            driverName = challan.driverName,
                            notes = challan.notes,
                            status = challan.status,
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
                        ),
                        canEdit = canManage,
                        canDispatch = canManage,
                        canCancel = canManage,
                    )
                },
                onFailure = ::handleFailure,
            )
        }
    }

    private fun operate(action: suspend () -> Result<Unit>) {
        if (_state.value.isOperating) return
        _state.update { it.copy(isOperating = true) }
        viewModelScope.launch {
            action().fold(
                onSuccess = {
                    _state.update { it.copy(isOperating = false) }
                    effectChannel.trySend(
                        DeliveryChallanDetailsUiEffect.ShowMessage(R.string.delivery_challan_operation_success),
                    )
                    load()
                },
                onFailure = {
                    _state.update { state -> state.copy(isOperating = false) }
                    handleFailure(it)
                },
            )
        }
    }

    private fun generatePdf(preview: Boolean) {
        if (_state.value.isGeneratingPdf || challanId.isBlank()) return
        _state.update { it.copy(isGeneratingPdf = true) }
        viewModelScope.launch {
            generateDeliveryChallanPdf(challanId).fold(
                onSuccess = { pdf ->
                    _state.update { it.copy(isGeneratingPdf = false) }
                    effectChannel.trySend(
                        if (preview) {
                            DeliveryChallanDetailsUiEffect.PreviewPdf(pdf.cacheFileName)
                        } else {
                            DeliveryChallanDetailsUiEffect.SharePdf(pdf.cacheFileName)
                        },
                    )
                },
                onFailure = { throwable ->
                    _state.update { it.copy(isGeneratingPdf = false) }
                    handlePdfFailure(throwable)
                },
            )
        }
    }

    private fun handlePdfFailure(throwable: Throwable) {
        val resource = when ((throwable as? DeliveryChallanPdfFailure)?.error) {
            DeliveryChallanPdfError.CompanyConfigurationIncomplete -> R.string.delivery_challan_pdf_company_configuration_error
            DeliveryChallanPdfError.CustomerUnavailable -> R.string.delivery_challan_pdf_customer_error
            DeliveryChallanPdfError.DeliveryChallanUnavailable -> R.string.delivery_challan_error_generic
            DeliveryChallanPdfError.GenerationFailed, null -> R.string.delivery_challan_pdf_generation_error
        }
        effectChannel.trySend(DeliveryChallanDetailsUiEffect.ShowMessage(resource))
    }

    private fun handleFailure(throwable: Throwable) {
        val message = DeliveryChallanPresentationErrorMapper.map(throwable)
        if (message.unauthorized) {
            sendUnauthorized()
        } else {
            _state.update { it.copy(content = DeliveryChallanDetailsContent.Error) }
            effectChannel.trySend(DeliveryChallanDetailsUiEffect.ShowMessage(message.messageRes))
        }
    }

    private fun sendUnauthorized() {
        effectChannel.trySend(DeliveryChallanDetailsUiEffect.Unauthorized)
    }

    private fun isActiveAdmin(): Boolean =
        ((sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user)?.let {
            it.active && it.role == UserRole.ADMIN
        } == true

    private companion object {
        const val CHALLAN_ID_ARGUMENT = "challanId"
    }
}
