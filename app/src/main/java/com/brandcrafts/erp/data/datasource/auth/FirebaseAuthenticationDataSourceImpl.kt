package com.brandcrafts.erp.data.datasource.auth

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject

class FirebaseAuthenticationDataSourceImpl @Inject constructor(private val auth: FirebaseAuth, private val firestore: FirebaseFirestore) : FirebaseAuthenticationDataSource {
    override suspend fun signIn(email: String, password: String): String = auth.signInWithEmailAndPassword(email, password).await().user?.uid ?: error("Missing authenticated user")
    override suspend fun currentUserId(): String? = auth.currentUser?.uid
    override suspend fun userProfile(uid: String): DocumentSnapshot? = firestore.collection("users").document(uid).get().await().takeIf { it.exists() }
    override fun signOut() = auth.signOut()
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) }
}
