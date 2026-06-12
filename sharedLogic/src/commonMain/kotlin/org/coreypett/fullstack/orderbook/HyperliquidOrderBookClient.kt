package org.coreypett.fullstack.orderbook

import kotlinx.coroutines.flow.Flow

interface HyperliquidOrderBookClient {
    fun snapshots(selection: OrderBookSelection): Flow<OrderBookSnapshot>
}
