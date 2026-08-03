package com.brandcrafts.erp.feature.purchaseorder

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.feature.invoice.InvoiceListRoute
import com.brandcrafts.erp.feature.quotation.QuotationRoute
import com.brandcrafts.erp.ui.LocalCurrentUser
import com.brandcrafts.erp.feature.deliverychallan.DeliveryChallanListRoute

@Composable
fun OrdersRoute(
    onCreateQuotation: () -> Unit,
    onEditQuotation: (String) -> Unit,
    onOpenQuotation: (String) -> Unit,
    onCreatePurchaseOrder: () -> Unit,
    onOpenPurchaseOrder: (String) -> Unit,
    onEditPurchaseOrder: (String) -> Unit,
    onCreateInvoice: () -> Unit,
    onOpenInvoice: (String) -> Unit,
    onEditInvoice: (String) -> Unit,
    onCreateDeliveryChallan: () -> Unit,
    onCreateDeliveryChallanFromInvoice: () -> Unit,
    onOpenDeliveryChallan: (String) -> Unit,
    onEditDeliveryChallan: (String) -> Unit,
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
    Column(modifier = Modifier.fillMaxSize()) {
        OrdersTabRow(
            tabs = tabs,
            selectedIndex = selectedIndex,
            onTabSelected = { selectedIndex = it },
        )
        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
            when (tabs[selectedIndex]) {
            OrdersTab.Quotations -> QuotationRoute(onCreateQuotation, onEditQuotation, onOpenQuotation)
            OrdersTab.PurchaseOrders -> PurchaseOrderRoute(onCreatePurchaseOrder, onOpenPurchaseOrder, onEditPurchaseOrder, onUnauthorized)
            OrdersTab.Invoices -> InvoiceListRoute(
                onCreateInvoice = onCreateInvoice,
                onOpenInvoice = onOpenInvoice,
                onEditInvoice = onEditInvoice,
                onRecordPayment = onOpenInvoice,
                onUnauthorized = onUnauthorized,
            )
            OrdersTab.DeliveryChallans -> DeliveryChallanListRoute(
                onCreate = onCreateDeliveryChallan,
                onCreateFromInvoice = onCreateDeliveryChallanFromInvoice,
                onDetails = onOpenDeliveryChallan,
                onEdit = onEditDeliveryChallan,
                onUnauthorized = onUnauthorized,
            )
            }
        }
    }
}

@Composable
private fun OrdersTabRow(
    tabs: List<OrdersTab>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.red < .2f
    val shape = RoundedCornerShape(18.dp)
    val selectedTint = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) .25f else .13f)

    // This intentionally mirrors PeopleTabs: equal cells keep the employee's
    // three available modules centered instead of leaving a fourth empty slot.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(shape)
            .background(if (isDark) Color(0xFF111A25) else Color(0xFFF9F4F0))
            .padding(3.dp),
    ) {
        val itemGap = 4.dp
        val segmentWidth = (maxWidth - itemGap * (tabs.size - 1)) / tabs.size
        val selectedOffset by animateDpAsState(
            targetValue = (segmentWidth + itemGap) * selectedIndex,
            animationSpec = tween(durationMillis = 180),
            label = "ordersTabSlide",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = selectedOffset)
                    .width(segmentWidth)
                    .height(34.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(selectedTint),
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(itemGap),
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = index == selectedIndex
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else {
                            if (isDark) Color(0xFFB2BBC6) else Color(0xFF6B6B6B)
                        },
                        label = "ordersTabText",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .clickable { onTabSelected(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(tab.labelRes),
                            color = contentColor,
                            fontSize = 9.sp,
                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private enum class OrdersTab(val labelRes: Int) {
    Quotations(R.string.orders_tab_quotations),
    Invoices(R.string.orders_tab_invoices),
    PurchaseOrders(R.string.orders_tab_purchase_orders),
    DeliveryChallans(R.string.orders_tab_delivery_challans),
}
