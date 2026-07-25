package com.brandcrafts.erp.domain.model

data class CompanyConfig(val companyName:String,val legalName:String,val addressLine1:String,val addressLine2:String,val city:String,val state:String,val pincode:String,val country:String,val phone:String,val email:String,val website:String,val gstNumber:String,val logoUrl:String,val quotationTerms:String,val authorizedSignatoryName:String,val authorizedSignatoryDesignation:String,val signatureImageUrl:String,val updatedAtMillis:Long?,val updatedBy:String)
