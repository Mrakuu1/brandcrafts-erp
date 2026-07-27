package com.brandcrafts.erp.feature.deliverychallan

import androidx.annotation.StringRes

sealed interface DeliveryChallanListUiEffect { data object NavigateCreateIndependent : DeliveryChallanListUiEffect; data object NavigateCreateFromInvoice : DeliveryChallanListUiEffect; data class NavigateDetails(val id: String) : DeliveryChallanListUiEffect; data class NavigateEditDraft(val id: String) : DeliveryChallanListUiEffect; data class ConfirmDispatch(val id: String) : DeliveryChallanListUiEffect; data class ConfirmCancellation(val id: String) : DeliveryChallanListUiEffect; data class ShowMessage(@StringRes val messageRes: Int) : DeliveryChallanListUiEffect; data object Unauthorized : DeliveryChallanListUiEffect }
