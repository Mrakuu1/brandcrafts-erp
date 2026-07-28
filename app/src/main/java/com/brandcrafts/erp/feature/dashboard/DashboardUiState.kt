package com.brandcrafts.erp.feature.dashboard

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    data class Loaded(
        val adminMetrics: AdminDashboardMetrics? = null,
        val employeeMetrics: EmployeeDashboardMetrics? = null,
        val recentActivities: List<DashboardActivity> = emptyList(),
        val assignedTasks: List<DashboardTask> = emptyList(),
        val lowStockAlerts: List<LowStockAlert> = emptyList(),
        val hasPartialDataFailure: Boolean = false,
    ) : DashboardUiState

    data class Error(val type: DashboardErrorType) : DashboardUiState
}

enum class DashboardErrorType {
    NETWORK,
    UNKNOWN,
}
