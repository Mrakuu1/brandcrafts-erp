package com.brandcrafts.erp.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.EmployeeResult
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.model.ActivityLog
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.model.InventoryItem
import com.brandcrafts.erp.domain.model.InvoiceSummary
import com.brandcrafts.erp.domain.model.PurchaseOrder
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.brandcrafts.erp.domain.model.DeliveryChallanSummary
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.ObserveInventoryItemsUseCase
import com.brandcrafts.erp.domain.usecase.activity.ObserveRecentActivitiesUseCase
import com.brandcrafts.erp.domain.usecase.contact.ObserveContactsUseCase
import com.brandcrafts.erp.domain.usecase.deliverychallan.ObserveDeliveryChallansUseCase
import com.brandcrafts.erp.domain.usecase.employee.ObserveEmployeesUseCase
import com.brandcrafts.erp.domain.usecase.invoice.ObserveInvoicesUseCase
import com.brandcrafts.erp.domain.usecase.purchaseorder.ObservePurchaseOrdersUseCase
import com.brandcrafts.erp.domain.usecase.quotation.ObserveQuotationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeInvoices: ObserveInvoicesUseCase,
    private val observeInventoryItems: ObserveInventoryItemsUseCase,
    private val observeEmployees: ObserveEmployeesUseCase,
    private val observeContacts: ObserveContactsUseCase,
    private val observeQuotations: ObserveQuotationsUseCase,
    private val observePurchaseOrders: ObservePurchaseOrdersUseCase,
    private val observeDeliveryChallans: ObserveDeliveryChallansUseCase,
    private val observeRecentActivities: ObserveRecentActivitiesUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state = _state.asStateFlow()

    private var invoices = emptyList<InvoiceSummary>()
    private var inventory = emptyList<InventoryItem>()
    private var employeeCount: Int? = null
    private var customerCount: Int? = null
    private var quotationCount: Int? = null
    private var purchaseOrderCount: Int? = null
    private var deliveryChallanCount: Int? = null
    private var quotations = emptyList<Quotation>()
    private var purchaseOrders = emptyList<PurchaseOrder>()
    private var deliveryChallans = emptyList<DeliveryChallanSummary>()
    private var activities = emptyList<ActivityLog>()
    private var anySnapshotLoaded = false
    private var partialDataFailure = false
    private var invoicesLoaded = false
    private var inventoryLoaded = false
    private var jobs = emptyList<Job>()

    init { retry() }

    fun retry() {
        jobs.forEach(Job::cancel)
        anySnapshotLoaded = false
        partialDataFailure = false
        _state.value = DashboardUiState.Loading
        jobs = listOf(
            collectInvoices(), collectInventory(), collectEmployees(), collectContacts(),
            collectQuotations(), collectPurchaseOrders(), collectDeliveryChallans(), collectActivities(),
        )
    }

    private fun collectInvoices() = viewModelScope.launch {
        observeInvoices().collect { result -> result.onSuccess { invoices = it; invoicesLoaded = true; loaded() }.onFailure { partialFailure() } }
    }

    private fun collectInventory() = viewModelScope.launch {
        observeInventoryItems().collect { result -> if (result is InventoryResult.Success) { inventory = result.data; inventoryLoaded = true; loaded() } else { partialFailure() } }
    }

    private fun collectEmployees() = viewModelScope.launch {
        observeEmployees().collect { result -> if (result is EmployeeResult.Success) { employeeCount = result.data.size; loaded() } else { partialFailure() } }
    }

    private fun collectContacts() = viewModelScope.launch {
        observeContacts().collect { result -> result.onSuccess { contacts -> customerCount = contacts.count { it.type == ContactType.CUSTOMER }; loaded() }.onFailure { partialFailure() } }
    }

    private fun collectQuotations() = viewModelScope.launch {
        observeQuotations().collect { result -> result.onSuccess { quotations = it; quotationCount = it.size; loaded() }.onFailure { partialFailure() } }
    }

    private fun collectPurchaseOrders() = viewModelScope.launch {
        observePurchaseOrders().collect { result -> result.onSuccess { purchaseOrders = it; purchaseOrderCount = it.size; loaded() }.onFailure { partialFailure() } }
    }

    private fun collectDeliveryChallans() = viewModelScope.launch {
        observeDeliveryChallans().collect { result -> result.onSuccess { deliveryChallans = it; deliveryChallanCount = it.size; loaded() }.onFailure { partialFailure() } }
    }

    private fun collectActivities() = viewModelScope.launch {
        try {
            observeRecentActivities().collect { result -> result.onSuccess { activities = it; loaded() }.onFailure { partialFailure() } }
        } catch (exception: CancellationException) { throw exception }
    }

    private fun loaded() {
        anySnapshotLoaded = true
        publish()
    }

    private fun partialFailure() {
        partialDataFailure = true
        if (anySnapshotLoaded) publish()
    }

    private fun publish() {
        val user = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user ?: return
        val lowStock = inventory.filter { it.active && it.availableQuantity <= it.minimumQuantity }
        val currentDateMillis = System.currentTimeMillis()
        val currentMonthInvoices = invoices.filter { it.invoiceDateMillis.isInCurrentCalendarMonth(currentDateMillis) }
        val issued = currentMonthInvoices.filter { it.status == InvoiceStatus.ISSUED }
        val overdue = issued.filter { it.isOverdue(currentDateMillis) }
        _state.value = DashboardUiState.Loaded(
            adminMetrics = if (user.role == UserRole.ADMIN && anySnapshotLoaded) {
                AdminDashboardMetrics(
                    totalSales = if (invoicesLoaded) DashboardCurrencyAmount(issued.fold(BigDecimal.ZERO) { total, item -> total + item.grandTotal }, "INR") else null,
                    outstandingPayments = if (invoicesLoaded) DashboardCurrencyAmount(issued.fold(BigDecimal.ZERO) { total, item -> total + item.outstandingAmount }, "INR") else null,
                    overduePayments = if (invoicesLoaded) DashboardCurrencyAmount(overdue.fold(BigDecimal.ZERO) { total, item -> total + item.outstandingAmount }, "INR") else null,
                    lowStockCount = lowStock.size.takeIf { inventoryLoaded },
                    employeeCount = employeeCount,
                    customerCount = customerCount,
                    quotationCount = quotationCount,
                    invoiceCount = invoices.size.takeIf { invoicesLoaded },
                    purchaseOrderCount = purchaseOrderCount,
                    deliveryChallanCount = deliveryChallanCount,
                    draftQuotationCount = quotations.count { it.status == QuotationStatus.DRAFT }.takeIf { quotationCount != null },
                    issuedInvoiceCount = issued.size,
                    approvedPurchaseOrderCount = purchaseOrders.count { it.status == PurchaseOrderStatus.APPROVED }.takeIf { purchaseOrderCount != null },
                    dispatchedDeliveryChallanCount = deliveryChallans.count { it.status == DeliveryChallanStatus.DISPATCHED }.takeIf { deliveryChallanCount != null },
                )
            } else null,
            employeeMetrics = if (user.role == UserRole.EMPLOYEE && anySnapshotLoaded) EmployeeDashboardMetrics(null, lowStock.size) else null,
            lowStockAlerts = lowStock.map { LowStockAlert(it.id, it.name, it.availableQuantity, it.unit) },
            recentActivities = activities.map(::activityUi),
            hasPartialDataFailure = partialDataFailure,
        )
    }

    private fun activityUi(activity: ActivityLog): DashboardActivity = DashboardActivity(
        id = activity.id,
        titleRes = when (activity.module) {
            "INVOICES" -> R.string.dashboard_activity_invoice
            "DELIVERY_CHALLAN" -> R.string.dashboard_activity_delivery_challan
            "INVENTORY" -> R.string.dashboard_activity_inventory
            "CONTACTS" -> R.string.dashboard_activity_contact
            "EMPLOYEES" -> R.string.dashboard_activity_employee
            else -> R.string.dashboard_activity_business
        },
        description = activity.performedByName?.let { "$it · ${activity.description.ifBlank { activity.action }}" }
            ?: activity.description.ifBlank { activity.action },
        timestamp = activity.createdAtMillis?.let { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it)) }.orEmpty(),
        status = when (activity.action) {
            "INVOICE_ISSUED", "DELIVERY_CHALLAN_DISPATCHED", "STOCK_IN", "STOCK_OUT" -> DashboardActivityStatus.COMPLETED
            else -> DashboardActivityStatus.INFO
        },
    )

    private fun Long.isInCurrentCalendarMonth(currentDateMillis: Long): Boolean {
        val now = Calendar.getInstance()
        now.timeInMillis = currentDateMillis
        val invoiceDate = Calendar.getInstance()
        invoiceDate.timeInMillis = this
        return now.get(Calendar.YEAR) == invoiceDate.get(Calendar.YEAR) &&
            now.get(Calendar.MONTH) == invoiceDate.get(Calendar.MONTH)
    }
}
