package org.coreypett.fullstack.orderbook.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class L2BookDataDto(
    val coin: String,
    val levels: List<List<L2BookLevelDto>>,
    val time: Long = 0L,
)
