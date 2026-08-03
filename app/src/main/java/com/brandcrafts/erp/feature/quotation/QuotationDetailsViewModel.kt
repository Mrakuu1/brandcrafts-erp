package com.brandcrafts.erp.feature.quotation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.model.QuotationPdfError
import com.brandcrafts.erp.domain.model.QuotationPdfFailure
import com.brandcrafts.erp.domain.usecase.quotation.GenerateQuotationPdfUseCase
import com.brandcrafts.erp.domain.usecase.quotation.GetQuotationUseCase
import com.brandcrafts.erp.domain.usecase.quotation.QuotationCalculator
import com.brandcrafts.erp.domain.usecase.quotation.QuotationCalculationLine
import com.brandcrafts.erp.domain.usecase.quotation.QuotationTotals
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class QuotationDetailsUiState(val loading: Boolean = true, val quotation: Quotation? = null, val customerName: String? = null, val customerSubtitle: String? = null, val totals: QuotationTotals? = null, val error: Boolean = false, val pdfGenerating: Boolean = false)
sealed interface QuotationDetailsUiEvent { data object Retry: QuotationDetailsUiEvent; data object Back: QuotationDetailsUiEvent; data object Edit: QuotationDetailsUiEvent; data object PreviewPdf: QuotationDetailsUiEvent; data object SharePdf: QuotationDetailsUiEvent }
sealed interface QuotationDetailsUiEffect { data object NavigateBack: QuotationDetailsUiEffect; data class NavigateToEdit(val quotationId:String): QuotationDetailsUiEffect; data class PreviewPdf(val cacheFileName:String): QuotationDetailsUiEffect; data class SharePdf(val cacheFileName:String): QuotationDetailsUiEffect; data class ShowMessage(val messageRes:Int): QuotationDetailsUiEffect }

@HiltViewModel
class QuotationDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getQuotation: GetQuotationUseCase,
    private val observeContacts: ObserveContactsUseCase,
    private val calculator: QuotationCalculator,
    private val generateQuotationPdf: GenerateQuotationPdfUseCase,
) : ViewModel() {
    private val id = savedStateHandle.get<String>("quotationId")?.takeIf { it.isNotBlank() }
    private val _state = MutableStateFlow(QuotationDetailsUiState())
    val state = _state.asStateFlow()
    private val effectsChannel = Channel<QuotationDetailsUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()
    private var customersById: Map<String, Pair<String, String?>> = emptyMap()
    init { observeCustomers(); load() }
    fun onEvent(event: QuotationDetailsUiEvent) = when (event) {
        QuotationDetailsUiEvent.Retry -> load()
        QuotationDetailsUiEvent.Back -> send(QuotationDetailsUiEffect.NavigateBack)
        QuotationDetailsUiEvent.Edit -> _state.value.quotation
            ?.takeIf { it.status == com.brandcrafts.erp.domain.model.QuotationStatus.DRAFT }
            ?.let { send(QuotationDetailsUiEffect.NavigateToEdit(it.id)) }
        QuotationDetailsUiEvent.PreviewPdf -> generate(preview = true)
        QuotationDetailsUiEvent.SharePdf -> generate(preview = false)
    }
    private fun load() { val quotationId=id ?: run { _state.value=QuotationDetailsUiState(loading=false,error=true); return }; _state.value=_state.value.copy(loading=true,error=false); viewModelScope.launch { getQuotation(quotationId).fold({ quotation -> _state.value=QuotationDetailsUiState(loading=false,quotation=quotation,totals=calculator.totals(quotation.lines.map { QuotationCalculationLine(it.quantity,it.unitPrice,it.discount,it.tax) })).withCustomer(quotation) },{ _state.value=QuotationDetailsUiState(loading=false,error=true) }) } }
    private fun observeCustomers() = viewModelScope.launch {
        observeContacts().collect { result ->
            result.onSuccess { contacts ->
                customersById = contacts.filter { it.type == ContactType.CUSTOMER }.associate { contact ->
                    contact.id to (contact.name to contact.company.takeIf(String::isNotBlank))
                }
                _state.value.quotation?.let { quotation -> _state.value = _state.value.withCustomer(quotation) }
            }
        }
    }
    private fun QuotationDetailsUiState.withCustomer(quotation: Quotation): QuotationDetailsUiState {
        val customer = customersById[quotation.contactId]
        return copy(customerName = customer?.first, customerSubtitle = customer?.second)
    }
    private fun generate(preview: Boolean) {
        val quotationId = id ?: return
        if (_state.value.pdfGenerating) return

        _state.value = _state.value.copy(pdfGenerating = true)
        viewModelScope.launch {
            try {
                generateQuotationPdf(quotationId).fold(
                    onSuccess = { pdf ->
                        _state.value = _state.value.copy(pdfGenerating = false)
                        send(
                            if (preview) {
                                QuotationDetailsUiEffect.PreviewPdf(pdf.cacheFileName)
                            } else {
                                QuotationDetailsUiEffect.SharePdf(pdf.cacheFileName)
                            },
                        )
                    },
                    onFailure = { failure ->
                        _state.value = _state.value.copy(pdfGenerating = false)
                        val message = when ((failure as? QuotationPdfFailure)?.error) {
                            QuotationPdfError.CompanyConfigurationIncomplete -> R.string.quotation_pdf_company_configuration_error
                            QuotationPdfError.CustomerUnavailable -> R.string.quotation_pdf_customer_error
                            else -> R.string.quotation_pdf_generation_error
                        }
                        send(QuotationDetailsUiEffect.ShowMessage(message))
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Throwable) {
                _state.value = _state.value.copy(pdfGenerating = false)
                send(QuotationDetailsUiEffect.ShowMessage(R.string.quotation_pdf_generation_error))
            }
        }
    }
    private fun send(effect:QuotationDetailsUiEffect){viewModelScope.launch{effectsChannel.send(effect)}}
}
