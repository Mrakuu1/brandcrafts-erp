package com.brandcrafts.erp.core.common

import com.brandcrafts.erp.domain.model.AuthenticatedUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface CurrentUserState {
    data object Unauthenticated : CurrentUserState
    data class Authenticated(val user: AuthenticatedUser) : CurrentUserState
}

@Singleton
class SessionManager @Inject constructor() {
    private val _currentUser = MutableStateFlow<CurrentUserState>(CurrentUserState.Unauthenticated)
    val currentUser: StateFlow<CurrentUserState> = _currentUser.asStateFlow()

    fun setCurrentUser(user: AuthenticatedUser) {
        _currentUser.value = CurrentUserState.Authenticated(user)
    }

    fun clearCurrentUser() {
        _currentUser.value = CurrentUserState.Unauthenticated
    }
}
