package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

data class BottomNavigationItem(
    val id: String,
    val label: String,
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
    NavigationBar(modifier = modifier.fillMaxWidth()) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.id == selectedItemId,
                onClick = { onItemSelected(item) },
                icon = {
                    item.icon?.let { icon ->
                        Icon(imageVector = icon, contentDescription = item.contentDescription)
                    }
                },
                label = { Text(text = item.label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomNavigationBarPreview() {
    val items = listOf(
        BottomNavigationItem("home", "Home"),
        BottomNavigationItem("stock", "Stock"),
    )
    BrandCraftsTheme {
        BottomNavigationBar(items = items, selectedItemId = "home", onItemSelected = {})
    }
}
