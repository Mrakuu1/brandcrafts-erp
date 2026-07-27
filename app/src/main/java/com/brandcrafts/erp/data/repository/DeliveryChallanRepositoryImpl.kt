package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.datasource.deliverychallan.DeliveryChallanCancellationRemoteDataSource
import com.brandcrafts.erp.data.datasource.deliverychallan.DeliveryChallanCreateRemoteDataSource
import com.brandcrafts.erp.data.datasource.deliverychallan.DeliveryChallanDispatchRemoteDataSource
import com.brandcrafts.erp.data.datasource.deliverychallan.DeliveryChallanDraftUpdateRemoteDataSource
import com.brandcrafts.erp.data.datasource.deliverychallan.DeliveryChallanInvoiceCreateRemoteDataSource
import com.brandcrafts.erp.data.datasource.deliverychallan.DeliveryChallanRemoteDataSource
import com.brandcrafts.erp.data.mapper.toDomain
import com.brandcrafts.erp.data.mapper.toSummaryDomain
import com.brandcrafts.erp.domain.model.DeliveryChallan
import com.brandcrafts.erp.domain.model.DeliveryChallanCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanDispatchRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanDraftUpdateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanInvoiceCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanSummary
import com.brandcrafts.erp.domain.repository.DeliveryChallanRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class DeliveryChallanRepositoryImpl @Inject constructor(
    private val reads: DeliveryChallanRemoteDataSource,
    private val independentCreate: DeliveryChallanCreateRemoteDataSource,
    private val invoiceCreate: DeliveryChallanInvoiceCreateRemoteDataSource,
    private val draftUpdate: DeliveryChallanDraftUpdateRemoteDataSource,
    private val dispatch: DeliveryChallanDispatchRemoteDataSource,
    private val cancellation: DeliveryChallanCancellationRemoteDataSource,
) : DeliveryChallanRepository {
    override fun observeDeliveryChallans(): Flow<Result<List<DeliveryChallanSummary>>> = reads.observeParents().map { parents -> Result.success(parents.map { it.toSummaryDomain() }) }.catch { exception -> if (exception is CancellationException) throw exception; emit(Result.failure(mapFailure(exception))) }
    override suspend fun getDeliveryChallan(challanId: String): Result<DeliveryChallan> = result { val parent = reads.getParent(challanId) ?: throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound); val id = parent.id ?: throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound); parent.toDomain(reads.getLines(id).map { it.toDomain(it.itemId ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)) }) }
    override suspend fun getDeliveryChallanLines(challanId: String) = result { reads.getLines(challanId).map { it.toDomain(it.itemId ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)) } }
    override suspend fun createDeliveryChallan(request: DeliveryChallanCreateRequest): Result<String> = result { independentCreate.createIndependent(request) }
    override suspend fun createDeliveryChallanFromInvoice(request: DeliveryChallanInvoiceCreateRequest): Result<String> = result { invoiceCreate.createFromInvoice(request) }
    override suspend fun updateDraftDeliveryChallan(request: DeliveryChallanDraftUpdateRequest): Result<Unit> = result { draftUpdate.updateDraft(request) }
    override suspend fun dispatchDeliveryChallan(request: DeliveryChallanDispatchRequest): Result<Unit> = result { dispatch.dispatch(request) }
    override suspend fun cancelDraftDeliveryChallan(challanId: String): Result<Unit> = result { cancellation.cancelDraft(challanId) }
    private suspend fun <T> result(block: suspend () -> T): Result<T> = try { Result.success(block()) } catch (exception: CancellationException) { throw exception } catch (failure: DeliveryChallanFailure) { Result.failure(failure) } catch (exception: Throwable) { Result.failure(mapFailure(exception)) }
    private fun mapFailure(exception: Throwable): DeliveryChallanFailure = if (exception is DeliveryChallanFailure) exception else DeliveryChallanFailure(DeliveryChallanError.Unknown)
}
