package com.brandcrafts.erp.data.repository
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.StockOutError
import com.brandcrafts.erp.core.result.StockOutResult
import com.brandcrafts.erp.domain.model.MaterialUsageInput
import com.brandcrafts.erp.domain.repository.MaterialUsageRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
class MaterialUsageRepositoryImpl @Inject constructor(private val firestore:FirebaseFirestore,private val session:SessionManager):MaterialUsageRepository{
 override suspend fun recordUsage(input:MaterialUsageInput):StockOutResult<Unit>{if(input.materialId.isBlank()||input.quantity<=0||input.jobReference.isBlank())return StockOutResult.Error(StockOutError.VALIDATION_FAILED);val user=(session.currentUser.value as? CurrentUserState.Authenticated)?.user?.takeIf{it.active}?:return StockOutResult.Error(StockOutError.UNAUTHORIZED);return try{val m=firestore.collection("materials").document(input.materialId);val t=firestore.collection("stock_transactions").document();val a=firestore.collection("activity_logs").document();firestore.runTransaction{x->val s=x.get(m);if(!s.exists())throw IllegalArgumentException("missing");if(s.getBoolean("active")!=true)throw IllegalStateException("inactive");val q=s.getDouble("availableQuantity")?:0.0;if(q<input.quantity)throw IllegalStateException("stock");x.update(m,mapOf("availableQuantity" to q-input.quantity,"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to user.uid));x.set(t,mapOf("id" to t.id,"materialId" to input.materialId,"transactionType" to "MATERIAL_USAGE","quantity" to input.quantity,"unit" to(s.getString("unit")?:""),"referenceId" to input.jobReference.trim(),"referenceType" to "JOB","supplierId" to "","remarks" to input.remarks.trim(),"performedBy" to user.uid,"createdAt" to FieldValue.serverTimestamp()));x.set(a,mapOf("id" to a.id,"module" to "INVENTORY","action" to "MATERIAL_USAGE","referenceId" to t.id,"referenceType" to "STOCK_TRANSACTION","description" to "Material usage recorded","performedBy" to user.uid,"performedByName" to user.name,"createdAt" to FieldValue.serverTimestamp()))}.await();StockOutResult.Success(Unit)}catch(e:Throwable){StockOutResult.Error(if(e.message=="stock")StockOutError.INSUFFICIENT_STOCK else StockOutError.UNKNOWN)}}
}
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await():T=kotlinx.coroutines.suspendCancellableCoroutine{c->addOnSuccessListener{c.resume(it)}.addOnFailureListener{c.resumeWithException(it)}}
