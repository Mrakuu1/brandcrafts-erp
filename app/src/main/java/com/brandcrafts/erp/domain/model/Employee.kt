package com.brandcrafts.erp.domain.model

data class Employee(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val active: Boolean,
    val firstLogin: Boolean,
    val designation: String,
    val profileImage: String,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val createdBy: String,
    val updatedBy: String,
)
