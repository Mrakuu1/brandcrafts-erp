package com.brandcrafts.erp.di

import com.brandcrafts.erp.data.datasource.auth.FirebaseAuthenticationDataSource
import com.brandcrafts.erp.data.datasource.auth.FirebaseAuthenticationDataSourceImpl
import com.brandcrafts.erp.data.datasource.contact.ContactsRemoteDataSource
import com.brandcrafts.erp.data.datasource.contact.FirestoreContactsRemoteDataSource
import com.brandcrafts.erp.data.datasource.employee.EmployeeRemoteDataSource
import com.brandcrafts.erp.data.datasource.employee.EmployeeFunctionsDataSource
import com.brandcrafts.erp.data.datasource.employee.FirebaseEmployeeFunctionsDataSource
import com.brandcrafts.erp.data.datasource.employee.FirestoreEmployeeRemoteDataSource
import com.brandcrafts.erp.data.datasource.inventory.FirestoreInventoryRemoteDataSource
import com.brandcrafts.erp.data.datasource.inventory.InventoryRemoteDataSource
import com.brandcrafts.erp.data.datasource.stock.FirestoreStockRemoteDataSource
import com.brandcrafts.erp.data.datasource.stock.StockRemoteDataSource
import com.brandcrafts.erp.data.datasource.stock.FirestoreStockOutRemoteDataSource
import com.brandcrafts.erp.data.datasource.stock.StockOutRemoteDataSource
import com.brandcrafts.erp.data.repository.AuthenticationRepositoryImpl
import com.brandcrafts.erp.data.repository.ContactRepositoryImpl
import com.brandcrafts.erp.data.repository.EmployeeRepositoryImpl
import com.brandcrafts.erp.data.repository.InventoryRepositoryImpl
import com.brandcrafts.erp.data.repository.StockRepositoryImpl
import com.brandcrafts.erp.data.repository.StockOutRepositoryImpl
import com.brandcrafts.erp.data.repository.MaterialUsageRepositoryImpl
import com.brandcrafts.erp.data.repository.InventoryTransactionRepositoryImpl
import com.brandcrafts.erp.data.datasource.quotation.QuotationRemoteDataSource
import com.brandcrafts.erp.data.datasource.quotation.FirestoreQuotationRemoteDataSource
import com.brandcrafts.erp.data.repository.QuotationRepositoryImpl
import com.brandcrafts.erp.data.datasource.company.CompanyConfigRemoteDataSource
import com.brandcrafts.erp.data.datasource.company.FirestoreCompanyConfigRemoteDataSource
import com.brandcrafts.erp.data.repository.CompanyConfigRepositoryImpl
import com.brandcrafts.erp.data.datasource.purchaseorder.PurchaseOrderRemoteDataSource
import com.brandcrafts.erp.data.datasource.purchaseorder.FirestorePurchaseOrderRemoteDataSource
import com.brandcrafts.erp.data.repository.PurchaseOrderRepositoryImpl
import com.brandcrafts.erp.data.repository.PurchaseOrderPdfRepositoryImpl
import com.brandcrafts.erp.domain.repository.AuthenticationRepository
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.brandcrafts.erp.domain.repository.EmployeeRepository
import com.brandcrafts.erp.domain.repository.InventoryRepository
import com.brandcrafts.erp.domain.repository.StockRepository
import com.brandcrafts.erp.domain.repository.StockOutRepository
import com.brandcrafts.erp.domain.repository.MaterialUsageRepository
import com.brandcrafts.erp.domain.repository.InventoryTransactionRepository
import com.brandcrafts.erp.domain.repository.QuotationRepository
import com.brandcrafts.erp.domain.repository.CompanyConfigRepository
import com.brandcrafts.erp.domain.repository.PurchaseOrderRepository
import com.brandcrafts.erp.domain.repository.PurchaseOrderPdfRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthenticationDataSource(impl: FirebaseAuthenticationDataSourceImpl): FirebaseAuthenticationDataSource
    @Binds @Singleton abstract fun bindAuthenticationRepository(impl: AuthenticationRepositoryImpl): AuthenticationRepository
    @Binds @Singleton abstract fun bindInventoryRemoteDataSource(impl: FirestoreInventoryRemoteDataSource): InventoryRemoteDataSource
    @Binds @Singleton abstract fun bindInventoryRepository(impl: InventoryRepositoryImpl): InventoryRepository
    @Binds @Singleton abstract fun bindStockRemoteDataSource(impl: FirestoreStockRemoteDataSource): StockRemoteDataSource
    @Binds @Singleton abstract fun bindStockRepository(impl: StockRepositoryImpl): StockRepository
    @Binds @Singleton abstract fun bindStockOutRemoteDataSource(impl: FirestoreStockOutRemoteDataSource): StockOutRemoteDataSource
    @Binds @Singleton abstract fun bindStockOutRepository(impl: StockOutRepositoryImpl): StockOutRepository
    @Binds @Singleton abstract fun bindMaterialUsageRepository(impl: MaterialUsageRepositoryImpl): MaterialUsageRepository
    @Binds @Singleton abstract fun bindInventoryTransactionRepository(impl: InventoryTransactionRepositoryImpl): InventoryTransactionRepository
    @Binds @Singleton abstract fun bindContactsRemoteDataSource(impl: FirestoreContactsRemoteDataSource): ContactsRemoteDataSource
    @Binds @Singleton abstract fun bindContactRepository(impl: ContactRepositoryImpl): ContactRepository
    @Binds @Singleton abstract fun bindEmployeeRemoteDataSource(impl: FirestoreEmployeeRemoteDataSource): EmployeeRemoteDataSource
    @Binds @Singleton abstract fun bindEmployeeFunctionsDataSource(impl: FirebaseEmployeeFunctionsDataSource): EmployeeFunctionsDataSource
    @Binds @Singleton abstract fun bindEmployeeRepository(impl: EmployeeRepositoryImpl): EmployeeRepository
    @Binds @Singleton abstract fun bindQuotationRemoteDataSource(impl: FirestoreQuotationRemoteDataSource): QuotationRemoteDataSource
    @Binds @Singleton abstract fun bindQuotationRepository(impl: QuotationRepositoryImpl): QuotationRepository
    @Binds @Singleton abstract fun bindCompanyConfigRemoteDataSource(impl: FirestoreCompanyConfigRemoteDataSource): CompanyConfigRemoteDataSource
    @Binds @Singleton abstract fun bindCompanyConfigRepository(impl: CompanyConfigRepositoryImpl): CompanyConfigRepository
    @Binds @Singleton abstract fun bindPurchaseOrderRemoteDataSource(impl: FirestorePurchaseOrderRemoteDataSource): PurchaseOrderRemoteDataSource
    @Binds @Singleton abstract fun bindPurchaseOrderRepository(impl: PurchaseOrderRepositoryImpl): PurchaseOrderRepository
    @Binds @Singleton abstract fun bindPurchaseOrderPdfRepository(impl: PurchaseOrderPdfRepositoryImpl): PurchaseOrderPdfRepository
}
