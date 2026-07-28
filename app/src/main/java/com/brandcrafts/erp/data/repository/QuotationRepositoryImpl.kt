package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.data.datasource.quotation.QuotationRemoteDataSource
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationDraft
import com.brandcrafts.erp.domain.model.QuotationLineItem
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.QuotationRepository
import com.brandcrafts.erp.domain.usecase.quotation.QuotationCalculationLine
import com.brandcrafts.erp.domain.usecase.quotation.QuotationCalculator
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class QuotationRepositoryImpl @Inject constructor(
    private val source: QuotationRemoteDataSource,
    private val firestore: FirebaseFirestore,
    private val session: SessionManager,
) : QuotationRepository {
    private val calculator = QuotationCalculator()
    override fun observeQuotations() = source.observeQuotations().map { Result.success(it) }.catch { emit(Result.failure(it)) }
    override suspend fun getQuotation(id: String): Result<Quotation> = runCatching {
        val header=firestore.collection("documents").document(id).get().await()
        check(header.exists() && header.getString("type")=="QUOTATION")
        val lines=header.reference.collection("items").orderBy("sortOrder").get().await().documents.map { item ->
            QuotationLineItem(item.id,item.getString("materialId")?:throw IllegalArgumentException(),item.getString("description")?:throw IllegalArgumentException(),decimal(item,"quantity"),item.getString("unit")?:throw IllegalArgumentException(),decimal(item,"unitPrice"),decimal(item,"discountPercent"),decimal(item,"taxPercent"),decimal(item,"lineTotal"))
        };require(lines.isNotEmpty())
        Quotation(header.id,header.getString("documentNumber")?:throw IllegalArgumentException(),header.getString("contactId")?:throw IllegalArgumentException(),businessDate(header, "date", required = true),businessDate(header, "validUntil", required = false),QuotationStatus.valueOf(header.getString("status")?:throw IllegalArgumentException()),decimal(header,"grandTotal"),header.getString("pdfUrl")?:"",header.getString("createdBy")?:"",header.getString("remarks")?:"",lines)
    }
    override suspend fun createQuotation(draft: QuotationDraft): Result<String> = writeCreate(draft)
    override suspend fun updateQuotation(id: String, draft: QuotationDraft): Result<Unit> = runCatching {
        val user = admin() ?: throw SecurityException()
        validate(draft)
        val totals = calculator.totals(draft.lines.map { QuotationCalculationLine(it.quantity,it.unitPrice,it.discountPercent,it.taxPercent) })
        val ref = firestore.collection("documents").document(id)
        val existingItemIds = ref.collection("items").get().await().documents.map { it.id }.toSet()
        val retainedItemIds = draft.lines.mapNotNull { it.id }.toSet()
        require(retainedItemIds.size == draft.lines.count { it.id != null })
        require(retainedItemIds.all(existingItemIds::contains))
        firestore.runTransaction { tx ->
            val old = tx.get(ref)
            check(old.exists() && old.getString("type") == "QUOTATION")
            check(old.getString("status") == "DRAFT")
            tx.update(ref, mapOf("contactId" to draft.contactId,"validUntil" to draft.validUntilMillis,"subtotal" to totals.subtotal.plain(),"discountTotal" to totals.discount.plain(),"taxableTotal" to totals.taxable.plain(),"taxTotal" to totals.tax.plain(),"grandTotal" to totals.grandTotal.plain(),"remarks" to draft.remarks,"updatedBy" to user.uid,"updatedAt" to FieldValue.serverTimestamp()))
            val items=ref.collection("items")
            existingItemIds.filterNot(retainedItemIds::contains).forEach { itemId -> tx.delete(items.document(itemId)) }
            draft.lines.forEachIndexed { index,line -> val item=if(line.id==null)items.document()else items.document(line.id);val c=calculator.line(QuotationCalculationLine(line.quantity,line.unitPrice,line.discountPercent,line.taxPercent));tx.set(item,mapOf("itemId" to item.id,"materialId" to line.materialId,"description" to line.description,"quantity" to line.quantity.toPlainString(),"unit" to line.unit,"unitPrice" to line.unitPrice.toPlainString(),"discountPercent" to line.discountPercent.toPlainString(),"taxPercent" to line.taxPercent.toPlainString(),"lineSubtotal" to c.subtotal.plain(),"lineDiscount" to c.discount.plain(),"taxableAmount" to c.taxable.plain(),"lineTax" to c.tax.plain(),"lineTotal" to c.total.plain(),"sortOrder" to index)) }
        }.await()
    }
    override suspend fun updateQuotationStatus(id: String, status: QuotationStatus): Result<Unit> = runCatching {
        require(status == QuotationStatus.APPROVED || status == QuotationStatus.REJECTED)
        val user = admin() ?: throw SecurityException()
        val reference = firestore.collection("documents").document(id)
        firestore.runTransaction { transaction ->
            val current = transaction.get(reference)
            check(current.exists() && current.getString("type") == "QUOTATION")
            check(current.getString("status") == QuotationStatus.DRAFT.name)
            transaction.update(
                reference,
                mapOf(
                    "status" to status.name,
                    "updatedBy" to user.uid,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }.await()
    }
    private suspend fun writeCreate(draft:QuotationDraft):Result<String> = runCatching {
        val user=admin()?:throw SecurityException();validate(draft)
        val totals=calculator.totals(draft.lines.map{QuotationCalculationLine(it.quantity,it.unitPrice,it.discountPercent,it.taxPercent)})
        val document=firestore.collection("documents").document()
        firestore.runTransaction { tx ->
            val counter=firestore.collection("counters").document("quotation");val state=tx.get(counter)
            val next=if(state.exists()) state.getLong("nextNumber")?:throw IllegalArgumentException() else 1L
            require(next in 1..999999); val number="QT-"+next.toString().padStart(6,'0')
            tx.set(counter,mapOf("nextNumber" to next+1,"prefix" to "QT"))
            tx.set(document,mapOf("id" to document.id,"documentNumber" to number,"type" to "QUOTATION","contactId" to draft.contactId,"date" to FieldValue.serverTimestamp(),"validUntil" to draft.validUntilMillis,"status" to "DRAFT","subtotal" to totals.subtotal.plain(),"discountTotal" to totals.discount.plain(),"taxableTotal" to totals.taxable.plain(),"taxTotal" to totals.tax.plain(),"grandTotal" to totals.grandTotal.plain(),"remarks" to draft.remarks,"pdfUrl" to "","createdBy" to user.uid,"updatedBy" to user.uid,"createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp()))
            draft.lines.forEachIndexed { index,line -> val item=document.collection("items").document();val c=calculator.line(QuotationCalculationLine(line.quantity,line.unitPrice,line.discountPercent,line.taxPercent));tx.set(item,mapOf("itemId" to item.id,"materialId" to line.materialId,"description" to line.description,"quantity" to line.quantity.toPlainString(),"unit" to line.unit,"unitPrice" to line.unitPrice.toPlainString(),"discountPercent" to line.discountPercent.toPlainString(),"taxPercent" to line.taxPercent.toPlainString(),"lineSubtotal" to c.subtotal.plain(),"lineDiscount" to c.discount.plain(),"taxableAmount" to c.taxable.plain(),"lineTax" to c.tax.plain(),"lineTotal" to c.total.plain(),"sortOrder" to index)) }
        }.await();document.id
    }
    private fun admin()=(session.currentUser.value as? CurrentUserState.Authenticated)?.user?.takeIf{it.active&&it.role==UserRole.ADMIN}
    private fun validate(d:QuotationDraft){require(d.contactId.isNotBlank()&&d.lines.isNotEmpty());d.lines.forEach{require(it.materialId.isNotBlank());calculator.line(QuotationCalculationLine(it.quantity,it.unitPrice,it.discountPercent,it.taxPercent))}}
    private fun BigDecimal.plain()=setScale(2,RoundingMode.HALF_UP).toPlainString()
    private fun decimal(document:com.google.firebase.firestore.DocumentSnapshot,field:String)=BigDecimal(document.getString(field)?:throw IllegalArgumentException())
    private fun businessDate(document: com.google.firebase.firestore.DocumentSnapshot, field: String, required: Boolean): Long? = when (val value = document.get(field)) {
        null -> if (required) throw IllegalArgumentException("Missing quotation $field") else null
        is com.google.firebase.Timestamp -> value.toDate().time
        is Number -> value.toLong().takeIf { it > 0 }
        is String -> value.toLongOrNull()?.takeIf { it > 0 }
        else -> null
    } ?: if (required) throw IllegalArgumentException("Invalid quotation $field") else null
}
private suspend fun <T> Task<T>.await():T=suspendCancellableCoroutine{c->addOnSuccessListener{c.resume(it)}.addOnFailureListener{c.resumeWithException(it)}}
