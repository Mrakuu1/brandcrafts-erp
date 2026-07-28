package com.brandcrafts.erp.feature.quotation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.OutlinedButton
import com.brandcrafts.erp.ui.components.SectionHeader

@Composable
fun QuotationDetailsScreen(
    state: QuotationDetailsUiState,
    onEvent: (QuotationDetailsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.quotation_title),
                navigationIcon = Icons.Outlined.ArrowBack,
                navigationContentDescription = stringResource(R.string.quotation_form_back),
                onNavigationClick = { onEvent(QuotationDetailsUiEvent.Back) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading && state.quotation == null -> LoadingView(
                modifier = Modifier.padding(innerPadding),
                message = stringResource(R.string.quotation_details_loading),
            )
            state.error && state.quotation == null -> ErrorState(
                title = stringResource(R.string.quotation_details_error),
                description = stringResource(R.string.quotation_details_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(QuotationDetailsUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = { onEvent(QuotationDetailsUiEvent.Back) },
                modifier = Modifier.padding(innerPadding),
            )
            state.quotation != null -> QuotationDetailsBody(
                quotation = state.quotation,
                pdfGenerating = state.pdfGenerating,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
            else -> ErrorState(
                title = stringResource(R.string.quotation_details_error),
                description = stringResource(R.string.quotation_details_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(QuotationDetailsUiEvent.Retry) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun QuotationDetailsBody(
    quotation: Quotation,
    pdfGenerating: Boolean,
    onEvent: (QuotationDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (pdfGenerating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { QuotationHeaderCard(quotation) }
            item { SectionHeader(title = stringResource(R.string.quotation_lines)) }
            itemsIndexed(quotation.lines, key = { _, line -> line.id }) { index, line ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            stringResource(
                                R.string.quotation_details_line_description,
                                index + 1,
                                line.description,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(
                                R.string.quotation_details_line_quantity,
                                line.quantity.toPlainString(),
                                line.unit,
                            ),
                        )
                        Text(
                            stringResource(
                                R.string.quotation_details_line_total,
                                formatIndianCurrency(line.total),
                            ),
                        )
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(
                        text = stringResource(R.string.quotation_grand_total, formatIndianCurrency(quotation.grandTotal)),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            if (quotation.remarks.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(stringResource(R.string.quotation_notes), style = MaterialTheme.typography.titleMedium)
                            Text(quotation.remarks)
                        }
                    }
                }
            }
        }
        QuotationDetailsActions(pdfGenerating = pdfGenerating, onEvent = onEvent)
    }
}

@Composable
private fun QuotationHeaderCard(quotation: Quotation) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(quotation.number, style = MaterialTheme.typography.headlineSmall)
            quotation.dateMillis?.let { Text(stringResource(R.string.quotation_date, java.text.DateFormat.getDateInstance().format(java.util.Date(it)))) }
            quotation.validUntilMillis?.let { Text(stringResource(R.string.quotation_valid_until, java.text.DateFormat.getDateInstance().format(java.util.Date(it)))) }
        }
    }
}

@Composable
private fun QuotationDetailsActions(
    pdfGenerating: Boolean,
    onEvent: (QuotationDetailsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            text = stringResource(R.string.quotation_preview_pdf),
            onClick = { onEvent(QuotationDetailsUiEvent.PreviewPdf) },
            enabled = !pdfGenerating,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            text = stringResource(R.string.quotation_share_pdf),
            onClick = { onEvent(QuotationDetailsUiEvent.SharePdf) },
            enabled = !pdfGenerating,
            modifier = Modifier.fillMaxWidth(),
        )
        if (pdfGenerating) Text(stringResource(R.string.quotation_generating_pdf), style = MaterialTheme.typography.labelMedium)
    }
}
