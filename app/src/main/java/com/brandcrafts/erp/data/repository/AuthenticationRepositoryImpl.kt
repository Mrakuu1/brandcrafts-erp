package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.AppResult
import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.data.datasource.auth.FirebaseAuthenticationDataSource
import com.brandcrafts.erp.data.mapper.toAuthenticatedUser
import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.repository.AuthenticationRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import javax.inject.Inject

class AuthenticationRepositoryImpl @Inject constructor(private val source: FirebaseAuthenticationDataSource) : AuthenticationRepository {
    override suspend fun login(email: String, password: String): AppResult<AuthenticatedUser> = try {
        val uid = source.signIn(email, password)
        val user = source.userProfile(uid)?.toAuthenticatedUser() ?: run {
            source.signOut()
            return AppResult.Error(AuthenticationError.USER_PROFILE_MISSING)
        }
        if (!user.active) { source.signOut(); AppResult.Error(AuthenticationError.ACCOUNT_DISABLED) } else AppResult.Success(user)
    } catch (error: Throwable) {
        source.signOut()
        AppResult.Error(error.toAuthenticationError())
    }
    override suspend fun resetPassword(email: String): AppResult<Unit> = try {
        source.sendPasswordReset(email)
        AppResult.Success(Unit)
    } catch (error: Throwable) {
        when (error) {
            is FirebaseAuthInvalidUserException,
            is FirebaseAuthInvalidCredentialsException -> AppResult.Success(Unit)
            else -> AppResult.Error(error.toAuthenticationError())
        }
    }
    override suspend fun logout(): AppResult<Unit> = try { source.signOut(); AppResult.Success(Unit) } catch (error: Throwable) { AppResult.Error(error.toAuthenticationError()) }
    override suspend fun getCurrentUser(): AppResult<AuthenticatedUser?> = try {
        val uid = source.currentUserId() ?: return AppResult.Success(null)
        val user = source.userProfile(uid)?.toAuthenticatedUser() ?: run {
            source.signOut()
            return AppResult.Error(AuthenticationError.USER_PROFILE_MISSING)
        }
        if (user.active) AppResult.Success(user) else { source.signOut(); AppResult.Error(AuthenticationError.ACCOUNT_DISABLED) }
    } catch (error: Throwable) { AppResult.Error(error.toAuthenticationError()) }
}
private fun Throwable.toAuthenticationError() = when (this) {
    is FirebaseAuthInvalidCredentialsException -> AuthenticationError.INVALID_CREDENTIALS
    is FirebaseNetworkException -> AuthenticationError.NETWORK_UNAVAILABLE
    else -> AuthenticationError.UNKNOWN
}
