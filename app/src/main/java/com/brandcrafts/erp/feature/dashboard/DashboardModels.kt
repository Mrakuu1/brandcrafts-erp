package com.brandcrafts.erp.feature.dashboard

import java.math.BigDecimal

data class DashboardCurrencyAmount(
    val amount: BigDecimal,
    val currencyCode: String,
)

data class AdminDashboardMetrics(
    val totalSales: DashboardCurrencyAmount?,
    val outstandingPayments: DashboardCurrencyAmount?,
    val lowStockCount: Int?,
    val employeeCount: Int?,
    val customerCount: Int?,
    val quotationCount: Int?,
    val invoiceCount: Int?,
    val purchaseOrderCount: Int?,
    val deliveryChallanCount: Int?,
    val draftQuotationCount: Int?,
    val issuedInvoiceCount: Int?,
    val approvedPurchaseOrderCount: Int?,
    val dispatchedDeliveryChallanCount: Int?,
)

data class EmployeeDashboardMetrics(
    val assignedTaskCount: Int?,
    val lowStockAlertCount: Int,
)

data class DashboardActivity(
    val id: String,
    @androidx.annotation.StringRes val titleRes: Int,
    val description: String,
    val timestamp: String,
    val status: DashboardActivityStatus,
)

enum class DashboardActivityStatus {
    PENDING,
    COMPLETED,
    INFO,
}

data class DashboardTask(
    val id: String,
    val title: String,
    val description: String,
    val status: DashboardTaskStatus,
)

enum class DashboardTaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
}

data class LowStockAlert(
    val id: String,
    val materialName: String,
    val availableQuantity: Double,
    val unit: String,
)
