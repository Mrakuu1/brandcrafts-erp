package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

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
    val darkMode = MaterialTheme.colorScheme.background.red < .2f
    val containerShape = RoundedCornerShape(20.dp)
    val accent = if (darkMode) Color(0xFFFF6A00) else Color(0xFFFF6500)
    val unselected = if (darkMode) Color(0xFFB2BBC6) else Color(0xFF6B6B6B)
    // Insets and outer spacing must stay outside the painted surface. Keeping
    // them on Surface made the navigation-bar reserve look like a solid block.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Color.Transparent)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp,),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(60.dp).background(color = Color.Transparent),
            shape = containerShape,
            color = if (darkMode) Color(0xFF111A25) else Color.White,
            contentColor = if (darkMode) Color(0xFFF8FAFC) else Color(0xFF141414),
            shadowElevation = if (darkMode) 2.dp else 8.dp,
            border = if (darkMode) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF283646)) else null,
        ) {
            NavigationBar(
                modifier = Modifier.height(60.dp),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0),
            ) {
                items.forEach { item ->
                    val selected = item.id == selectedItemId
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onItemSelected(item) },
                        icon = {
                            when {
                                item.drawableRes != null -> Icon(
                                    modifier = Modifier.size(21.dp),
                                    painter = painterResource(item.drawableRes),
                                    contentDescription = item.contentDescription,
                                )

                                item.icon != null -> Icon(
                                    modifier = Modifier.size(21.dp),
                                    imageVector = item.icon,
                                    contentDescription = item.contentDescription,
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = unselected,
                            unselectedTextColor = unselected,
                        ),
                    )
                }
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
