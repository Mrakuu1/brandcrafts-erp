package com.brandcrafts.erp.di

import com.brandcrafts.erp.data.repository.InvoiceActorValidator
import com.brandcrafts.erp.data.repository.InvoiceCreateIdGenerator
import com.brandcrafts.erp.data.repository.InvoiceCreatePreparer
import com.brandcrafts.erp.data.repository.InvoiceCustomerValidator
import com.brandcrafts.erp.data.repository.InvoiceDraftUpdatePreparer
import com.brandcrafts.erp.domain.usecase.invoice.InvoiceCalculator
import com.brandcrafts.erp.domain.usecase.invoice.InvoiceValidator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InvoiceModule {

    @Provides
    @Singleton
    fun provideInvoiceCalculator(): InvoiceCalculator = InvoiceCalculator()

    @Provides
    @Singleton
    fun provideInvoiceValidator(calculator: InvoiceCalculator): InvoiceValidator =
        InvoiceValidator(calculator)

    @Provides
    @Singleton
    fun provideInvoiceCreatePreparer(
        actorValidator: InvoiceActorValidator,
        customerValidator: InvoiceCustomerValidator,
        validator: InvoiceValidator,
        calculator: InvoiceCalculator,
        idGenerator: InvoiceCreateIdGenerator,
    ): InvoiceCreatePreparer = InvoiceCreatePreparer(
        actorValidator = actorValidator,
        customerValidator = customerValidator,
        validator = validator,
        calculator = calculator,
        idGenerator = idGenerator,
    )

    @Provides
    @Singleton
    fun provideInvoiceDraftUpdatePreparer(
        actorValidator: InvoiceActorValidator,
        customerValidator: InvoiceCustomerValidator,
        validator: InvoiceValidator,
        calculator: InvoiceCalculator,
        idGenerator: InvoiceCreateIdGenerator,
    ): InvoiceDraftUpdatePreparer = InvoiceDraftUpdatePreparer(
        actorValidator = actorValidator,
        customerValidator = customerValidator,
        validator = validator,
        calculator = calculator,
        ids = idGenerator,
    )
}
