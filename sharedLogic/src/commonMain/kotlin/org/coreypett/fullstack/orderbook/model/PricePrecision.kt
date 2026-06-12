package org.coreypett.fullstack.orderbook.model

enum class PricePrecision(val nSigFigs: Int, val label: String) {
    Two(2, "2 sig figs"),
    Three(3, "3 sig figs"),
    Four(4, "4 sig figs"),
    Five(5, "5 sig figs"),
}
