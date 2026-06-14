package org.coreypett.fullstack.di

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class SharedDependencyGraphTest {
    @AfterTest
    fun tearDown() {
        SharedDependencyGraph.stop()
    }

    @Test
    fun resolvesDefaultRepositories() {
        SharedDependencyGraph.start()

        assertNotNull(SharedDependencyGraph.orderBookRepository())
        assertNotNull(SharedDependencyGraph.candleRepository())
        assertNotNull(SharedDependencyGraph.livePriceRepository())
        assertNotNull(SharedDependencyGraph.candleChartFeature())
    }
}
