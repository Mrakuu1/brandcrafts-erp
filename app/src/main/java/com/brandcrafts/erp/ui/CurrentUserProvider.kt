package com.brandcrafts.erp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.brandcrafts.erp.core.common.CurrentUserState

val LocalCurrentUser = staticCompositionLocalOf<CurrentUserState> {
    CurrentUserState.Unauthenticated
}

@Composable
fun CurrentUserProvider(
    currentUser: CurrentUserState,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCurrentUser provides currentUser, content = content)
}
