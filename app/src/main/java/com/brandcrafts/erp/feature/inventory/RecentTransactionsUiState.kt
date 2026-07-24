package com.brandcrafts.erp.feature.inventory

import androidx.annotation.StringRes
import com.brandcrafts.erp.ui.components.StatusTone

data class RecentTransactionUiModel(
    val id: String,
    @param:StringRes val typeLabelRes: Int,
    val tone: StatusTone,
    val quantityText: String,
    val timestampText: String,
    val referenceText: String?,
    val remarks: String?,
    @param:StringRes val performerLabelRes: Int,
)

sealed interface RecentTransactionsUiState {
    val materialId: String
    data class Loading(override val materialId: String) : RecentTransactionsUiState
    data class Loaded(override val materialId: String, val transactions: List<RecentTransactionUiModel>) : RecentTransactionsUiState
    data class Empty(override val materialId: String) : RecentTransactionsUiState
    data class Error(override val materialId: String) : RecentTransactionsUiState
}
