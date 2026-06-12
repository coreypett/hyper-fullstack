package org.coreypett.fullstack.orderbook.util

import org.coreypett.fullstack.orderbook.model.LevelChange

internal object OrderBookLevelCalculator {
    fun depthFraction(size: Double, maxSize: Double): Float =
        (size / maxSize.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)

    fun change(previousSize: Double?, currentSize: Double): LevelChange = when {
        previousSize == null -> LevelChange.None
        currentSize > previousSize -> LevelChange.Up
        currentSize < previousSize -> LevelChange.Down
        else -> LevelChange.None
    }
}
