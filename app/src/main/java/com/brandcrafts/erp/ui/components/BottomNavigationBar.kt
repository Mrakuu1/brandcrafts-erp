package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

data class BottomNavigationItem(
    val id: String,
    val label: String,
    @DrawableRes val drawableRes: Int? = null,
    val icon: ImageVector? = null,
    val contentDescription: String? = null,
)

@Composable
fun BottomNavigationBar(
    items: List<BottomNavigationItem>,
    selectedItemId: String,
    onItemSelected: (BottomNavigationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = BrandVisualTokens.OverlaySurfaceAlpha),
        shadowElevation = BrandVisualTokens.BottomBarElevation,
    ) {
        NavigationBar(
            modifier = Modifier.height(64.dp),
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
        ) {
            items.forEach { item ->
                val selected = item.id == selectedItemId
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemSelected(item) },
                    icon = {
                        when {
                            item.drawableRes != null -> Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(item.drawableRes),
                                contentDescription = item.contentDescription,
                            )

                            item.icon != null -> Icon(
                                modifier = Modifier.size(24.dp),
                                imageVector = item.icon,
                                contentDescription = item.contentDescription,
                            )
                        }
                    },
                    label = { Text(text = item.label) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavigationBarPreview() {
    val items = listOf(
        BottomNavigationItem("home", "Home", R.drawable.home_icon),
        BottomNavigationItem("stock", "Stock", R.drawable.inventory_icon),
    )
    BrandCraftsTheme {
        BottomNavigationBar(items = items, selectedItemId = "home", onItemSelected = {})
    }
}
