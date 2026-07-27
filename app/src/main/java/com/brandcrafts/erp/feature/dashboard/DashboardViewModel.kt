package com.brandcrafts.erp.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.model.InventoryItem
import com.brandcrafts.erp.domain.model.InvoiceSummary
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.ObserveInventoryItemsUseCase
import com.brandcrafts.erp.domain.usecase.invoice.ObserveInvoicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeInvoices: ObserveInvoicesUseCase,
    private val observeInventoryItems: ObserveInventoryItemsUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _state = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val state = _state.asStateFlow()
    private var invoices: List<InvoiceSummary> = emptyList()
    private var inventory: List<InventoryItem> = emptyList()
    private var invoicesLoaded = false
    private var inventoryLoaded = false
    private var invoicesJob: Job? = null
    private var inventoryJob: Job? = null

    init { retry() }

    fun retry() {
        invoicesJob?.cancel()
        inventoryJob?.cancel()
        _state.value = DashboardUiState.Loading
        invoicesJob = viewModelScope.launch {
            observeInvoices().collect { result ->
                result.onSuccess { invoices = it; invoicesLoaded = true }
                publish()
            }
        }
        inventoryJob = viewModelScope.launch {
            observeInventoryItems().collect { result ->
                if (result is InventoryResult.Success) {
                    inventory = result.data
                    inventoryLoaded = true
                }
                publish()
            }
        }
    }

    private fun publish() {
        val user = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user ?: return
        val lowStock = inventory.filter { it.active && it.availableQuantity <= it.minimumQuantity }
        val issued = invoices.filter { it.status == InvoiceStatus.ISSUED }
        _state.value = DashboardUiState.Loaded(
            adminMetrics = if (user.role == UserRole.ADMIN && invoicesLoaded && inventoryLoaded) AdminDashboardMetrics(
                totalSales = DashboardCurrencyAmount(issued.fold(BigDecimal.ZERO) { total, item -> total + item.grandTotal }, "INR"),
                outstandingPayments = DashboardCurrencyAmount(issued.fold(BigDecimal.ZERO) { total, item -> total + item.outstandingAmount }, "INR"),
                lowStockCount = lowStock.size,
            ) else null,
            employeeMetrics = if (user.role == UserRole.EMPLOYEE && inventoryLoaded) EmployeeDashboardMetrics(null, lowStock.size) else null,
            lowStockAlerts = lowStock.map { LowStockAlert(it.id, it.name, it.availableQuantity, it.unit) },
        )
    }
}
