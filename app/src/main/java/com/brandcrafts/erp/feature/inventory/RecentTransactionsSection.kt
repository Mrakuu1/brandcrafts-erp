package com.brandcrafts.erp.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import androidx.compose.ui.unit.dp

@Composable fun RecentTransactionsSection(state:RecentTransactionsUiState,onRetryClick:()->Unit,modifier:Modifier=Modifier){Column(modifier,verticalArrangement=Arrangement.spacedBy(12.dp)){SectionHeader(stringResource(R.string.recent_transactions));when(state){is RecentTransactionsUiState.Loading->LoadingView(message=stringResource(R.string.transactions_loading));is RecentTransactionsUiState.Empty->EmptyState(stringResource(R.string.no_recent_transactions),stringResource(R.string.no_recent_transactions_description));is RecentTransactionsUiState.Error->ErrorState(stringResource(R.string.transactions_error),stringResource(R.string.transactions_error_description),stringResource(R.string.retry),onRetryClick);is RecentTransactionsUiState.Loaded->state.transactions.forEach{t->Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){ListItem(headlineContent={Text(stringResource(t.typeLabelRes))},supportingContent={Column{Text(t.quantityText);t.referenceText?.let{Text(stringResource(R.string.transaction_reference,it))};t.remarks?.let{Text(stringResource(R.string.transaction_remarks,it))};Text(stringResource(t.performerLabelRes))}},trailingContent={Column{StatusChip(stringResource(t.typeLabelRes),tone=t.tone);Text(t.timestampText,style=MaterialTheme.typography.labelSmall)}})}}}}}
@Preview(showBackground=true) @Composable private fun AdminPreview(){BrandCraftsTheme{RecentTransactionsSection(RecentTransactionsUiState.Loaded("m",listOf(RecentTransactionUiModel("1",R.string.stock_in_title,StatusTone.SUCCESS,"4 rolls","Today",null,null,R.string.transaction_team_member))),{})}}
@Preview(showBackground=true) @Composable private fun EmployeeDarkPreview(){BrandCraftsTheme(darkTheme=true){RecentTransactionsSection(RecentTransactionsUiState.Loaded("m",listOf(RecentTransactionUiModel("1",R.string.material_usage_title,StatusTone.INFO,"2 rolls","Today","JOB-1","Used",R.string.transaction_you))),{})}}
@Preview(showBackground=true) @Composable private fun EmptyPreview(){BrandCraftsTheme{RecentTransactionsSection(RecentTransactionsUiState.Empty("m"),{})}}
@Preview(showBackground=true) @Composable private fun LoadingPreview(){BrandCraftsTheme{RecentTransactionsSection(RecentTransactionsUiState.Loading("m"),{})}}
@Preview(showBackground=true) @Composable private fun ErrorPreview(){BrandCraftsTheme{RecentTransactionsSection(RecentTransactionsUiState.Error("m"),{})}}
