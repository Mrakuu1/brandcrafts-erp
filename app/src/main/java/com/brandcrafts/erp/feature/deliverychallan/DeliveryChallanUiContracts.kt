package com.brandcrafts.erp.feature.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallanStatus

sealed interface DeliveryChallanListContent { data object Loading : DeliveryChallanListContent; data object Empty : DeliveryChallanListContent; data object Loaded : DeliveryChallanListContent; data object Error : DeliveryChallanListContent }
data class DeliveryChallanListUiState(
    val content: DeliveryChallanListContent = DeliveryChallanListContent.Loading,
    val searchQuery: String = "",
    val statusFilter: DeliveryChallanStatus? = null,
    val rows: List<DeliveryChallanListItem> = emptyList(),
    val canCreateIndependent: Boolean = false,
    val canCreateFromInvoice: Boolean = false,
    val isRefreshing: Boolean = false,
    val actionInProgress: DeliveryChallanListOperation? = null,
)

sealed interface DeliveryChallanListOperation {
    val challanId: String

    data class Dispatch(override val challanId: String) : DeliveryChallanListOperation
    data class Cancel(override val challanId: String) : DeliveryChallanListOperation
}
sealed interface DeliveryChallanFormContent { data object Loading : DeliveryChallanFormContent; data object Ready : DeliveryChallanFormContent; data object Error : DeliveryChallanFormContent }
data class DeliveryChallanFormUiState(val content: DeliveryChallanFormContent = DeliveryChallanFormContent.Loading, val mode: DeliveryChallanFormMode = DeliveryChallanFormMode.INDEPENDENT_CREATE, val challanId: String? = null, val customerOptions: List<DeliveryChallanCustomerOption> = emptyList(), val materialOptions: List<DeliveryChallanMaterialOption> = emptyList(), val selectedCustomerId: String? = null, val deliveryAddress: String = "", val dateMillis: Long? = null, val sourceInvoiceId: String? = null, val sourceInvoiceNumber: String? = null, val vehicleNumber: String = "", val driverName: String = "", val notes: String = "", val lines: List<EditableDeliveryChallanLine> = emptyList(), val errors: DeliveryChallanFormErrors = DeliveryChallanFormErrors(), val isSaving: Boolean = false)
sealed interface DeliveryChallanDetailsContent { data object Loading : DeliveryChallanDetailsContent; data object Loaded : DeliveryChallanDetailsContent; data object Error : DeliveryChallanDetailsContent }
data class DeliveryChallanDetailsUiState(val content: DeliveryChallanDetailsContent = DeliveryChallanDetailsContent.Loading, val challan: DeliveryChallanDetailsModel? = null, val canEdit: Boolean = false, val canDispatch: Boolean = false, val canCancel: Boolean = false, val isOperating: Boolean = false, val isGeneratingPdf: Boolean = false)
