package com.brandcrafts.erp.feature.dashboard

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.bottomsheet.BrandBottomSheet
import com.brandcrafts.erp.ui.theme.BrandMotion

data class DashboardQuickActionOption(
    val id: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun DashboardQuickActionGrid(
    actions: List<DashboardQuickActionOption>,
    editable: Boolean,
    modifier: Modifier = Modifier,
) {
    var appliedSelection by rememberSaveable(actions.map(DashboardQuickActionOption::id)) {
        mutableStateOf(actions.take(DEFAULT_VISIBLE_ACTIONS).map(DashboardQuickActionOption::id))
    }
    var editing by rememberSaveable { mutableStateOf(false) }
    val visibleActions = actions.filter { it.id in appliedSelection }.take(MAX_VISIBLE_ACTIONS)

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.dashboard_quick_actions),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            )
            if (editable) {
                IconButton(onClick = { editing = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.dashboard_edit_quick_actions),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            visibleActions.forEach { action ->
                DashboardQuickActionTile(option = action, modifier = Modifier.weight(1f))
            }
        }
    }

    if (editing) {
        DashboardQuickActionEditorSheet(
            actions = actions,
            initialSelection = appliedSelection,
            onDismissRequest = { editing = false },
            onSave = {
                appliedSelection = it
                editing = false
            },
        )
    }
}

@Composable
private fun DashboardQuickActionEditorSheet(
    actions: List<DashboardQuickActionOption>,
    initialSelection: List<String>,
    onDismissRequest: () -> Unit,
    onSave: (List<String>) -> Unit,
) {
    var temporarySelection by remember(initialSelection) { mutableStateOf(initialSelection) }
    BrandBottomSheet(
        title = stringResource(R.string.dashboard_edit_quick_actions),
        onDismissRequest = onDismissRequest,
        containerColor = dashboardDialogColor(),
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.dashboard_edit_quick_actions),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(Icons.Outlined.Cancel, contentDescription = stringResource(R.string.close))
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.padding(top = 8.dp).heightIn(max = 650.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_quick_actions_selection_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = dashboardSecondaryColor(),
            )
            Column(
                modifier = Modifier.heightIn(max = 470.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                    actions.chunked(EDITOR_COLUMNS).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            row.forEach { action ->
                                val selected = action.id in temporarySelection
                                DashboardQuickActionSelectionCard(
                                    option = action,
                                    selected = selected,
                                    onClick = {
                                        temporarySelection = when {
                                            selected && temporarySelection.size > MIN_VISIBLE_ACTIONS ->
                                                temporarySelection - action.id
                                            !selected && temporarySelection.size < MAX_VISIBLE_ACTIONS ->
                                                temporarySelection + action.id
                                            else -> temporarySelection
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            repeat(EDITOR_COLUMNS - row.size) { Box(modifier = Modifier.weight(1f)) }
                        }
                    }
            }
            Text(
                text = stringResource(
                    R.string.dashboard_selected_actions,
                    temporarySelection.size,
                    MAX_VISIBLE_ACTIONS,
                ),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
            )
            DashboardSaveButton(onClick = { onSave(temporarySelection) })
        }
    }
}

@Composable
private fun DashboardQuickActionSelectionCard(
    option: DashboardQuickActionOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = actionColor(option.id)
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .height(124.dp)
            .clip(shape)
            .background(dashboardCardColor())
            .border(1.dp, color.copy(alpha = if (selected) .85f else .55f), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.dp, if (selected) color else dashboardSecondaryColor(), CircleShape)
                    .background(if (selected) color else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), Color.White)
            }
        }
        Icon(option.icon, null, Modifier.size(30.dp), color)
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun DashboardQuickActionTile(option: DashboardQuickActionOption, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, BrandMotion.fast(), label = "dashboardActionScale")
    val color = actionColor(option.id)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(4.dp, RoundedCornerShape(12.dp), clip = false)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.linearGradient(listOf(color, color.copy(alpha = .74f))))
                .clickable(interactionSource = interactionSource, indication = null, onClick = option.onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(option.icon, null, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun DashboardSaveButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.dashboard_save_changes),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun dashboardDialogColor(): Color = if (isDashboardDark()) Color(0xFF111A25) else Color.White

@Composable
private fun dashboardCardColor(): Color = if (isDashboardDark()) Color(0xFF16212E) else Color.White

@Composable
private fun dashboardSecondaryColor(): Color = if (isDashboardDark()) Color(0xFFB2BBC6) else Color(0xFF6B6B6B)

@Composable
private fun isDashboardDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

private fun actionColor(actionId: String): Color = when (actionId) {
    "add_stock" -> Color(0xFFFF6500)
    "create_invoice" -> Color(0xFF2E71C7)
    "create_quotation" -> Color(0xFF20894E)
    "employees" -> Color(0xFFCE3540)
    "stock_in" -> Color(0xFF20894E)
    "stock_out" -> Color(0xFFCE3540)
    "material_usage" -> Color(0xFFE67B19)
    else -> Color(0xFFFF6500)
}

private const val MIN_VISIBLE_ACTIONS = 2
private const val DEFAULT_VISIBLE_ACTIONS = 4
private const val MAX_VISIBLE_ACTIONS = 4
private const val EDITOR_COLUMNS = 2
