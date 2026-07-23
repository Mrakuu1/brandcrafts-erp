package com.brandcrafts.erp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.brandcrafts.erp.navigation.AppNavHost
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrandCraftsTheme {
                AppNavHost()
            }
        }
    }
}
