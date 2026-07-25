package com.brandcrafts.erp.domain.repository
import com.brandcrafts.erp.domain.model.CompanyConfig
interface CompanyConfigRepository { suspend fun getCompanyConfig(): Result<CompanyConfig> }
