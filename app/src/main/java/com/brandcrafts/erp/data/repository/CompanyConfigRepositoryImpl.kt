package com.brandcrafts.erp.data.repository
import com.brandcrafts.erp.data.datasource.company.CompanyConfigRemoteDataSource
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.brandcrafts.erp.domain.repository.CompanyConfigRepository
import javax.inject.Inject
class CompanyConfigRepositoryImpl @Inject constructor(private val source:CompanyConfigRemoteDataSource):CompanyConfigRepository{
    override suspend fun getCompanyConfig(): Result<CompanyConfig> = runCatching {
        source.getCompanyConfig()
    }
}
