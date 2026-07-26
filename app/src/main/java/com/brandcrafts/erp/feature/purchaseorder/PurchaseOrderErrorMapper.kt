package com.brandcrafts.erp.feature.purchaseorder

import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.PurchaseOrderError

internal data class PurchaseOrderMessage(val messageRes:Int,val unauthorized:Boolean=false)
internal fun PurchaseOrderError.toPurchaseOrderMessage()=when(this){
    PurchaseOrderError.Unauthenticated,PurchaseOrderError.AdminAccessRequired,PurchaseOrderError.PermissionDenied->PurchaseOrderMessage(R.string.purchase_order_error_unauthorized,true)
    PurchaseOrderError.UserProfileMissing->PurchaseOrderMessage(R.string.purchase_order_error_user_profile_missing,true)
    PurchaseOrderError.InactiveUser->PurchaseOrderMessage(R.string.purchase_order_error_user_inactive,true)
    PurchaseOrderError.SupplierNotFound->PurchaseOrderMessage(R.string.purchase_order_error_supplier_not_found)
    PurchaseOrderError.SupplierInactive->PurchaseOrderMessage(R.string.purchase_order_error_supplier_inactive)
    PurchaseOrderError.ContactIsNotSupplier->PurchaseOrderMessage(R.string.purchase_order_error_invalid_supplier)
    PurchaseOrderError.PurchaseOrderNotFound->PurchaseOrderMessage(R.string.purchase_order_error_not_found)
    PurchaseOrderError.WrongDocumentType,PurchaseOrderError.MalformedStoredDecimal,PurchaseOrderError.MalformedStoredDate,PurchaseOrderError.InvalidStoredStatus->PurchaseOrderMessage(R.string.purchase_order_error_invalid_data)
    PurchaseOrderError.InvalidStatusTransition->PurchaseOrderMessage(R.string.purchase_order_error_invalid_status_transition)
    PurchaseOrderError.DraftOnlyUpdateRequired->PurchaseOrderMessage(R.string.purchase_order_error_draft_only_edit)
    PurchaseOrderError.PurchaseOrderDateMissing->PurchaseOrderMessage(R.string.purchase_order_error_po_date_required)
    PurchaseOrderError.EmptyItemList->PurchaseOrderMessage(R.string.purchase_order_error_items_required)
    PurchaseOrderError.InvalidQuantity,PurchaseOrderError.InvalidDecimalInput->PurchaseOrderMessage(R.string.purchase_order_error_quantity_invalid)
    PurchaseOrderError.NegativeUnitPrice->PurchaseOrderMessage(R.string.purchase_order_error_unit_price_invalid)
    is PurchaseOrderError.FirestoreOperationLimitExceeded->PurchaseOrderMessage(R.string.purchase_order_error_operation_limit)
    PurchaseOrderError.StockReferenceValidationUnavailable->PurchaseOrderMessage(R.string.purchase_order_error_approved_cancel_unsupported)
    PurchaseOrderError.FirestoreUnavailable->PurchaseOrderMessage(R.string.purchase_order_error_retry)
    PurchaseOrderError.CounterValueMalformed,PurchaseOrderError.CounterValueOverflow,PurchaseOrderError.CounterTransactionFailed,PurchaseOrderError.PurchaseOrderWriteFailed,PurchaseOrderError.LineItemWriteFailed,PurchaseOrderError.ActivityLogWriteFailed,PurchaseOrderError.TransactionAborted,PurchaseOrderError.Unknown->PurchaseOrderMessage(R.string.purchase_order_error_generic)
}
