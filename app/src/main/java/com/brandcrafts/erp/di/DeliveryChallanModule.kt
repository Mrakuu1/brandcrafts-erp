package com.brandcrafts.erp.di

import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.data.datasource.deliverychallan.*
import com.brandcrafts.erp.data.repository.DeliveryChallanRepositoryImpl
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.brandcrafts.erp.domain.repository.DeliveryChallanRepository
import com.brandcrafts.erp.domain.usecase.deliverychallan.DeliveryChallanValidator
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
abstract class DeliveryChallanBindingModule {
    @Binds @Singleton abstract fun bindRepository(impl: DeliveryChallanRepositoryImpl): DeliveryChallanRepository
}

@Module @InstallIn(SingletonComponent::class)
object DeliveryChallanModule {
    @Provides @Singleton fun validator() = DeliveryChallanValidator()
    @Provides @Singleton fun reads(db: FirebaseFirestore): DeliveryChallanRemoteDataSource = FirestoreDeliveryChallanRemoteDataSource(db)
    @Provides @Singleton fun independent(db: FirebaseFirestore, contacts: ContactRepository, session: SessionManager, validator: DeliveryChallanValidator): DeliveryChallanCreateRemoteDataSource = FirestoreDeliveryChallanCreateRemoteDataSource(db, contacts, session, validator)
    @Provides @Singleton fun invoice(db: FirebaseFirestore, session: SessionManager): DeliveryChallanInvoiceCreateRemoteDataSource = FirestoreDeliveryChallanInvoiceCreateRemoteDataSource(db, session)
    @Provides @Singleton fun update(db: FirebaseFirestore, session: SessionManager): DeliveryChallanDraftUpdateRemoteDataSource = FirestoreDeliveryChallanDraftUpdateRemoteDataSource(db, session)
    @Provides @Singleton fun dispatch(db: FirebaseFirestore, session: SessionManager): DeliveryChallanDispatchRemoteDataSource = FirestoreDeliveryChallanDispatchRemoteDataSource(db, session)
    @Provides @Singleton fun cancel(db: FirebaseFirestore, session: SessionManager): DeliveryChallanCancellationRemoteDataSource = FirestoreDeliveryChallanCancellationRemoteDataSource(db, session)
}
