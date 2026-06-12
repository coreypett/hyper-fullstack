package org.coreypett.fullstack.orderbook.dto

internal data class L2BookLevelDto(
    val price: Double,
    val size: Double,
    val orderCount: Int,
)
