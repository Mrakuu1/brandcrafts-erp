package com.brandcrafts.erp.domain.usecase.deliverychallan

import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.domain.model.DeliveryChallan
import com.brandcrafts.erp.domain.model.DeliveryChallanLine
import com.brandcrafts.erp.domain.model.DeliveryChallanSourceType
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import java.math.BigDecimal

class DeliveryChallanValidator {
    fun validateDraft(
        customerId: String,
        deliveryAddress: String,
        dateMillis: Long,
        sourceType: DeliveryChallanSourceType,
        sourceInvoiceId: String?,
        sourceInvoiceNumber: String?,
        lines: List<DeliveryChallanLine>,
    ): Result<Unit> = validationResult {
        if (customerId.isBlank()) fail(DeliveryChallanError.CustomerRequired)
        if (deliveryAddress.isBlank()) fail(DeliveryChallanError.DeliveryAddressRequired)
        if (dateMillis <= 0L) fail(DeliveryChallanError.DeliveryDateRequired)
        validateSource(sourceType, sourceInvoiceId, sourceInvoiceNumber)
        validateLines(lines)
    }

    fun validateDraftEdit(challan: DeliveryChallan): Result<Unit> = validationResult {
        if (challan.status != DeliveryChallanStatus.DRAFT) fail(DeliveryChallanError.DraftOnlyUpdateRequired)
    }

    fun validateTransition(
        current: DeliveryChallanStatus,
        target: DeliveryChallanStatus,
    ): Result<Unit> = validationResult {
        val allowed = current == DeliveryChallanStatus.DRAFT &&
            (target == DeliveryChallanStatus.DISPATCHED || target == DeliveryChallanStatus.CANCELLED)
        if (!allowed) fail(DeliveryChallanError.InvalidStatusTransition)
    }

    fun validateDispatch(challan: DeliveryChallan): Result<Unit> = validationResult {
        if (challan.status != DeliveryChallanStatus.DRAFT) fail(DeliveryChallanError.DispatchNotEligible)
        validateDraft(
            challan.customerId,
            challan.deliveryAddress,
            challan.dateMillis,
            challan.sourceType,
            challan.sourceInvoiceId,
            challan.sourceInvoiceNumber,
            challan.lines,
        ).getOrElse { throw it }
    }

    fun validateDraftCancellation(challan: DeliveryChallan): Result<Unit> = validationResult {
        if (challan.status != DeliveryChallanStatus.DRAFT) fail(DeliveryChallanError.DraftCancellationRequired)
    }

    fun validateInvoiceQuantities(
        lines: List<DeliveryChallanLine>,
        availableQuantityByMaterialId: Map<String, BigDecimal>,
    ): Result<Unit> = validationResult {
        validateLines(lines)
        lines.groupBy { it.materialId }.forEach { (materialId, materialLines) ->
            val permitted = availableQuantityByMaterialId[materialId]
                ?: fail(DeliveryChallanError.InvoiceQuantityExceeded)
            val requested = materialLines.fold(BigDecimal.ZERO) { total, line -> total + line.quantity }
            if (requested > permitted) fail(DeliveryChallanError.InvoiceQuantityExceeded)
        }
    }

    private fun validateSource(
        sourceType: DeliveryChallanSourceType,
        sourceInvoiceId: String?,
        sourceInvoiceNumber: String?,
    ) {
        if (sourceType == DeliveryChallanSourceType.INVOICE &&
            (sourceInvoiceId.isNullOrBlank() || sourceInvoiceNumber.isNullOrBlank())
        ) fail(DeliveryChallanError.InvalidInvoiceSource)
        if (sourceType == DeliveryChallanSourceType.INDEPENDENT &&
            (!sourceInvoiceId.isNullOrBlank() || !sourceInvoiceNumber.isNullOrBlank())
        ) fail(DeliveryChallanError.InvalidInvoiceSource)
    }

    private fun validateLines(lines: List<DeliveryChallanLine>) {
        if (lines.isEmpty()) fail(DeliveryChallanError.EmptyItemList)
        if (lines.map { it.id }.any(String::isBlank) || lines.map { it.id }.toSet().size != lines.size) {
            fail(DeliveryChallanError.InvalidLineId)
        }
        lines.forEachIndexed { index, line ->
            if (line.sortOrder != index || (line.materialId.isBlank() && line.description.isBlank())) {
                fail(DeliveryChallanError.InvalidLine)
            }
            if (line.quantity <= BigDecimal.ZERO) fail(DeliveryChallanError.InvalidQuantity)
            if (line.unit.isBlank()) fail(DeliveryChallanError.InvalidUnit)
        }
    }

    private fun fail(error: DeliveryChallanError): Nothing = throw DeliveryChallanFailure(error)

    private inline fun validationResult(block: () -> Unit): Result<Unit> = try {
        block()
        Result.success(Unit)
    } catch (failure: DeliveryChallanFailure) {
        Result.failure(failure)
    }
}
