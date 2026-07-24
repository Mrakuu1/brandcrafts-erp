package com.brandcrafts.erp.data.model.contact

import com.google.firebase.Timestamp

data class ContactDto(
    val id: String,
    val type: String,
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
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val createdBy: String,
    val updatedBy: String,
)
