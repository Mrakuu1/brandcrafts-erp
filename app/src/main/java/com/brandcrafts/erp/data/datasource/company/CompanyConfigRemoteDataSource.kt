package com.brandcrafts.erp.data.datasource.company
import com.brandcrafts.erp.domain.model.CompanyConfig
interface CompanyConfigRemoteDataSource { suspend fun getCompanyConfig(): CompanyConfig }
