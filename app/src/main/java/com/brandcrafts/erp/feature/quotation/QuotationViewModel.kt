package com.brandcrafts.erp.feature.quotation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.quotation.ObserveQuotationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class QuotationViewModel @Inject constructor(
    private val observeQuotations: ObserveQuotationsUseCase,
    private val observeContacts: ObserveContactsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(QuotationUiState())
    val state = _state.asStateFlow()

    private var quotationsJob: Job? = null
    private var contactsJob: Job? = null
    private var quotations: List<Quotation> = emptyList()
    private var customerNamesById: Map<String, String> = emptyMap()

    init {
        load()
    }

    fun search(query: String) {
        _state.value = _state.value.copy(query = query)
        publishFiltered()
    }

    fun status(status: QuotationStatus?) {
        _state.value = _state.value.copy(status = status)
        publishFiltered()
    }

    fun retry() = load()

    private fun load() {
        quotationsJob?.cancel()
        contactsJob?.cancel()
        _state.value = _state.value.copy(content = QuotationUiState.Content.Loading)
        quotationsJob = viewModelScope.launch {
            observeQuotations().collect { result ->
                result.fold(
                    onSuccess = { items ->
                        quotations = items
                        publishFiltered()
                    },
                    onFailure = { _state.value = _state.value.copy(content = QuotationUiState.Content.Error) },
                )
            }
        }
        contactsJob = viewModelScope.launch {
            observeContacts().collect { result ->
                result.onSuccess { contacts ->
                    customerNamesById = contacts
                        .asSequence()
                        .filter { it.type == ContactType.CUSTOMER }
                        .associate { contact ->
                            contact.id to if (contact.company.isBlank()) contact.name else "${contact.name} · ${contact.company}"
                        }
                    if (_state.value.content != QuotationUiState.Content.Error) publishFiltered()
                }
            }
        }
    }

    private fun publishFiltered() {
        val state = _state.value
        val query = state.query.trim()
        val visible = quotations
            .asSequence()
            .filter { (state.status == null || it.status == state.status) && (query.isBlank() || it.number.contains(query, ignoreCase = true)) }
            .map { quotation -> QuotationListItem(quotation, customerNamesById[quotation.contactId]) }
            .toList()
        _state.value = state.copy(
            all = quotations,
            visible = visible,
            content = if (visible.isEmpty()) QuotationUiState.Content.Empty else QuotationUiState.Content.Loaded,
        )
    }
}
