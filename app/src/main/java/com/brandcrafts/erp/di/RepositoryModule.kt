package com.brandcrafts.erp.di

import com.brandcrafts.erp.data.datasource.auth.FirebaseAuthenticationDataSource
import com.brandcrafts.erp.data.datasource.auth.FirebaseAuthenticationDataSourceImpl
import com.brandcrafts.erp.data.repository.AuthenticationRepositoryImpl
import com.brandcrafts.erp.domain.repository.AuthenticationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuthenticationDataSource(impl: FirebaseAuthenticationDataSourceImpl): FirebaseAuthenticationDataSource
    @Binds @Singleton abstract fun bindAuthenticationRepository(impl: AuthenticationRepositoryImpl): AuthenticationRepository
}
