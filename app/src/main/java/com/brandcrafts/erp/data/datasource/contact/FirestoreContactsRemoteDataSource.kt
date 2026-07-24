package com.brandcrafts.erp.data.datasource.contact

import com.brandcrafts.erp.data.mapper.toContactDto
import com.brandcrafts.erp.data.mapper.toCreateMap
import com.brandcrafts.erp.data.mapper.toUpdateMap
import com.brandcrafts.erp.data.model.contact.ContactDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreContactsRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ContactsRemoteDataSource {
    override fun observeContacts(): Flow<List<ContactDto>> = callbackFlow {
        val registration = firestore.collection(CONTACTS_COLLECTION)
            .addSnapshotListener { snapshot, exception ->
                when {
                    exception != null -> close(exception)
                    snapshot != null -> runCatching {
                        snapshot.documents.map { it.toContactDto() }
                    }.onSuccess { trySend(it) }.onFailure { close(it) }
                }
            }
        awaitClose(registration::remove)
    }

    override suspend fun getContact(id: String): ContactDto? = firestore
        .collection(CONTACTS_COLLECTION)
        .document(id)
        .get()
        .await()
        .takeIf { it.exists() }
        ?.toContactDto()

    override suspend fun createContact(contact: ContactDto, performedByName: String) {
        checkUniqueValues(contact.phone, contact.email, excludingId = null)
        val document = firestore.collection(CONTACTS_COLLECTION).document()
        val activity = firestore.collection(ACTIVITY_LOGS_COLLECTION).document()
        firestore.runTransaction { transaction ->
            transaction.set(document, contact.toCreateMap(document.id))
            transaction.set(activity, activityMap(
                id = activity.id,
                referenceId = document.id,
                action = "CREATE",
                description = "CONTACT_CREATED",
                performedBy = contact.createdBy,
                performedByName = performedByName,
            ))
        }.await()
    }

    override suspend fun updateContact(contact: ContactDto, performedByName: String) {
        checkUniqueValues(contact.phone, contact.email, excludingId = contact.id)
        val document = firestore.collection(CONTACTS_COLLECTION).document(contact.id)
        val activity = firestore.collection(ACTIVITY_LOGS_COLLECTION).document()
        firestore.runTransaction { transaction ->
            check(transaction.get(document).exists()) { "Contact not found" }
            transaction.update(document, contact.toUpdateMap())
            transaction.set(activity, activityMap(
                id = activity.id,
                referenceId = document.id,
                action = "UPDATE",
                description = "CONTACT_UPDATED",
                performedBy = contact.updatedBy,
                performedByName = performedByName,
            ))
        }.await()
    }

    private suspend fun checkUniqueValues(
        phone: String,
        email: String,
        excludingId: String?,
    ) {
        val phoneInUse = firestore.collection(CONTACTS_COLLECTION)
            .whereEqualTo("phone", phone)
            .get()
            .await()
            .documents
            .any { it.id != excludingId }
        if (phoneInUse) throw DuplicatePhoneException

        if (email.isNotBlank()) {
            val emailInUse = firestore.collection(CONTACTS_COLLECTION)
                .whereEqualTo("email", email)
                .get()
                .await()
                .documents
                .any { it.id != excludingId }
            if (emailInUse) throw DuplicateEmailException
        }
    }

    private fun activityMap(
        id: String,
        referenceId: String,
        action: String,
        description: String,
        performedBy: String,
        performedByName: String,
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "module" to "CONTACTS",
        "action" to action,
        "referenceId" to referenceId,
        "referenceType" to "CONTACT",
        "description" to description,
        "performedBy" to performedBy,
        "performedByName" to performedByName,
        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
    )

    private companion object {
        const val CONTACTS_COLLECTION = "contacts"
        const val ACTIVITY_LOGS_COLLECTION = "activity_logs"
    }
}

internal data object DuplicatePhoneException : IllegalStateException()
internal data object DuplicateEmailException : IllegalStateException()

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
        .addOnFailureListener { continuation.resumeWithException(it) }
}
