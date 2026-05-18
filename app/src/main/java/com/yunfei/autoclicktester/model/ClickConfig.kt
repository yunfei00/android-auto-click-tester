package com.yunfei.autoclicktester.model

data class ClickConfig(
    val intervalMs: Long = 1000L,
    val xPercent: Float = 50f,
    val yPercent: Float = 50f,
    val showTouchMarker: Boolean = true
)
