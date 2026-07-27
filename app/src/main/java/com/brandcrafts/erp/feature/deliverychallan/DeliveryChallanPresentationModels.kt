package com.brandcrafts.erp.feature.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallanSourceType
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import java.math.BigDecimal

data class DeliveryChallanListItem(
    val id: String,
    val number: String,
    val customerId: String,
    val customerName: String?,
    val dateMillis: Long,
    val sourceType: DeliveryChallanSourceType,
    val sourceInvoiceNumber: String?,
    val status: DeliveryChallanStatus,
    val canEdit: Boolean,
    val canDispatch: Boolean,
    val canCancel: Boolean,
)
data class DeliveryChallanCustomerOption(val id: String, val label: String, val deliveryAddress: String)
data class DeliveryChallanMaterialOption(val id: String, val name: String, val unit: String)
data class EditableDeliveryChallanLine(val localId: String, val persistedLineId: String?, val sourceInvoiceLineId: String? = null, val materialId: String?, val description: String, val quantity: BigDecimal?, val unit: String, val errors: DeliveryChallanLineErrors = DeliveryChallanLineErrors())
enum class DeliveryChallanFormMode { INDEPENDENT_CREATE, INVOICE_CREATE, EDIT_DRAFT }
enum class DeliveryChallanFieldError { REQUIRED, INVALID_QUANTITY, INVALID_UNIT, INVALID_DATE, INVALID_SOURCE }
data class DeliveryChallanLineErrors(val materialOrDescription: DeliveryChallanFieldError? = null, val quantity: DeliveryChallanFieldError? = null, val unit: DeliveryChallanFieldError? = null)
data class DeliveryChallanFormErrors(val customer: DeliveryChallanFieldError? = null, val address: DeliveryChallanFieldError? = null, val date: DeliveryChallanFieldError? = null, val sourceInvoice: DeliveryChallanFieldError? = null, val lines: Map<String, DeliveryChallanLineErrors> = emptyMap())
data class DeliveryChallanDetailsModel(val id: String, val number: String, val customer: DeliveryChallanCustomerOption, val deliveryAddress: String, val dateMillis: Long, val sourceType: DeliveryChallanSourceType, val sourceInvoiceNumber: String?, val vehicleNumber: String, val driverName: String, val notes: String, val status: DeliveryChallanStatus, val lines: List<EditableDeliveryChallanLine>)
