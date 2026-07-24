package com.brandcrafts.erp.data.repository
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.domain.model.InventoryTransaction
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.InventoryTransactionRepository
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
class InventoryTransactionRepositoryImpl @Inject constructor(private val db:FirebaseFirestore,private val session:SessionManager):InventoryTransactionRepository{override fun observeRecent(materialId:String)=callbackFlow<Result<List<InventoryTransaction>>>{val user=(session.currentUser.value as? CurrentUserState.Authenticated)?.user;if(user==null||!user.active){trySend(Result.failure(SecurityException()));close();return@callbackFlow};var q=db.collection("stock_transactions").whereEqualTo("materialId",materialId);if(user.role==UserRole.EMPLOYEE)q=q.whereEqualTo("performedBy",user.uid);val l=q.orderBy("createdAt",com.google.firebase.firestore.Query.Direction.DESCENDING).limit(20).addSnapshotListener{v,e->if(e!=null)trySend(Result.failure(e))else trySend(Result.success(v!!.documents.map{d->InventoryTransaction(d.id,d.getString("materialId")?:"",InventoryTransaction.Type.valueOf(d.getString("transactionType")?:"STOCK_IN"),d.getDouble("quantity")?:0.0,d.getString("unit")?:"",d.getString("referenceId")?:"",d.getString("referenceType")?:"",d.getString("remarks")?:"",d.getString("performedBy")?:"",d.getTimestamp("createdAt")?.toDate()?.time)}))};awaitClose(l::remove)}}
