package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.data.model.employee.EmployeeDto
import com.brandcrafts.erp.domain.model.Employee
import com.brandcrafts.erp.domain.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

fun DocumentSnapshot.toEmployeeDto(): EmployeeDto = EmployeeDto(
    // Existing login profiles use this same users/{uid} schema.  Older
    // profiles can omit display-only values, so optional mapping must not
    // fail the complete employee-list listener.
    // users/{uid} is the profile identity used by login and direct profile
    // reads.  Always preserve the document ID for list actions and edits.
    uid = id,
    name = optionalText("name"),
    email = optionalText("email"),
    phone = optionalText("phone"),
    role = optionalText("role").ifBlank { UserRole.EMPLOYEE.name }.uppercase(),
    active = booleanOrDefault("active", default = false),
    firstLogin = booleanOrDefault("firstLogin", default = false),
    designation = optionalText("designation"),
    profileImage = optionalText("profileImage"),
    createdAt = optionalTimestamp("createdAt"),
    updatedAt = optionalTimestamp("updatedAt"),
    createdBy = optionalText("createdBy"),
    updatedBy = optionalText("updatedBy"),
)

private fun DocumentSnapshot.optionalText(field: String): String =
    (get(field) as? String)?.trim().orEmpty()

private fun DocumentSnapshot.optionalTimestamp(field: String): com.google.firebase.Timestamp? = when (val value = get(field)) {
    is com.google.firebase.Timestamp -> value
    is Number -> value.toLong().takeIf { it > 0 }?.let { com.google.firebase.Timestamp(java.util.Date(it)) }
    is String -> value.toLongOrNull()?.takeIf { it > 0 }?.let { com.google.firebase.Timestamp(java.util.Date(it)) }
    else -> null
}

private fun DocumentSnapshot.booleanOrDefault(field: String, default: Boolean): Boolean = when (val value = get(field)) {
    null -> default
    is Boolean -> value
    is String -> when {
        value.equals("true", ignoreCase = true) -> true
        value.equals("false", ignoreCase = true) -> false
        else -> throw IllegalArgumentException("User $id has invalid $field")
    }
    else -> throw IllegalArgumentException("User $id has invalid $field")
}

fun EmployeeDto.toDomain(): Employee = Employee(
    uid = uid,
    name = name,
    email = email,
    phone = phone,
    role = UserRole.entries.firstOrNull { it.name == role }
        ?: throw IllegalArgumentException("Invalid user role: $role"),
    active = active,
    firstLogin = firstLogin,
    designation = designation,
    profileImage = profileImage,
    createdAtMillis = createdAt?.toDate()?.time,
    updatedAtMillis = updatedAt?.toDate()?.time,
    createdBy = createdBy,
    updatedBy = updatedBy,
)

fun employeeStatusUpdate(active: Boolean, updatedBy: String): Map<String, Any> = mapOf(
    "active" to active,
    "updatedAt" to FieldValue.serverTimestamp(),
    "updatedBy" to updatedBy,
)

fun employeeRoleUpdate(role: UserRole, updatedBy: String): Map<String, Any> = mapOf(
    "role" to role.name,
    "updatedAt" to FieldValue.serverTimestamp(),
    "updatedBy" to updatedBy,
)
