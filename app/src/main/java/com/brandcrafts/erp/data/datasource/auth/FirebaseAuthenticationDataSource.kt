package com.brandcrafts.erp.data.datasource.auth

import com.google.firebase.firestore.DocumentSnapshot

interface FirebaseAuthenticationDataSource {
    suspend fun signIn(email: String, password: String): String
    suspend fun currentUserId(): String?
    suspend fun userProfile(uid: String): DocumentSnapshot?
    fun signOut()
}
