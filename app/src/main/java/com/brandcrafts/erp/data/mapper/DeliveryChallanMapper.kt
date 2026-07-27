package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.model.deliverychallan.DeliveryChallanDto
import com.brandcrafts.erp.data.model.deliverychallan.DeliveryChallanLineDto
import com.brandcrafts.erp.domain.model.DeliveryChallan
import com.brandcrafts.erp.domain.model.DeliveryChallanLine
import com.brandcrafts.erp.domain.model.DeliveryChallanSourceType
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.domain.model.DeliveryChallanSummary
import com.google.firebase.Timestamp
import java.math.BigDecimal
import java.util.Date

fun DeliveryChallan.toDto() = DeliveryChallanDto(id, number, customerId, deliveryAddress, dateMillis, sourceType.name, sourceInvoiceId, sourceInvoiceNumber, vehicleNumber, driverName, notes, status.name, createdAtMillis, updatedAtMillis, createdBy, updatedBy, dispatchedAtMillis, dispatchedBy, cancelledAtMillis, cancelledBy)
fun DeliveryChallanLine.toDto() = DeliveryChallanLineDto(id, materialId, description, quantity.toPlainString(), unit, sortOrder)
fun DeliveryChallanDto.toDomain(lines: List<DeliveryChallanLine>) = DeliveryChallan(required(id), required(dcNumber), required(customerId), required(deliveryAddress), date(date, true)!!, sourceType(sourceType), sourceInvoiceId, sourceInvoiceNumber, vehicleNumber.orEmpty(), driverName.orEmpty(), notes.orEmpty(), status(status), date(createdAt, false), date(updatedAt, false), required(createdBy), required(updatedBy), date(dispatchedAt, false), dispatchedBy, date(cancelledAt, false), cancelledBy, lines)
fun DeliveryChallanLineDto.toDomain(documentId: String) = DeliveryChallanLine(required(itemId ?: documentId), materialId.orEmpty(), description.orEmpty(), quantity(quantity), required(unit), sortOrder ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine))
fun DeliveryChallanDto.toSummaryDomain() = DeliveryChallanSummary(required(id), required(dcNumber), required(customerId), date(date, true)!!, sourceType(sourceType), sourceInvoiceNumber, status(status))

private fun required(value: String?): String = value?.takeIf(String::isNotBlank) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)
private fun quantity(value: String?): BigDecimal = try { BigDecimal(value ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidQuantity)).also { if (it <= BigDecimal.ZERO) throw DeliveryChallanFailure(DeliveryChallanError.InvalidQuantity) } } catch (_: NumberFormatException) { throw DeliveryChallanFailure(DeliveryChallanError.InvalidQuantity) }
private fun status(value: String?): DeliveryChallanStatus = try { DeliveryChallanStatus.valueOf(value ?: "") } catch (_: IllegalArgumentException) { throw DeliveryChallanFailure(DeliveryChallanError.InvalidStoredStatus) }
private fun sourceType(value: String?): DeliveryChallanSourceType = try { DeliveryChallanSourceType.valueOf(value ?: "") } catch (_: IllegalArgumentException) { throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource) }
private fun date(value: Any?, required: Boolean): Long? = when (value) { null -> if (required) throw DeliveryChallanFailure(DeliveryChallanError.MalformedStoredDate) else null; is Timestamp -> value.toDate().time.takeIf { it > 0 } ?: throw DeliveryChallanFailure(DeliveryChallanError.MalformedStoredDate); is Date -> value.time.takeIf { it > 0 } ?: throw DeliveryChallanFailure(DeliveryChallanError.MalformedStoredDate); is Number -> value.toLong().takeIf { it > 0 } ?: throw DeliveryChallanFailure(DeliveryChallanError.MalformedStoredDate); is String -> value.toLongOrNull()?.takeIf { it > 0 } ?: throw DeliveryChallanFailure(DeliveryChallanError.MalformedStoredDate); else -> throw DeliveryChallanFailure(DeliveryChallanError.MalformedStoredDate) }
