package com.pobedie.attackgraph.network

import com.pobedie.attackgraph.core.entity.AttackVector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DecisionParserTest {
    @Test
    fun bindsDecisionsToRequestedSourceAndTargets() {
        val decisions = DecisionParser.parse(
            """{"answers":{"potential_attacks":{"probabilities":{"target":0.8,"source":0.9,"unrequested":0.9}}}}""",
            "source", setOf("target")
        )
        assertEquals(listOf(DecisionOutput("source", "target", 0.8f)), decisions)
    }

    @Test
    fun preservesLowConfidenceForDomainFiltering() {
        val decisions = DecisionParser.parse(
            """{"answers":{"potential_attacks":{"probabilities":{"target":0.2}}}}""",
            "source", setOf("target")
        )
        assertEquals(0.2f, decisions.single().confidence)
    }

    @Test
    fun rejectsApiErrorsAndMissingAnswers() {
        for (response in listOf("""{"error":"unauthorized"}""", """{"answers":{}}""", "not json")) {
            assertFailsWith<IllegalArgumentException> {
                DecisionParser.parse(response, "source", setOf("target"))
            }
        }
    }

    @Test
    fun neverInventsConfidence() {
        assertFailsWith<IllegalArgumentException> {
            DecisionParser.parse(
                """{"answers":{"potential_attacks":{"choice":"target"}}}""",
                "source", setOf("target")
            )
        }
    }

    @Test
    fun supportsChoiceWithExplicitConfidence() {
        assertEquals(listOf(DecisionOutput("source", "target", 0.7f)), DecisionParser.parse(
            """{"answers":{"potential_attacks":{"choice":"target","confidence":0.7}}}""",
            "source", setOf("target")
        ))
    }

    @Test
    fun rejectsOutOfRangeConfidence() {
        assertFailsWith<IllegalArgumentException> {
            DecisionParser.parse(
                """{"answers":{"potential_attacks":{"probabilities":{"target":1.2}}}}""",
                "source", setOf("target")
            )
        }
    }

    @Test
    fun ignoresUnknownTargetsWithoutFallingBackToTextParsing() {
        assertTrue(DecisionParser.parse(
            """{"answers":{"potential_attacks":{"probabilities":{"unknown":0.9}}}}""",
            "source", setOf("target")
        ).isEmpty())
    }

    @Test
    fun promptUsesCaseStudyIdAndOnlyRelevantVectors() {
        val vectors = listOf(
            AttackVector("case-study", 1, "step-one", "tactic", "target", "relevant evidence", emptyList()),
            AttackVector("other-study", 2, "step-two", "tactic", "other", "excluded evidence", emptyList())
        )
        val prompt = buildDecisionPrompt(setOf("target"), emptyList(), emptyList(), vectors)
        assertTrue("BELONGS TO CASE-STUDY: case-study" in prompt)
        assertTrue("STEP ID: step-one" in prompt)
        assertTrue("excluded evidence" !in prompt)
    }

    @Test
    fun normalizesSavedEndpointsForBothOperations() {
        for (suffix in listOf("", "/", "/systemone", "/systemone/", "/chat/completions", "/models/")) {
            assertEquals("http://localhost:11434/v1/systemone", decisionApiEndpoint("http://localhost:11434/v1$suffix", "systemone"))
            assertEquals("http://localhost:11434/v1/models", decisionApiEndpoint("http://localhost:11434/v1$suffix", "models"))
        }
    }
}
