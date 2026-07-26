package com.brandcrafts.erp.feature.purchaseorder

import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import java.math.BigDecimal

sealed interface PurchaseOrderFormMode { data object Create : PurchaseOrderFormMode; data class Edit(val purchaseOrderId:String) : PurchaseOrderFormMode }
data class PurchaseOrderListItemUi(val id:String,val number:String,val supplierName:String?,val dateMillis:Long?,val expectedDeliveryDateMillis:Long?,val total:BigDecimal,val status:PurchaseOrderStatus,val canEdit:Boolean,val canApprove:Boolean,val canCancel:Boolean)
data class PurchaseOrderSupplierOption(val id:String,val name:String,val company:String)
data class PurchaseOrderInventoryOption(val id:String,val name:String,val unit:String)
enum class PurchaseOrderFieldError { REQUIRED, MALFORMED, OUT_OF_RANGE, LIMIT_EXCEEDED }
data class EditablePurchaseOrderLine(val localId:String=java.util.UUID.randomUUID().toString(),val lineId:String?=null,val materialId:String?=null,val description:String="",val quantity:String="",val unit:String="",val unitPrice:String="",val lineTotal:BigDecimal?=null,val descriptionError:PurchaseOrderFieldError?=null,val quantityError:PurchaseOrderFieldError?=null,val unitError:PurchaseOrderFieldError?=null,val unitPriceError:PurchaseOrderFieldError?=null)
data class PurchaseOrderDetailsUi(val id:String,val number:String,val supplier:PurchaseOrderSupplierOption?,val dateMillis:Long?,val expectedDeliveryDateMillis:Long?,val status:PurchaseOrderStatus,val lines:List<EditablePurchaseOrderLine>,val total:BigDecimal,val remarks:String,val canEdit:Boolean,val canApprove:Boolean,val canCancel:Boolean)
