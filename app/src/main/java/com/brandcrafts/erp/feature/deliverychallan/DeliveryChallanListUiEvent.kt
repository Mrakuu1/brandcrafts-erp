package com.brandcrafts.erp.feature.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallanStatus

sealed interface DeliveryChallanListUiEvent { data object Refresh : DeliveryChallanListUiEvent; data object Retry : DeliveryChallanListUiEvent; data class SearchChanged(val query: String) : DeliveryChallanListUiEvent; data class StatusChanged(val status: DeliveryChallanStatus?) : DeliveryChallanListUiEvent; data object CreateIndependentClicked : DeliveryChallanListUiEvent; data object CreateFromInvoiceClicked : DeliveryChallanListUiEvent; data class DetailsClicked(val id: String) : DeliveryChallanListUiEvent; data class EditClicked(val id: String) : DeliveryChallanListUiEvent; data class DispatchClicked(val id: String) : DeliveryChallanListUiEvent; data object DispatchConfirmed : DeliveryChallanListUiEvent; data class CancelClicked(val id: String) : DeliveryChallanListUiEvent; data object CancelConfirmed : DeliveryChallanListUiEvent }
