package com.brandcrafts.erp.data.datasource.quotation
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
class FirestoreQuotationRemoteDataSource @Inject constructor(private val firestore:FirebaseFirestore):QuotationRemoteDataSource{
 override fun observeQuotations()=callbackFlow{
  val listener=firestore.collection("documents").whereEqualTo("type","QUOTATION").orderBy("date",Query.Direction.DESCENDING).addSnapshotListener{snapshot,error->
   if(error!=null) close(error) else if(snapshot!=null) runCatching{snapshot.documents.map{d->Quotation(d.id,d.getString("documentNumber")?:"",d.getString("contactId")?:"",d.getTimestamp("date")?.toDate()?.time,d.getTimestamp("validUntil")?.toDate()?.time,QuotationStatus.entries.firstOrNull{it.name==d.getString("status")}?:throw IllegalArgumentException("Invalid quotation status"),BigDecimal(d.getString("grandTotal")?:"0"),d.getString("pdfUrl")?:"",d.getString("createdBy")?:"",d.getString("remarks")?:"")}}.onSuccess{trySend(it)}.onFailure{close(it)}
  };awaitClose(listener::remove)
 }
}
