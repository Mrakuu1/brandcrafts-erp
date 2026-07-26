package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.repository.InvoiceCounter
import com.brandcrafts.erp.data.repository.InvoiceCreateMapBuilder
import com.brandcrafts.erp.data.repository.InvoiceCreatePreparer
import com.brandcrafts.erp.data.repository.InvoiceWritePolicy
import com.brandcrafts.erp.domain.model.InvoiceCreateRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreInvoiceCreateRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val preparer: InvoiceCreatePreparer,
) : InvoiceCreateRemoteDataSource {
    override suspend fun createInvoice(request: InvoiceCreateRequest): String = try {
        val prepared = preparer.prepare(request)
        InvoiceWritePolicy.validateCreate(prepared.lines.size)
        firestore.runTransaction { transaction ->
            val counter = firestore.collection(COUNTERS).document(COUNTER)
            val parent = firestore.collection(INVOICES).document(prepared.parent.invoiceId)
            val counterSnapshot = transaction.get(counter)
            val parentSnapshot = transaction.get(parent)
            if (parentSnapshot.exists()) throw InvoiceFailure(InvoiceError.InvalidGeneratedDocumentId)
            val invoiceNumber = InvoiceCounter.format(InvoiceCounter.next(InvoiceCounter.current(if (counterSnapshot.exists()) counterSnapshot.get("nextNumber") else null)))
            val finalized = prepared.finalize(invoiceNumber)
            transaction.set(counter, InvoiceCreateMapBuilder.counter(InvoiceCounter.next(InvoiceCounter.current(if (counterSnapshot.exists()) counterSnapshot.get("nextNumber") else null))))
            transaction.set(parent, InvoiceCreateMapBuilder.parent(finalized))
            finalized.lines.forEach { line -> transaction.set(parent.collection(ITEMS).document(line.lineId), InvoiceCreateMapBuilder.line(line)) }
            transaction.set(firestore.collection(ACTIVITY_LOGS).document(finalized.activity.activityId), InvoiceCreateMapBuilder.activity(finalized.activity))
            prepared.parent.invoiceId
        }.awaitInvoiceCreate()
    } catch (exception: CancellationException) { throw exception
    } catch (failure: InvoiceFailure) { throw failure
    } catch (exception: FirebaseFirestoreException) { throw InvoiceFailure(when (exception.code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> InvoiceError.PermissionDenied
        FirebaseFirestoreException.Code.UNAVAILABLE -> InvoiceError.FirestoreUnavailable
        else -> InvoiceError.RepositoryUnavailable
    })
    } catch (_: Throwable) { throw InvoiceFailure(InvoiceError.RepositoryUnavailable) }

    private companion object { const val INVOICES="invoices"; const val ITEMS="items"; const val COUNTERS="counters"; const val COUNTER="invoice"; const val ACTIVITY_LOGS="activity_logs" }
}

private suspend fun <T> Task<T>.awaitInvoiceCreate(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
