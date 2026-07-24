package com.brandcrafts.erp.feature.inventory

sealed interface StockInUiEffect { data object NavigateBack : StockInUiEffect; data object Saved : StockInUiEffect }
