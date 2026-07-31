package com.brandcrafts.erp.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/** Short, consistent motion specifications. They intentionally avoid slow workflow animations. */
object BrandMotion {
    const val FastDurationMillis = 120
    const val StandardDurationMillis = 180
    const val EmphasizedDurationMillis = 220

    fun <T> fast(): TweenSpec<T> = tween(FastDurationMillis, easing = FastOutSlowInEasing)
    fun <T> standard(): TweenSpec<T> = tween(StandardDurationMillis, easing = FastOutSlowInEasing)
    fun <T> emphasized(): TweenSpec<T> = tween(EmphasizedDurationMillis, easing = FastOutSlowInEasing)
}
