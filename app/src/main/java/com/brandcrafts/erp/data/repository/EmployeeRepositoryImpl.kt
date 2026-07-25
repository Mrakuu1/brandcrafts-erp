package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.EmployeeError
import com.brandcrafts.erp.core.result.EmployeeResult
import com.brandcrafts.erp.data.datasource.employee.EmployeeRemoteDataSource
import com.brandcrafts.erp.data.datasource.employee.EmployeeFunctionsDataSource
import com.brandcrafts.erp.data.datasource.employee.EmployeeFunctionEmployee
import com.brandcrafts.erp.data.mapper.toDomain
import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.model.Employee
import com.brandcrafts.erp.domain.model.EmployeeCreateCommand
import com.brandcrafts.erp.domain.model.EmployeeUpdateCommand
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.EmployeeRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class EmployeeRepositoryImpl @Inject constructor(
    private val remoteDataSource: EmployeeRemoteDataSource,
    private val functionsDataSource: EmployeeFunctionsDataSource,
    private val sessionManager: SessionManager,
) : EmployeeRepository {
    override fun observeEmployees(): Flow<EmployeeResult<List<Employee>>> {
        if (adminUser() == null) return flowOf(EmployeeResult.Error(EmployeeError.UNAUTHORIZED))
        return remoteDataSource.observeEmployees()
            .map { employees -> EmployeeResult.Success(employees.map { it.toDomain() }) as EmployeeResult<List<Employee>> }
            .catch { error -> emit(EmployeeResult.Error(error.toEmployeeError())) }
    }

    override suspend fun setEmployeeActive(uid: String, active: Boolean): EmployeeResult<Unit> {
        if (uid.isBlank()) return EmployeeResult.Error(EmployeeError.EMPLOYEE_NOT_FOUND)
        val admin = adminUser() ?: return EmployeeResult.Error(EmployeeError.UNAUTHORIZED)
        return runCatching { remoteDataSource.setEmployeeActive(uid, active, admin.uid, admin.name) }
            .fold(
                onSuccess = { EmployeeResult.Success(Unit) },
                onFailure = { EmployeeResult.Error(it.toEmployeeError()) },
            )
    }

    override suspend fun updateEmployeeRole(uid: String, role: UserRole): EmployeeResult<Unit> {
        if (uid.isBlank()) return EmployeeResult.Error(EmployeeError.EMPLOYEE_NOT_FOUND)
        val admin = adminUser() ?: return EmployeeResult.Error(EmployeeError.UNAUTHORIZED)
        return runCatching { remoteDataSource.updateEmployeeRole(uid, role, admin.uid, admin.name) }
            .fold(
                onSuccess = { EmployeeResult.Success(Unit) },
                onFailure = { EmployeeResult.Error(it.toEmployeeError()) },
            )
    }

    override suspend fun getEmployee(uid: String): EmployeeResult<Employee> {
        if (uid.isBlank()) return EmployeeResult.Error(EmployeeError.EMPLOYEE_NOT_FOUND)
        if (adminUser() == null) return EmployeeResult.Error(EmployeeError.UNAUTHORIZED)
        return runCatching { remoteDataSource.getEmployee(uid)?.toDomain() ?: throw NoSuchElementException() }
            .fold(
                onSuccess = { EmployeeResult.Success(it) },
                onFailure = { EmployeeResult.Error(it.toEmployeeError()) },
            )
    }

    override suspend fun createEmployee(command: EmployeeCreateCommand): EmployeeResult<Employee> {
        if (adminUser() == null) return EmployeeResult.Error(EmployeeError.UNAUTHORIZED)
        return runCatching { functionsDataSource.createEmployee(command).toDomain() }
            .fold(
                onSuccess = { EmployeeResult.Success(it) },
                onFailure = { EmployeeResult.Error(it.toEmployeeError()) },
            )
    }

    override suspend fun updateEmployee(command: EmployeeUpdateCommand): EmployeeResult<Employee> {
        if (adminUser() == null) return EmployeeResult.Error(EmployeeError.UNAUTHORIZED)
        return runCatching { functionsDataSource.updateEmployee(command).toDomain() }
            .fold(
                onSuccess = { EmployeeResult.Success(it) },
                onFailure = { EmployeeResult.Error(it.toEmployeeError()) },
            )
    }

    private fun adminUser(): AuthenticatedUser? =
        (sessionManager.currentUser.value as? CurrentUserState.Authenticated)
            ?.user
            ?.takeIf { it.active && it.role == UserRole.ADMIN }

    private fun Throwable.toEmployeeError(): EmployeeError = when (this) {
        is NoSuchElementException -> EmployeeError.EMPLOYEE_NOT_FOUND
        is FirebaseNetworkException -> EmployeeError.NETWORK_UNAVAILABLE
        is FirebaseFunctionsException -> when (code) {
            FirebaseFunctionsException.Code.UNAUTHENTICATED,
            FirebaseFunctionsException.Code.PERMISSION_DENIED -> EmployeeError.UNAUTHORIZED
            FirebaseFunctionsException.Code.NOT_FOUND -> EmployeeError.EMPLOYEE_NOT_FOUND
            FirebaseFunctionsException.Code.ALREADY_EXISTS -> EmployeeError.DUPLICATE_EMAIL
            FirebaseFunctionsException.Code.INVALID_ARGUMENT -> EmployeeError.VALIDATION_FAILED
            FirebaseFunctionsException.Code.UNAVAILABLE -> EmployeeError.NETWORK_UNAVAILABLE
            else -> EmployeeError.UNKNOWN
        }
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> EmployeeError.UNAUTHORIZED
            FirebaseFirestoreException.Code.NOT_FOUND -> EmployeeError.EMPLOYEE_NOT_FOUND
            FirebaseFirestoreException.Code.UNAVAILABLE -> EmployeeError.NETWORK_UNAVAILABLE
            else -> EmployeeError.UNKNOWN
        }
        else -> EmployeeError.UNKNOWN
    }

    private fun EmployeeFunctionEmployee.toDomain(): Employee = Employee(
        uid = uid, name = name, email = email, phone = phone,
        role = UserRole.entries.firstOrNull { it.name == role } ?: throw IllegalStateException("Invalid employee role"),
        active = active, firstLogin = firstLogin, designation = "", profileImage = "",
        createdAtMillis = null, updatedAtMillis = null, createdBy = "", updatedBy = "",
    )
}
