package com.brandcrafts.erp.feature.quotation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SearchBar
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.feature.purchaseorder.OrdersFabAction
import com.brandcrafts.erp.feature.purchaseorder.OrdersFilterBottomSheet
import com.brandcrafts.erp.feature.purchaseorder.OrdersFilterChoice
import com.brandcrafts.erp.feature.purchaseorder.OrdersFilterChoiceList
import com.brandcrafts.erp.feature.purchaseorder.OrdersListScaffold
import com.brandcrafts.erp.feature.purchaseorder.OrdersDocumentCard
import com.brandcrafts.erp.feature.purchaseorder.OrdersCardAction
import com.brandcrafts.erp.feature.purchaseorder.OrdersCardActions
import com.brandcrafts.erp.feature.purchaseorder.OrdersDocumentLeadingIcon
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date

@Composable
fun QuotationScreen(
    state: QuotationUiState,
    canManageQuotations: Boolean,
    onCreateQuotation: () -> Unit,
    onEditQuotation: (String) -> Unit,
    onOpenQuotation: (String) -> Unit,
    onApproveQuotation: (String) -> Unit,
    onRejectQuotation: (String) -> Unit,
    onSearch: (String) -> Unit,
    onStatus: (QuotationStatus?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by remember { mutableStateOf(false) }
    var pendingStatus by remember(state.status) { mutableStateOf(state.status) }
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    OrdersListScaffold(
        query = state.query,
        onQueryChange = onSearch,
        searchPlaceholder = stringResource(R.string.quotation_search),
        refreshing = state.content == QuotationUiState.Content.Loading && state.all.isNotEmpty(),
        onRefresh = onRetry,
        isFilterActive = state.status != null,
        onOpenFilters = {
            pendingStatus = state.status
            filtersOpen = true
        },
        snackbarHostState = snackbarHostState,
        createAction = if (canManageQuotations) {
            OrdersFabAction(stringResource(R.string.quotation_create), onCreateQuotation)
        } else {
            null
        },
        modifier = modifier,
    ) {
        when {
            state.content == QuotationUiState.Content.Loading && state.visible.isEmpty() -> {
                LoadingView(message = stringResource(R.string.quotation_loading))
            }
            state.content == QuotationUiState.Content.Error && state.visible.isEmpty() -> ErrorState(
                stringResource(R.string.quotation_error), stringResource(R.string.quotation_error_description),
                stringResource(R.string.retry), onRetry,
            )
            state.content == QuotationUiState.Content.Empty -> EmptyState(
                stringResource(if (state.query.isBlank()) R.string.quotation_empty else R.string.quotation_no_results),
                stringResource(R.string.quotation_empty_description),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.visible, key = { it.quotation.id }) { item ->
                    QuotationListItem(
                        item = item,
                        canEdit = canManageQuotations && item.quotation.status == QuotationStatus.DRAFT,
                        canChangeStatus = canManageQuotations && item.quotation.status == QuotationStatus.DRAFT,
                        onEdit = { onEditQuotation(item.quotation.id) },
                        onOpen = { onOpenQuotation(item.quotation.id) },
                        onApprove = { onApproveQuotation(item.quotation.id) },
                        onReject = { onRejectQuotation(item.quotation.id) },
                    )
                }
            }
        }
    }
    if (filtersOpen) {
        OrdersFilterBottomSheet(
            onDismissRequest = { filtersOpen = false },
            onApply = {
                onStatus(pendingStatus)
                filtersOpen = false
            },
        ) {
            OrdersFilterChoiceList(
                choices = buildList {
                    add(
                        OrdersFilterChoice(
                            id = "all",
                            label = stringResource(R.string.quotation_all),
                            selected = pendingStatus == null,
                            onSelected = { pendingStatus = null },
                        ),
                    )
                    QuotationStatus.entries.forEach { status ->
                        add(
                            OrdersFilterChoice(
                                id = status.name,
                                label = stringResource(status.res()),
                                selected = pendingStatus == status,
                                onSelected = { pendingStatus = status },
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun QuotationListItem(
    item: QuotationListItem,
    canEdit: Boolean,
    canChangeStatus: Boolean,
    onEdit: () -> Unit,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val quotation = item.quotation
    OrdersDocumentCard(onClick = onOpen) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { OrdersDocumentLeadingIcon() },
            headlineContent = { Text(quotation.number) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.customerDisplayName ?: stringResource(R.string.quotation_customer_unavailable))
                    Text(stringResource(R.string.quotation_date, formatDate(quotation.dateMillis)))
                    Text(stringResource(R.string.quotation_valid_until, formatDate(quotation.validUntilMillis)))
                    Text(currency(quotation.grandTotal))
                }
            },
            trailingContent = {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    StatusChip(stringResource(quotation.status.res()), tone = tone(quotation.status))
                }
            },
        )
        OrdersCardActions(
            buildList {
                if (canEdit) add(OrdersCardAction(stringResource(R.string.quotation_edit), Icons.Outlined.Edit, onEdit))
                if (canChangeStatus) {
                    add(OrdersCardAction(stringResource(R.string.quotation_approve), Icons.Outlined.Edit, onApprove))
                    add(OrdersCardAction(stringResource(R.string.quotation_reject), Icons.Outlined.Edit, onReject))
                }
            },
        )
    }
}

private fun QuotationStatus.res() = when (this) {
    QuotationStatus.DRAFT -> R.string.quotation_status_draft
    QuotationStatus.APPROVED -> R.string.quotation_status_approved
    QuotationStatus.REJECTED -> R.string.quotation_status_rejected
    QuotationStatus.EXPIRED -> R.string.quotation_status_expired
}

private fun tone(status: QuotationStatus) = when (status) {
    QuotationStatus.DRAFT -> StatusTone.NEUTRAL
    QuotationStatus.APPROVED -> StatusTone.SUCCESS
    QuotationStatus.REJECTED -> StatusTone.ERROR
    QuotationStatus.EXPIRED -> StatusTone.WARNING
}

private fun formatDate(value: Long?) = value?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) } ?: "—"
private fun currency(value: BigDecimal) = formatIndianCurrency(value)

@Preview(showBackground = true)
@Composable
private fun QuotationListAdminPreview() {
    BrandCraftsTheme {
        QuotationScreen(
            state = QuotationUiState(content = QuotationUiState.Content.Loaded, visible = listOf(QuotationListItem(Quotation("1", "QT-000001", "c", 0, 0, QuotationStatus.DRAFT, BigDecimal("1250.00"), "", "", ""), "Acme Studio"))),
            canManageQuotations = true,
            onCreateQuotation = {}, onEditQuotation = {}, onOpenQuotation = {}, onApproveQuotation = {}, onRejectQuotation = {}, onSearch = {}, onStatus = {}, onRetry = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuotationListEmployeePreview() { BrandCraftsTheme { QuotationScreen(previewQuotationList(QuotationStatus.APPROVED), false, {}, {}, {}, {}, {}, {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationListMultiplePreview() { BrandCraftsTheme { QuotationScreen(previewQuotationList(QuotationStatus.DRAFT, QuotationStatus.APPROVED, QuotationStatus.REJECTED, QuotationStatus.EXPIRED), true, {}, {}, {}, {}, {}, {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationListMissingCustomerPreview() { BrandCraftsTheme { QuotationScreen(previewQuotationList(QuotationStatus.DRAFT, customerName = null), true, {}, {}, {}, {}, {}, {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationListLoadingPreview() { BrandCraftsTheme { QuotationScreen(QuotationUiState(), true, {}, {}, {}, {}, {}, {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationListEmptyPreview() { BrandCraftsTheme { QuotationScreen(QuotationUiState(content = QuotationUiState.Content.Empty), true, {}, {}, {}, {}, {}, {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationListNoResultsPreview() { BrandCraftsTheme { QuotationScreen(QuotationUiState(content = QuotationUiState.Content.Empty, query = "missing"), true, {}, {}, {}, {}, {}, {}, {}, {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationListErrorPreview() { BrandCraftsTheme { QuotationScreen(QuotationUiState(content = QuotationUiState.Content.Error), true, {}, {}, {}, {}, {}, {}, {}, {}) } }

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuotationListDarkPreview() { BrandCraftsTheme(darkTheme = true) { QuotationScreen(previewQuotationList(QuotationStatus.DRAFT), true, {}, {}, {}, {}, {}, {}, {}, {}) } }

private fun previewQuotationList(vararg statuses: QuotationStatus, customerName: String? = "Acme Studio"): QuotationUiState {
    val items = statuses.mapIndexed { index, status ->
        QuotationListItem(
            quotation = Quotation("preview-$index", "QT-00000${index + 1}", "customer-$index", 0, 0, status, BigDecimal("1250.00"), "", "", ""),
            customerDisplayName = customerName,
        )
    }
    return QuotationUiState(content = QuotationUiState.Content.Loaded, visible = items)
}

@Composable
private fun quotationsDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

@Composable
private fun quotationCardColor(): Color = if (quotationsDark()) Color(0xFF111A25) else Color.White
