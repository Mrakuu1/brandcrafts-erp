package com.brandcrafts.erp.feature.deliverychallan
import androidx.annotation.StringRes
sealed interface DeliveryChallanFormUiEffect { data class Saved(val id:String):DeliveryChallanFormUiEffect; data class ShowMessage(@StringRes val id:Int):DeliveryChallanFormUiEffect; data object Unauthorized:DeliveryChallanFormUiEffect }
