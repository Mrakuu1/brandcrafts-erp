package com.brandcrafts.erp.data.datasource.employee

import com.brandcrafts.erp.domain.model.EmployeeCreateCommand
import com.brandcrafts.erp.domain.model.EmployeeUpdateCommand
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseEmployeeFunctionsDataSource @Inject constructor(
    private val functions: FirebaseFunctions,
) : EmployeeFunctionsDataSource {
    override suspend fun createEmployee(command: EmployeeCreateCommand): EmployeeFunctionEmployee =
        call(CREATE_EMPLOYEE, command.toMap()).toEmployee()

    override suspend fun updateEmployee(command: EmployeeUpdateCommand): EmployeeFunctionEmployee =
        call(UPDATE_EMPLOYEE, command.toMap()).toEmployee()

    private suspend fun call(name: String, data: Map<String, Any>): Map<*, *> =
        functions.getHttpsCallable(name).call(data).await().data as? Map<*, *>
            ?: throw IllegalStateException("Invalid employee function response")

    private fun EmployeeCreateCommand.toMap(): Map<String, Any> = mapOf(
        "name" to name, "email" to email, "phone" to phone, "role" to role.name,
        "active" to active, "temporaryPassword" to temporaryPassword,
    )

    private fun EmployeeUpdateCommand.toMap(): Map<String, Any> = mapOf(
        "uid" to uid, "name" to name, "email" to email, "phone" to phone,
        "role" to role.name, "active" to active,
    )

    private fun Map<*, *>.toEmployee(): EmployeeFunctionEmployee = EmployeeFunctionEmployee(
        uid = this["uid"] as? String ?: error("Missing employee uid"),
        name = this["name"] as? String ?: error("Missing employee name"),
        email = this["email"] as? String ?: error("Missing employee email"),
        phone = this["phone"] as? String ?: error("Missing employee phone"),
        role = this["role"] as? String ?: error("Missing employee role"),
        active = this["active"] as? Boolean ?: error("Missing employee active state"),
        firstLogin = this["firstLogin"] as? Boolean ?: false,
    )

    private companion object {
        const val CREATE_EMPLOYEE = "createEmployee"
        const val UPDATE_EMPLOYEE = "updateEmployee"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
        .addOnFailureListener { continuation.resumeWithException(it) }
}
