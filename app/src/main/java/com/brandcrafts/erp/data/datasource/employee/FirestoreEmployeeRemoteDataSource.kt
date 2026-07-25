package com.brandcrafts.erp.data.datasource.employee

import com.brandcrafts.erp.data.mapper.employeeStatusUpdate
import com.brandcrafts.erp.data.mapper.employeeRoleUpdate
import com.brandcrafts.erp.data.mapper.toEmployeeDto
import com.brandcrafts.erp.data.model.employee.EmployeeDto
import com.brandcrafts.erp.domain.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreEmployeeRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : EmployeeRemoteDataSource {
    override fun observeEmployees(): Flow<List<EmployeeDto>> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> close(exception)
                    snapshot != null -> runCatching {
                        snapshot.documents.map { it.toEmployeeDto() }
                    }.onSuccess(::trySend).onFailure(::close)
                }
            }
        awaitClose(registration::remove)
    }

    override suspend fun getEmployee(uid: String): EmployeeDto? =
        firestore.collection(USERS_COLLECTION).document(uid).get().await()
            .takeIf { it.exists() }
            ?.toEmployeeDto()

    override suspend fun setEmployeeActive(
        uid: String,
        active: Boolean,
        updatedBy: String,
        updatedByName: String,
    ) {
        val employee = firestore.collection(USERS_COLLECTION).document(uid)
        val activity = firestore.collection(ACTIVITY_LOGS_COLLECTION).document()
        firestore.runTransaction { transaction ->
            check(transaction.get(employee).exists()) { "Employee not found" }
            transaction.update(employee, employeeStatusUpdate(active = active, updatedBy = updatedBy))
            transaction.set(activity, mapOf(
                "id" to activity.id,
                "module" to "EMPLOYEES",
                "action" to if (active) "ACTIVATE" else "DEACTIVATE",
                "referenceId" to uid,
                "referenceType" to "USER",
                "description" to if (active) "EMPLOYEE_ACTIVATED" else "EMPLOYEE_DEACTIVATED",
                "performedBy" to updatedBy,
                "performedByName" to updatedByName,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ))
        }.await()
    }

    override suspend fun updateEmployeeRole(
        uid: String,
        role: UserRole,
        updatedBy: String,
        updatedByName: String,
    ) {
        val employee = firestore.collection(USERS_COLLECTION).document(uid)
        val activity = firestore.collection(ACTIVITY_LOGS_COLLECTION).document()
        firestore.runTransaction { transaction ->
            check(transaction.get(employee).exists()) { "Employee not found" }
            transaction.update(employee, employeeRoleUpdate(role = role, updatedBy = updatedBy))
            transaction.set(activity, mapOf(
                "id" to activity.id,
                "module" to "EMPLOYEES",
                "action" to "CHANGE_ROLE",
                "referenceId" to uid,
                "referenceType" to "USER",
                "description" to "EMPLOYEE_ROLE_CHANGED",
                "performedBy" to updatedBy,
                "performedByName" to updatedByName,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            ))
        }.await()
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val ACTIVITY_LOGS_COLLECTION = "activity_logs"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
        .addOnFailureListener { continuation.resumeWithException(it) }
}
