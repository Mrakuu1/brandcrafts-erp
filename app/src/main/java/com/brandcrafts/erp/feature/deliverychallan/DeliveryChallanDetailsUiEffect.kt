package com.brandcrafts.erp.feature.deliverychallan

import androidx.annotation.StringRes

sealed interface DeliveryChallanDetailsUiEffect {
    data object Back : DeliveryChallanDetailsUiEffect
    data class EditDraft(val id: String) : DeliveryChallanDetailsUiEffect
    data object ConfirmDispatch : DeliveryChallanDetailsUiEffect
    data object ConfirmCancel : DeliveryChallanDetailsUiEffect
    data class PreviewPdf(val cacheFileName: String) : DeliveryChallanDetailsUiEffect
    data class SharePdf(val cacheFileName: String) : DeliveryChallanDetailsUiEffect
    data class ShowMessage(@StringRes val id: Int) : DeliveryChallanDetailsUiEffect
    data object Unauthorized : DeliveryChallanDetailsUiEffect
}
