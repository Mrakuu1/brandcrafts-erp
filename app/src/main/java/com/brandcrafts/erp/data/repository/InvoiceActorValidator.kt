package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.UserRole
import javax.inject.Inject

data class InvoiceValidatedActor(val userId: String, val displayName: String)

class InvoiceActorValidator @Inject constructor(
    private val sessionManager: SessionManager,
) {
    fun requireAdmin(): InvoiceValidatedActor {
        val user = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user
            ?: throw InvoiceFailure(InvoiceError.Unauthenticated)
        if (user.uid.isBlank()) throw InvoiceFailure(InvoiceError.UserProfileMissing)
        if (!user.active) throw InvoiceFailure(InvoiceError.InactiveUser)
        if (user.role != UserRole.ADMIN) throw InvoiceFailure(InvoiceError.AdminAccessRequired)
        return InvoiceValidatedActor(userId = user.uid, displayName = user.name)
    }
}
