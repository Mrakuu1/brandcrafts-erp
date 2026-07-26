package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.PurchaseOrderError
import com.brandcrafts.erp.core.result.PurchaseOrderFailure
import com.brandcrafts.erp.data.mapper.purchaseOrderDateTimestamp
import com.brandcrafts.erp.data.mapper.purchaseOrderOptionalDateTimestamp
import com.brandcrafts.erp.data.mapper.toPurchaseOrderDateMillis
import com.brandcrafts.erp.data.datasource.purchaseorder.PurchaseOrderRemoteDataSource
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.PurchaseOrder
import com.brandcrafts.erp.domain.model.PurchaseOrderDraft
import com.brandcrafts.erp.domain.model.PurchaseOrderLine
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.PurchaseOrderRepository
import com.brandcrafts.erp.domain.usecase.purchaseorder.PurchaseOrderCalculationLine
import com.brandcrafts.erp.domain.usecase.purchaseorder.PurchaseOrderCalculator
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PurchaseOrderRepositoryImpl @Inject constructor(
    private val source: PurchaseOrderRemoteDataSource,
    private val firestore: FirebaseFirestore,
    private val session: SessionManager,
    private val calculator: PurchaseOrderCalculator,
) : PurchaseOrderRepository {
    override fun observePurchaseOrders() = admin()?.let { source.observePurchaseOrders().map { Result.success(it) }.catch { emit(Result.failure(it)) } }
        ?: flowOf(Result.failure(SecurityException("Unauthorized")))

    override suspend fun getPurchaseOrder(id: String): Result<PurchaseOrder> = poResult(PurchaseOrderError.PurchaseOrderNotFound) {
        require(id.isNotBlank()); admin() ?: throw SecurityException("Unauthorized")
        val header = firestore.collection(DOCUMENTS).document(id).get().await()
        if (!header.exists()) throw PurchaseOrderFailure(PurchaseOrderError.PurchaseOrderNotFound)
        if (header.getString("type") != TYPE) throw PurchaseOrderFailure(PurchaseOrderError.WrongDocumentType)
        val lines = header.reference.collection(ITEMS).orderBy("sortOrder").get().await().documents.map { d ->
            PurchaseOrderLine(d.id, required(d,"materialId"), required(d,"description"), decimal(d,"quantity"), required(d,"unit"), decimal(d,"unitPrice"), decimal(d,"lineTotal"), d.getLong("sortOrder")?.toInt() ?: throw IllegalArgumentException())
        }
        val status = PurchaseOrderStatus.entries.firstOrNull { it.name == required(header,"status") } ?: throw PurchaseOrderFailure(PurchaseOrderError.InvalidStoredStatus)
        PurchaseOrder(header.id,required(header,"documentNumber"),required(header,"supplierId"),header.get("date").toPurchaseOrderDateMillis(true),header.get("expectedDeliveryDate").toPurchaseOrderDateMillis(false),header.getString("supplierReferenceNumber").orEmpty(),header.getString("remarks").orEmpty(),status,decimal(header,"total"),header.getTimestamp("createdAt")?.toDate()?.time,header.getTimestamp("updatedAt")?.toDate()?.time,required(header,"createdBy"),required(header,"updatedBy"),header.getTimestamp("approvedAt")?.toDate()?.time,header.getString("approvedBy").orEmpty(),header.getTimestamp("cancelledAt")?.toDate()?.time,header.getString("cancelledBy").orEmpty(),lines)
    }

    override suspend fun createPurchaseOrder(draft: PurchaseOrderDraft): Result<String> = poResult(PurchaseOrderError.PurchaseOrderWriteFailed) {
        val user = admin() ?: throw SecurityException("Unauthorized"); validate(draft); val createWrites=PurchaseOrderWritePolicy.createWriteCount(draft.lines.size); if(!PurchaseOrderWritePolicy.isAllowed(createWrites))throw PurchaseOrderFailure(PurchaseOrderError.FirestoreOperationLimitExceeded(createWrites,PurchaseOrderWritePolicy.MAX_SAFE_WRITES)); validateSupplier(draft.supplierId)
        val total = total(draft); val order = firestore.collection(DOCUMENTS).document(); val activity = firestore.collection(ACTIVITY).document()
        firestore.runTransaction { tx ->
            val counter = firestore.collection(COUNTERS).document(COUNTER); val snapshot = tx.get(counter); val next = if (snapshot.exists()) snapshot.getLong("nextNumber") ?: throw IllegalArgumentException() else 1L
            require(next in 1..999999); val number = "PO-" + next.toString().padStart(6,'0')
            tx.set(counter, mapOf("nextNumber" to next + 1, "prefix" to "PO"))
            tx.set(order, parent(order.id, number, draft, total, user.uid))
            draft.lines.forEachIndexed { index, line -> val item = order.collection(ITEMS).document(); tx.set(item, lineMap(line, item.id, index)) }
            tx.set(activity, activity(activity.id, order.id, "CREATE", "PURCHASE_ORDER_CREATED", user.uid, user.name))
        }.await(); order.id
    }

    override suspend fun updatePurchaseOrder(id: String, draft: PurchaseOrderDraft): Result<Unit> = poResult(PurchaseOrderError.PurchaseOrderWriteFailed) {
        val user=admin() ?: throw SecurityException("Unauthorized"); validate(draft); validateSupplier(draft.supplierId); val ref=firestore.collection(DOCUMENTS).document(id); val existing=ref.collection(ITEMS).get().await().documents.map { it.id }.toSet(); val retained=draft.lines.mapNotNull { it.id }.toSet(); require(retained.size==draft.lines.count{it.id!=null} && retained.all(existing::contains)); val updateWrites=PurchaseOrderWritePolicy.updateWriteCount(draft.lines.size,existing.count{it !in retained});if(!PurchaseOrderWritePolicy.isAllowed(updateWrites))throw PurchaseOrderFailure(PurchaseOrderError.FirestoreOperationLimitExceeded(updateWrites,PurchaseOrderWritePolicy.MAX_SAFE_WRITES)); val activity=firestore.collection(ACTIVITY).document()
        firestore.runTransaction { tx -> val old=tx.get(ref); check(old.exists()&&old.getString("type")==TYPE&&old.getString("status")=="DRAFT"); tx.update(ref,mapOf("supplierId" to draft.supplierId,"date" to purchaseOrderDateTimestamp(draft.dateMillis),"expectedDeliveryDate" to purchaseOrderOptionalDateTimestamp(draft.expectedDeliveryDateMillis),"supplierReferenceNumber" to draft.supplierReferenceNumber.trim(),"remarks" to draft.remarks.trim(),"total" to total(draft).toPlainString(),"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to user.uid)); existing.filterNot(retained::contains).forEach{tx.delete(ref.collection(ITEMS).document(it))}; draft.lines.forEachIndexed{index,line->val item=ref.collection(ITEMS).document(line.id?:ref.collection(ITEMS).document().id);tx.set(item,lineMap(line,item.id,index))}; tx.set(activity,activity(activity.id,id,"UPDATE","PURCHASE_ORDER_UPDATED",user.uid,user.name)) }.await()
    }
    override suspend fun approvePurchaseOrder(id:String):Result<Unit> = transition(id, PurchaseOrderStatus.DRAFT, PurchaseOrderStatus.APPROVED, "APPROVE", "PURCHASE_ORDER_APPROVED")
    override suspend fun cancelPurchaseOrder(id:String):Result<Unit> = poResult(PurchaseOrderError.InvalidStatusTransition) { val current=getPurchaseOrder(id).getOrThrow(); if(current.status==PurchaseOrderStatus.APPROVED) throw PurchaseOrderFailure(PurchaseOrderError.StockReferenceValidationUnavailable); if(current.status==PurchaseOrderStatus.CANCELLED) throw PurchaseOrderFailure(PurchaseOrderError.InvalidStatusTransition); transition(id,PurchaseOrderStatus.DRAFT,PurchaseOrderStatus.CANCELLED,"CANCEL","PURCHASE_ORDER_CANCELLED").getOrThrow() }
    private suspend fun transition(id:String, from:PurchaseOrderStatus,to:PurchaseOrderStatus,action:String,description:String):Result<Unit> = poResult(PurchaseOrderError.TransactionAborted) { val user=admin()?:throw PurchaseOrderFailure(PurchaseOrderError.AdminAccessRequired); val ref=firestore.collection(DOCUMENTS).document(id); val a=firestore.collection(ACTIVITY).document(); firestore.runTransaction{tx->val old=tx.get(ref);if(!old.exists())throw PurchaseOrderFailure(PurchaseOrderError.PurchaseOrderNotFound);if(old.getString("type")!=TYPE)throw PurchaseOrderFailure(PurchaseOrderError.WrongDocumentType);if(old.getString("status")!=from.name)throw PurchaseOrderFailure(PurchaseOrderError.InvalidStatusTransition);val audit=if(to==PurchaseOrderStatus.APPROVED)mapOf("approvedAt" to FieldValue.serverTimestamp(),"approvedBy" to user.uid)else mapOf("cancelledAt" to FieldValue.serverTimestamp(),"cancelledBy" to user.uid);tx.update(ref,audit+mapOf("status" to to.name,"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to user.uid));tx.set(a,activity(a.id,id,action,description,user.uid,user.name))}.await() }
    private suspend fun validateSupplier(id:String){val d=firestore.collection(CONTACTS).document(id).get().await();check(d.exists()&&d.getString("type")==ContactType.SUPPLIER.name&&d.getBoolean("active")==true)}
    private fun validate(d:PurchaseOrderDraft){require(d.supplierId.isNotBlank()&&d.dateMillis>0&&d.lines.isNotEmpty());d.lines.forEach{require(it.materialId.isNotBlank()&&it.description.isNotBlank()&&it.unit.isNotBlank());calculator.lineTotal(it.quantity,it.unitPrice)}}
    private fun total(d:PurchaseOrderDraft)=calculator.total(d.lines.map{PurchaseOrderCalculationLine(it.quantity,it.unitPrice)})
    private fun parent(id:String,n:String,d:PurchaseOrderDraft,t:BigDecimal,u:String)=mapOf("id" to id,"documentNumber" to n,"type" to TYPE,"supplierId" to d.supplierId,"date" to purchaseOrderDateTimestamp(d.dateMillis),"expectedDeliveryDate" to purchaseOrderOptionalDateTimestamp(d.expectedDeliveryDateMillis),"supplierReferenceNumber" to d.supplierReferenceNumber.trim(),"remarks" to d.remarks.trim(),"status" to "DRAFT","total" to t.toPlainString(),"createdAt" to FieldValue.serverTimestamp(),"updatedAt" to FieldValue.serverTimestamp(),"createdBy" to u,"updatedBy" to u)
    private fun lineMap(l:com.brandcrafts.erp.domain.model.PurchaseOrderDraftLine,itemId:String,i:Int)=mapOf("itemId" to itemId,"materialId" to l.materialId,"description" to l.description.trim(),"quantity" to l.quantity.toPlainString(),"unit" to l.unit,"unitPrice" to l.unitPrice.toPlainString(),"lineTotal" to calculator.lineTotal(l.quantity,l.unitPrice).toPlainString(),"sortOrder" to i)
    private fun activity(id:String,ref:String,action:String,description:String,uid:String,name:String)=mapOf("id" to id,"module" to "PURCHASE_ORDERS","action" to action,"referenceId" to ref,"referenceType" to "PURCHASE_ORDER","description" to description,"performedBy" to uid,"performedByName" to name,"createdAt" to FieldValue.serverTimestamp())
    private suspend fun <T> poResult(default:PurchaseOrderError, block:suspend()->T):Result<T> = try { Result.success(block()) } catch (e:CancellationException) { throw e } catch(e:PurchaseOrderFailure){Result.failure(e)} catch(e:FirebaseFirestoreException){Result.failure(PurchaseOrderFailure(when(e.code){FirebaseFirestoreException.Code.PERMISSION_DENIED->PurchaseOrderError.PermissionDenied;FirebaseFirestoreException.Code.UNAVAILABLE->PurchaseOrderError.FirestoreUnavailable;FirebaseFirestoreException.Code.ABORTED->PurchaseOrderError.TransactionAborted;else->default}))} catch(e:Throwable){Result.failure(PurchaseOrderFailure(default))}
    private fun admin()=(session.currentUser.value as? CurrentUserState.Authenticated)?.user?.takeIf{it.active&&it.role==UserRole.ADMIN}; private fun required(d:com.google.firebase.firestore.DocumentSnapshot,f:String)=d.getString(f)?:throw PurchaseOrderFailure(PurchaseOrderError.PurchaseOrderNotFound);private fun decimal(d:com.google.firebase.firestore.DocumentSnapshot,f:String)=try{BigDecimal(required(d,f))}catch(e:PurchaseOrderFailure){throw e}catch(e:Throwable){throw PurchaseOrderFailure(PurchaseOrderError.MalformedStoredDecimal)};private companion object{const val DOCUMENTS="documents";const val ITEMS="items";const val CONTACTS="contacts";const val COUNTERS="counters";const val COUNTER="purchaseOrder";const val ACTIVITY="activity_logs";const val TYPE="PURCHASE_ORDER"}
}
private suspend fun <T> Task<T>.await():T=suspendCancellableCoroutine{c->addOnSuccessListener{c.resume(it)}.addOnFailureListener{c.resumeWithException(it)}}
