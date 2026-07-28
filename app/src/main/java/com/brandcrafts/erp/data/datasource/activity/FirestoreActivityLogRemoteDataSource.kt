package com.brandcrafts.erp.data.datasource.activity

import com.brandcrafts.erp.domain.model.ActivityLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreActivityLogRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ActivityLogRemoteDataSource {
    override fun observeRecentActivities(limit: Long): Flow<List<ActivityLog>> = callbackFlow {
        require(limit > 0) { "Activity limit must be positive" }
        val registration = firestore.collection(COLLECTION)
            .orderBy(CREATED_AT, Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> {
                        // A malformed record must not hide all valid activity.
                        val records = snapshot.documents.mapNotNull(::mapOrNull)
                        trySend(records)
                    }
                }
            }
        awaitClose(registration::remove)
    }

    private fun mapOrNull(document: DocumentSnapshot): ActivityLog? {
        val id = document.id.takeIf(String::isNotBlank) ?: return null
        val module = document.getString(MODULE)?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val action = document.getString(ACTION)?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val referenceId = document.getString(REFERENCE_ID).orEmpty()
        val referenceType = document.getString(REFERENCE_TYPE).orEmpty()
        val description = document.getString(DESCRIPTION).orEmpty()
        val performedBy = document.getString(PERFORMED_BY).orEmpty()
        val timestamp = when (val value = document.get(CREATED_AT)) {
            is Timestamp -> value.toDate().time
            is Number -> value.toLong().takeIf { it > 0 }
            is String -> value.toLongOrNull()?.takeIf { it > 0 }
            else -> null // unresolved server timestamp is rendered without a date.
        }
        return ActivityLog(id, module, action, referenceId, referenceType, description, performedBy,
            document.getString(PERFORMED_BY_NAME)?.takeIf(String::isNotBlank), timestamp)
    }

    private companion object {
        const val COLLECTION = "activity_logs"
        const val MODULE = "module"
        const val ACTION = "action"
        const val REFERENCE_ID = "referenceId"
        const val REFERENCE_TYPE = "referenceType"
        const val DESCRIPTION = "description"
        const val PERFORMED_BY = "performedBy"
        const val PERFORMED_BY_NAME = "performedByName"
        const val CREATED_AT = "createdAt"
    }
}
