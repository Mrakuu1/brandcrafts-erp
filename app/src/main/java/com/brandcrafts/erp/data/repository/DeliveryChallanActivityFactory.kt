package com.brandcrafts.erp.data.repository

data class PreparedDeliveryChallanActivity(
    val activityId: String,
    val action: String,
    val challanId: String,
    val challanNumber: String,
    val actorId: String,
    val actorDisplayName: String,
)

object DeliveryChallanActivityFactory {
    const val CREATED = "DELIVERY_CHALLAN_CREATED"
    const val UPDATED = "DELIVERY_CHALLAN_UPDATED"
    const val DISPATCHED = "DELIVERY_CHALLAN_DISPATCHED"
    const val CANCELLED = "DELIVERY_CHALLAN_CANCELLED"

    fun created(activityId: String, challanId: String, number: String, actorId: String, actorName: String) = create(activityId, CREATED, challanId, number, actorId, actorName)
    fun updated(activityId: String, challanId: String, number: String, actorId: String, actorName: String) = create(activityId, UPDATED, challanId, number, actorId, actorName)
    fun dispatched(activityId: String, challanId: String, number: String, actorId: String, actorName: String) = create(activityId, DISPATCHED, challanId, number, actorId, actorName)
    fun cancelled(activityId: String, challanId: String, number: String, actorId: String, actorName: String) = create(activityId, CANCELLED, challanId, number, actorId, actorName)
    private fun create(activityId: String, action: String, challanId: String, number: String, actorId: String, actorName: String) = PreparedDeliveryChallanActivity(activityId, action, challanId, number, actorId, actorName)
}
