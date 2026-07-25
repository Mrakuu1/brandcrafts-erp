package com.brandcrafts.erp.data.model.employee

import com.google.firebase.Timestamp

data class EmployeeDto(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val active: Boolean,
    val firstLogin: Boolean,
    val designation: String,
    val profileImage: String,
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val createdBy: String,
    val updatedBy: String,
)
