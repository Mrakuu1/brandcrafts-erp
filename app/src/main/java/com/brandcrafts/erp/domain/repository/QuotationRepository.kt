package com.brandcrafts.erp.domain.repository
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationDraft
import com.brandcrafts.erp.domain.model.QuotationStatus
import kotlinx.coroutines.flow.Flow
interface QuotationRepository {
    fun observeQuotations(): Flow<Result<List<Quotation>>>
    suspend fun getQuotation(id: String): Result<Quotation>
    suspend fun createQuotation(draft: QuotationDraft): Result<String>
    suspend fun updateQuotation(id: String, draft: QuotationDraft): Result<Unit>
    suspend fun updateQuotationStatus(id: String, status: QuotationStatus): Result<Unit>
}
