package org.coreypett.fullstack.market.candle.dto

import kotlinx.serialization.Serializable

@Serializable
internal data class CandleSnapshotRequestDto(
    val type: String,
    val req: CandleSnapshotReqDto,
)

@Serializable
internal data class CandleSnapshotReqDto(
    val coin: String,
    val interval: String,
    val startTime: Long,
    val endTime: Long,
)
