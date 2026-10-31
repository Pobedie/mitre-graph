package com.pobedie.attackgraph.network

import kotlinx.serialization.json.Json

object DecisionParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(input: String, sourceId: String, targetIds: Set<String>): List<DecisionOutput> {
        val response = json.decodeFromString<DmResponse>(input)
        val answer = requireNotNull(response.answers["potential_attacks"]) {
            "Decision response is missing the potential_attacks answer"
        }
        val probabilities = if (answer.probabilities.isNotEmpty()) {
            answer.probabilities
        } else {
            val choice = requireNotNull(answer.choice) { "Decision response has no probabilities or choice" }
            val confidence = requireNotNull(answer.confidence) { "Decision response has no confidence" }
            mapOf(choice to confidence)
        }
        return probabilities.mapNotNull { (targetId, confidence) ->
            require(confidence.isFinite() && confidence in 0f..1f) {
                "Invalid decision confidence: $confidence"
            }
            if (targetId in targetIds && targetId != sourceId) {
                DecisionOutput(sourceId, targetId, confidence)
            } else {
                null
            }
        }
    }
}
