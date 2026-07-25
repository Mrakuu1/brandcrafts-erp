package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.data.model.employee.EmployeeDto
import com.brandcrafts.erp.domain.model.Employee
import com.brandcrafts.erp.domain.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

fun DocumentSnapshot.toEmployeeDto(): EmployeeDto = EmployeeDto(
    uid = getString("uid") ?: id,
    name = requireNotNull(getString("name")) { "User $id is missing name" },
    email = requireNotNull(getString("email")) { "User $id is missing email" },
    phone = getString("phone") ?: "",
    role = requireNotNull(getString("role")) { "User $id is missing role" },
    active = getBoolean("active") ?: false,
    firstLogin = getBoolean("firstLogin") ?: false,
    designation = getString("designation") ?: "",
    profileImage = getString("profileImage") ?: "",
    createdAt = getTimestamp("createdAt"),
    updatedAt = getTimestamp("updatedAt"),
    createdBy = getString("createdBy") ?: "",
    updatedBy = getString("updatedBy") ?: "",
)

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
