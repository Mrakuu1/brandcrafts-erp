package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.model.UserRole
import com.google.firebase.firestore.DocumentSnapshot

fun DocumentSnapshot.toAuthenticatedUser(): AuthenticatedUser = AuthenticatedUser(
    uid = getString("uid") ?: id, name = getString("name") ?: "", email = getString("email") ?: "", phone = getString("phone") ?: "",
    role = UserRole.valueOf(getString("role") ?: "EMPLOYEE"), active = getBoolean("active") ?: false, firstLogin = getBoolean("firstLogin") ?: false,
    designation = getString("designation") ?: "", profileImage = getString("profileImage") ?: "", createdAtMillis = getTimestamp("createdAt")?.toDate()?.time,
    updatedAtMillis = getTimestamp("updatedAt")?.toDate()?.time, createdBy = getString("createdBy") ?: "", updatedBy = getString("updatedBy") ?: "",
)
