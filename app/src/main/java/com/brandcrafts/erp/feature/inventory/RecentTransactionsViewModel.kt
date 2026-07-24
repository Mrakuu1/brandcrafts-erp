package com.brandcrafts.erp.feature.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.domain.model.InventoryTransaction
import com.brandcrafts.erp.domain.usecase.ObserveRecentInventoryTransactionsUseCase
import com.brandcrafts.erp.ui.components.StatusTone
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class RecentTransactionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeRecent: ObserveRecentInventoryTransactionsUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val materialId = savedStateHandle.get<String>("materialId").orEmpty()
    private val _uiState = MutableStateFlow<RecentTransactionsUiState>(RecentTransactionsUiState.Loading(materialId))
    val uiState: StateFlow<RecentTransactionsUiState> = _uiState.asStateFlow()
    private var collectionJob: Job? = null

    init { load() }
    fun retry() = load()

    private fun load() {
        collectionJob?.cancel()
        if (materialId.isBlank()) { _uiState.value = RecentTransactionsUiState.Empty(materialId); return }
        val userId = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user?.uid
        if (userId == null) { _uiState.value = RecentTransactionsUiState.Error(materialId); return }
        _uiState.value = RecentTransactionsUiState.Loading(materialId)
        collectionJob = viewModelScope.launch {
            observeRecent(materialId).collect { result ->
                result.fold(
                    onSuccess = { items ->
                        val mapped = items.map { it.toUi(userId) }
                        _uiState.value = if (mapped.isEmpty()) RecentTransactionsUiState.Empty(materialId) else RecentTransactionsUiState.Loaded(materialId, mapped)
                    },
                    onFailure = { _uiState.value = RecentTransactionsUiState.Error(materialId) },
                )
            }
        }
    }
}

private fun InventoryTransaction.toUi(currentUserId: String): RecentTransactionUiModel = RecentTransactionUiModel(
    id = id,
    typeLabelRes = when (type) { InventoryTransaction.Type.STOCK_IN -> R.string.stock_in_title; InventoryTransaction.Type.STOCK_OUT -> R.string.stock_out_title; InventoryTransaction.Type.MATERIAL_USAGE -> R.string.material_usage_title },
    tone = when (type) { InventoryTransaction.Type.STOCK_IN -> StatusTone.SUCCESS; InventoryTransaction.Type.STOCK_OUT -> StatusTone.WARNING; InventoryTransaction.Type.MATERIAL_USAGE -> StatusTone.INFO },
    quantityText = "${NumberFormat.getNumberInstance().format(quantity)} $unit",
    timestampText = createdAtMillis?.let { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it)) }.orEmpty(),
    referenceText = referenceId.takeIf(String::isNotBlank), remarks = remarks.takeIf(String::isNotBlank),
    performerLabelRes = if (performedBy == currentUserId) R.string.transaction_you else R.string.transaction_team_member,
)
