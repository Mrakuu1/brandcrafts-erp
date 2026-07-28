package com.brandcrafts.erp.domain.model

/** Immutable audit record used by the dashboard's recent-activity section. */
data class ActivityLog(
    val id: String,
    val module: String,
    val action: String,
    val referenceId: String,
    val referenceType: String,
    val description: String,
    val performedBy: String,
    val performedByName: String?,
    val createdAtMillis: Long?,
)
