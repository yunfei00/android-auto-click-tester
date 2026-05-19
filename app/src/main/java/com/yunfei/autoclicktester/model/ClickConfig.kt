package com.yunfei.autoclicktester.model

data class ClickPointConfig(
    val label: String,
    val intervalMs: Long,
    val xPercent: Float,
    val yPercent: Float
)

data class ClickConfig(
    val pointOne: ClickPointConfig = ClickPointConfig("1", 1000L, 50f, 50f),
    val pointTwo: ClickPointConfig = ClickPointConfig("2", 1500L, 75f, 50f),
    val enableSecondPoint: Boolean = false,
    val showTouchMarker: Boolean = true
) {
    val activePoints: List<ClickPointConfig>
        get() = if (enableSecondPoint) listOf(pointOne, pointTwo) else listOf(pointOne)
}
