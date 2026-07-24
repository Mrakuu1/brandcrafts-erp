package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.data.model.FirestoreStockIn
import com.brandcrafts.erp.domain.model.StockInInput
import com.brandcrafts.erp.domain.model.AuthenticatedUser

fun StockInInput.toFirestoreStockIn(user: AuthenticatedUser, unit: String) = FirestoreStockIn(
    materialId = materialId,
    quantity = quantity,
    unit = unit,
    referenceId = referenceId.trim(),
    remarks = remarks.trim(),
    performedBy = user.uid,
    performedByName = user.name,
)
