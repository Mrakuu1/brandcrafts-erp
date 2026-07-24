package com.brandcrafts.erp.domain.model
data class InventoryTransaction(val id:String,val materialId:String,val type:Type,val quantity:Double,val unit:String,val referenceId:String,val referenceType:String,val remarks:String,val performedBy:String,val createdAtMillis:Long?){enum class Type{STOCK_IN,STOCK_OUT,MATERIAL_USAGE}}
