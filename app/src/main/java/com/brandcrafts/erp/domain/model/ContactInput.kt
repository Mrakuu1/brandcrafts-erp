package com.brandcrafts.erp.domain.model

data class ContactInput(
    val type: ContactType,
    val name: String,
    val company: String,
    val phone: String,
    val email: String,
    val address: String,
    val gstNumber: String,
    val city: String,
    val state: String,
    val pincode: String,
    val notes: String,
    val active: Boolean,
)
