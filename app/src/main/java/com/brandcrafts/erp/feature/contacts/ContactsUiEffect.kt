package com.brandcrafts.erp.feature.contacts
sealed interface ContactsUiEffect{data object RequestAddCustomer:ContactsUiEffect;data object RequestAddSupplier:ContactsUiEffect;data class RequestEditCustomer(val id:String):ContactsUiEffect;data class RequestEditSupplier(val id:String):ContactsUiEffect;data object ShowUnavailableFeature:ContactsUiEffect}
