package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.datasource.invoice.InvoiceCancellationRemoteDataSource
import com.brandcrafts.erp.data.datasource.invoice.InvoiceCreateRemoteDataSource
import com.brandcrafts.erp.data.datasource.invoice.InvoiceDraftUpdateRemoteDataSource
import com.brandcrafts.erp.data.datasource.invoice.InvoiceIssueRemoteDataSource
import com.brandcrafts.erp.data.datasource.invoice.InvoicePaymentRemoteDataSource
import com.brandcrafts.erp.data.datasource.invoice.InvoiceRemoteDataSource
import com.brandcrafts.erp.data.mapper.toDomain
import com.brandcrafts.erp.domain.model.Invoice
import com.brandcrafts.erp.domain.model.InvoiceCreateRequest
import com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest
import com.brandcrafts.erp.domain.model.InvoiceLine
import com.brandcrafts.erp.domain.model.InvoicePaymentRequest
import com.brandcrafts.erp.domain.model.InvoiceSummary
import com.brandcrafts.erp.domain.repository.InvoiceRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class InvoiceRepositoryImpl @Inject constructor(
    private val readDataSource: InvoiceRemoteDataSource,
    private val createDataSource: InvoiceCreateRemoteDataSource,
    private val draftUpdateDataSource: InvoiceDraftUpdateRemoteDataSource,
    private val issueDataSource: InvoiceIssueRemoteDataSource,
    private val cancellationDataSource: InvoiceCancellationRemoteDataSource,
    private val paymentDataSource: InvoicePaymentRemoteDataSource,
) : InvoiceRepository {

    override fun observeInvoices(): Flow<Result<List<InvoiceSummary>>> =
        readDataSource.observeInvoiceParents()
            .map { Result.success(it) }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(Result.failure(mapFailure(exception)))
            }

    override suspend fun getInvoice(id: String): Result<Invoice> = invoiceResult {
        val parent = readDataSource.getInvoiceParent(id)
            ?: throw InvoiceFailure(InvoiceError.InvoiceNotFound)
        val parentId = parent.id ?: throw InvoiceFailure(InvoiceError.InvoiceNotFound)
        val lines = readDataSource.getInvoiceLines(parentId).map { line -> line.toDomain(line.itemId ?: throw InvoiceFailure(InvoiceError.InvalidLine)) }
        parent.toDomain(lines)
    }

    override suspend fun getInvoiceLines(invoiceId: String): Result<List<InvoiceLine>> =
        invoiceResult {
            readDataSource.getInvoiceLines(invoiceId).map { line -> line.toDomain(line.itemId ?: throw InvoiceFailure(InvoiceError.InvalidLine)) }
        }

    override suspend fun createInvoice(request: InvoiceCreateRequest): Result<String> =
        invoiceResult { createDataSource.createInvoice(request) }

    override suspend fun updateDraftInvoice(request: InvoiceDraftUpdateRequest): Result<Unit> =
        invoiceResult { draftUpdateDataSource.updateDraftInvoice(request) }

    override suspend fun issueInvoice(invoiceId: String): Result<Unit> =
        invoiceResult { issueDataSource.issueInvoice(invoiceId) }

    override suspend fun cancelInvoice(invoiceId: String): Result<Unit> =
        invoiceResult { cancellationDataSource.cancelInvoice(invoiceId) }

    override suspend fun recordPayment(request: InvoicePaymentRequest): Result<Unit> =
        invoiceResult { paymentDataSource.recordPayment(request) }

    private suspend fun <T> invoiceResult(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: CancellationException) {
        throw exception
    } catch (failure: InvoiceFailure) {
        Result.failure(failure)
    } catch (exception: Throwable) {
        Result.failure(mapFailure(exception))
    }

    private fun mapFailure(exception: Throwable): InvoiceFailure = when (exception) {
        is InvoiceFailure -> exception
        is FirebaseFirestoreException -> InvoiceFailure(
            when (exception.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> InvoiceError.PermissionDenied
                FirebaseFirestoreException.Code.UNAVAILABLE -> InvoiceError.FirestoreUnavailable
                else -> InvoiceError.RepositoryUnavailable
            },
        )
        else -> InvoiceFailure(InvoiceError.RepositoryUnavailable)
    }
}
