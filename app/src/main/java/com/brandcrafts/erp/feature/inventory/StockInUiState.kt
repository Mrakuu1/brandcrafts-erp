package com.brandcrafts.erp.feature.inventory

import androidx.annotation.StringRes

data class StockInUiState(
    val materialId: String,
    val materialName: String = "",
    val unit: String = "",
    val quantity: String = "",
    val referenceId: String = "",
    val remarks: String = "",
    @param:StringRes val quantityError: Int? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    @param:StringRes val errorMessage: Int? = null,
)
