package com.brandcrafts.erp.feature.deliverychallan

sealed interface DeliveryChallanDetailsUiEvent {
    data object Retry : DeliveryChallanDetailsUiEvent
    data object Back : DeliveryChallanDetailsUiEvent
    data object Edit : DeliveryChallanDetailsUiEvent
    data object Dispatch : DeliveryChallanDetailsUiEvent
    data object ConfirmDispatch : DeliveryChallanDetailsUiEvent
    data object Cancel : DeliveryChallanDetailsUiEvent
    data object ConfirmCancel : DeliveryChallanDetailsUiEvent
    data object PreviewPdf : DeliveryChallanDetailsUiEvent
    data object SharePdf : DeliveryChallanDetailsUiEvent
}
