package com.brandcrafts.erp.data.datasource.quotation
import com.brandcrafts.erp.domain.model.Quotation
import kotlinx.coroutines.flow.Flow
interface QuotationRemoteDataSource { fun observeQuotations():Flow<List<Quotation>> }
