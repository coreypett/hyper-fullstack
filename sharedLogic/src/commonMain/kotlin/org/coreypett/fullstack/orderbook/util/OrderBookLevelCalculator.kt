package org.coreypett.fullstack.orderbook.util

import org.coreypett.fullstack.orderbook.model.LevelChange

internal object OrderBookLevelCalculator {
    fun depthFraction(size: Double, maxSize: Double): Float =
        (size / if (maxSize > 0.0) maxSize else 1.0).toFloat().coerceIn(0f, 1f)

    fun change(previousSize: Double?, currentSize: Double): LevelChange = when {
        previousSize == null -> LevelChange.None
        currentSize > previousSize -> LevelChange.Up
        currentSize < previousSize -> LevelChange.Down
        else -> LevelChange.None
    }

    fun changeFraction(previousSize: Double?, currentSize: Double): Float {
        if (previousSize == null) return 0f
        val denominator = maxOf(kotlin.math.abs(previousSize), kotlin.math.abs(currentSize), 1.0)
        return (kotlin.math.abs(currentSize - previousSize) / denominator).toFloat()
    }
}
