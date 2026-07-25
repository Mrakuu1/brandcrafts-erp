package com.brandcrafts.erp.data.datasource.auth

import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthenticationDataSource {
    suspend fun signIn(email: String, password: String): String
    suspend fun sendPasswordReset(email: String)
    fun currentUserId(): String?
    suspend fun userProfile(uid: String): DocumentSnapshot?
    fun observeUserProfile(uid: String): Flow<DocumentSnapshot>
    fun signOut()
}
