package com.brandcrafts.erp.di

import com.brandcrafts.erp.data.datasource.auth.FirebaseAuthenticationDataSource
import com.brandcrafts.erp.data.datasource.auth.FirebaseAuthenticationDataSourceImpl
import com.brandcrafts.erp.data.datasource.inventory.FirestoreInventoryRemoteDataSource
import com.brandcrafts.erp.data.datasource.inventory.InventoryRemoteDataSource
import com.brandcrafts.erp.data.datasource.stock.FirestoreStockRemoteDataSource
import com.brandcrafts.erp.data.datasource.stock.StockRemoteDataSource
import com.brandcrafts.erp.data.repository.AuthenticationRepositoryImpl
import com.brandcrafts.erp.data.repository.InventoryRepositoryImpl
import com.brandcrafts.erp.data.repository.StockRepositoryImpl
import com.brandcrafts.erp.domain.repository.AuthenticationRepository
import com.brandcrafts.erp.domain.repository.InventoryRepository
import com.brandcrafts.erp.domain.repository.StockRepository
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
}
