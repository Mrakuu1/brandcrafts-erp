package com.brandcrafts.erp.feature.purchaseorder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.feature.invoice.InvoiceListRoute
import com.brandcrafts.erp.feature.quotation.QuotationRoute
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.ui.components.EmptyState

@Composable
fun OrdersRoute(
    onCreateQuotation: () -> Unit,
    onEditQuotation: (String) -> Unit,
    onCreatePurchaseOrder: () -> Unit,
    onOpenPurchaseOrder: (String) -> Unit,
    onEditPurchaseOrder: (String) -> Unit,
    onCreateInvoice: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    onEditInvoice: (String) -> Unit,
    onUnauthorized: (String) -> Unit,
) {
    val user = (LocalCurrentUser.current as? CurrentUserState.Authenticated)?.user
    val showPurchaseOrders = user?.active == true && user.role == UserRole.ADMIN
    val tabs = buildList {
        add(OrdersTab.Quotations)
        add(OrdersTab.Invoices)
        if (showPurchaseOrders) add(OrdersTab.PurchaseOrders)
        add(OrdersTab.DeliveryChallans)
    }
    var selectedIndex by remember(tabs) { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TabRow(selectedTabIndex = selectedIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(selected = selectedIndex == index, onClick = { selectedIndex = index }, text = { Text(stringResource(tab.labelRes)) })
            }
        }
        when (tabs[selectedIndex]) {
            OrdersTab.Quotations -> QuotationRoute(onCreateQuotation, onEditQuotation)
            OrdersTab.PurchaseOrders -> PurchaseOrderRoute(onCreatePurchaseOrder, onOpenPurchaseOrder, onEditPurchaseOrder, onUnauthorized)
            OrdersTab.Invoices -> InvoiceListRoute(
                onCreateInvoice = onCreateInvoice,
                onOpenInvoice = onOpenInvoice,
                onEditInvoice = onEditInvoice,
                onRecordPayment = onOpenInvoice,
                onUnauthorized = onUnauthorized,
            )
            OrdersTab.DeliveryChallans -> EmptyState(
                title = stringResource(R.string.feature_coming_later),
                description = stringResource(R.string.placeholder_screen_message),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private enum class OrdersTab(val labelRes: Int) {
    Quotations(R.string.orders_tab_quotations),
    Invoices(R.string.orders_tab_invoices),
    PurchaseOrders(R.string.orders_tab_purchase_orders),
    DeliveryChallans(R.string.orders_tab_delivery_challans),
}
