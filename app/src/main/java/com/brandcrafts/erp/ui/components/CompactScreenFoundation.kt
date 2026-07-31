package com.brandcrafts.erp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandSpacing

/** Shared list/form shell: a compact app bar and a single primary content region. */
@Composable
fun CompactScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    navigationIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList(),
    floatingActionButton: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        /*topBar = {
            CompactTopBar(
                title = title,
                navigationIcon = navigationIcon,
                navigationContentDescription = navigationContentDescription,
                onNavigationClick = onNavigationClick,
                actions = actions,
            )
        },*/
        snackbarHost = { snackbarHostState?.let { SnackbarHost(hostState = it) } },
        floatingActionButton = { floatingActionButton?.invoke() },
    ) { innerPadding -> content(innerPadding) }
}

/** Uses the existing app-bar implementation with the compact Material 3 treatment. */
@Composable
fun CompactTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    navigationContentDescription: String? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList(),
) {
    AppTopBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        navigationContentDescription = navigationContentDescription,
        onNavigationClick = onNavigationClick,
        actions = actions,
    )
}

@Composable
fun CompactSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BrandSpacing.Sm),
    ) {
        title?.let {
            SectionHeader(title = it, actionLabel = actionLabel, onActionClick = onActionClick)
        }
        content()
    }
}

@Composable
fun CompactSummaryCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = BrandCard(modifier = modifier, contentPadding = PaddingValues(BrandSpacing.Md), content = content)

@Composable
fun CompactListCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = BrandCard(
    modifier = modifier.fillMaxWidth(),
    onClick = onClick,
    contentPadding = PaddingValues(BrandSpacing.Md),
    content = content,
)

/** Horizontally scrollable, always-visible basic filters. Advanced filters belong in a sheet. */
@Composable
fun CompactFilterRow(
    filters: List<CompactFilter>,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(BrandSpacing.Sm),
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = filter.selected,
                onClick = { onFilterSelected(filter.id) },
                label = { Text(filter.label) },
            )
        }
    }
}

data class CompactFilter(
    val id: String,
    val label: String,
    val selected: Boolean,
)

/** Anchors a single primary form action above system navigation insets. */
@Composable
fun StickyActionBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BrandSpacing.Lg),
            horizontalArrangement = Arrangement.spacedBy(BrandSpacing.Md),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
fun ExpandableDetailsSection(
    title: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    expandLabel: String,
    collapseLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BrandCard(modifier = modifier.animateContentSize(), contentPadding = PaddingValues(BrandSpacing.Md)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            androidx.compose.material3.TextButton(onClick = { onExpandedChange(!expanded) }) {
                Text(if (expanded) collapseLabel else expandLabel)
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(top = BrandSpacing.Sm),
            ) { content() }
        }
    }
}

@Composable
fun CompactFormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompactSection(title = title, modifier = modifier) {
        CompactSummaryCard { content() }
    }
}
