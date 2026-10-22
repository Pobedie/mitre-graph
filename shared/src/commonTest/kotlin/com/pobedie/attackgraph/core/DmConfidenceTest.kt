package com.pobedie.attackgraph.core

import com.pobedie.attackgraph.core.entity.Host
import com.pobedie.attackgraph.network.DecisionOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DmConfidenceTest {
    private val service = AttackVectorService()
    private val hosts = listOf(Host("host", "host", listOf("source", "target")))

    @Test
    fun thresholdIsInclusiveAndConfigurable() {
        val decisions = listOf(DecisionOutput("source", "target", 0.75f))
        for ((threshold, expected) in listOf(0f to 1, 0.5f to 1, 0.75f to 1, 0.8f to 0, 1f to 0)) {
            val edges = service.processDmDecisions(decisions, hosts, emptyList(), emptyList(), threshold)
            assertEquals(expected, edges.size)
        }
    }

    @Test
    fun rejectsInvalidThresholdsAndConfidences() {
        for (threshold in listOf(-0.1f, 1.1f, Float.NaN)) {
            assertFailsWith<IllegalArgumentException> {
                service.processDmDecisions(emptyList(), hosts, emptyList(), emptyList(), threshold)
            }
        }
        val decisions = listOf(-0.1f, 1.1f, Float.NaN, Float.POSITIVE_INFINITY).map {
            DecisionOutput("source", "target", it)
        }
        assertEquals(0, service.processDmDecisions(decisions, hosts, emptyList(), emptyList(), 0f).size)
    }
}
