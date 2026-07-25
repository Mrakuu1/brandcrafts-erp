package com.brandcrafts.erp.data.datasource.company
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
class FirestoreCompanyConfigRemoteDataSource @Inject constructor(private val firestore:FirebaseFirestore):CompanyConfigRemoteDataSource{override suspend fun getCompanyConfig():CompanyConfig{val d=firestore.collection("config").document("company").get().await();require(d.exists());fun required(key:String)=d.getString(key)?.takeIf(String::isNotBlank)?:throw IllegalStateException();
    return CompanyConfig(required("companyName"),d.getString("legalName").orEmpty(),required("addressLine1"),d.getString("addressLine2").orEmpty(),required("city"),required("state"),required("pincode"),required("country"),required("phone"),required("email"),d.getString("website").orEmpty(),d.getString("gstNumber").orEmpty(),d.getString("logoUrl").orEmpty(),d.getString("quotationTerms").orEmpty(),d.getString("authorizedSignatoryName").orEmpty(),d.getString("authorizedSignatoryDesignation").orEmpty(),d.getString("signatureImageUrl").orEmpty(),d.getTimestamp("updatedAt")?.toDate()?.time,d.getString("updatedBy").orEmpty())}}
private suspend fun <T> Task<T>.await():T=suspendCancellableCoroutine{c->addOnSuccessListener{c.resume(it)}.addOnFailureListener{c.resumeWithException(it)}}
