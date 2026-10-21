package com.pobedie.attackgraph.network

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Node
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText

class LlmService(private val client: HttpClient) {

    suspend fun fetchModels(
        url: String,
        apiKey: String?
    ): List<String> {
        val fullUrl = decisionApiEndpoint(url, "models")
        val response = client.get(fullUrl) {
            expectSuccess = true
            header("Authorization", "Bearer ${apiKey?.takeIf { it.isNotBlank() } ?: "DUMMY_KEY"}")
        }
        val responseBody = response.body<ModelsResponse>()
        return responseBody.data.map { it.id }
    }

    suspend fun fetchDecision(
        url: String,
        apiKey: String?,
        model: String,
        techniques: List<Node>,
        mitigations: List<Mitigation>,
        attackVectors: List<AttackVector>,
        onProgress: (processed: Int, total: Int) -> Unit
    ): List<DecisionOutput> {

        val fullUrl = decisionApiEndpoint(url, "systemone")

        val uniqueTechniques = techniques.map { it.techniqueId }.distinct()
        val totalTechniques = uniqueTechniques.size
        val allDecisions = mutableListOf<DecisionOutput>()

        onProgress(0, totalTechniques)

        uniqueTechniques.forEachIndexed { index, sourceId ->
            val candidateTargets = uniqueTechniques.filter { it != sourceId }
            val targetBatches = candidateTargets.chunked(MAX_TARGETS_PER_REQUEST)

            for (batch in targetBatches) {
                val statePrompt = buildDecisionPrompt(
                    techniqueIds = setOf(sourceId) + batch,
                    techniques = techniques,
                    mitigations = mitigations,
                    attackVectors = attackVectors
                )

                val question = Question(
                    type = "choice",
                    instructions = "Which attack technique may follow after attack technique $sourceId",
                    criteria = batch.associateWith { it }
                )

                val requestBody = NimbleRequest(
                    model = model.ifBlank { "nimble" },
                    state = mapOf("AI_attack_graph" to statePrompt),
                    questions = mapOf("potential_attacks" to question)
                )

                val response = client.post(fullUrl) {
                    expectSuccess = true
                    header("Authorization", "Bearer ${apiKey?.takeIf { it.isNotBlank() } ?: "DUMMY_KEY"}")
                    header("Content-Type", "application/json")
                    setBody(requestBody)
                }

                val responseBody = response.bodyAsText()

                val parsedDecisions = DecisionParser.parse(responseBody, sourceId, batch.toSet())
                allDecisions.addAll(parsedDecisions)
            }

            onProgress(index + 1, totalTechniques)
        }

        return allDecisions
    }
}

// Accept a base URL or a previously saved API endpoint.
internal fun decisionApiEndpoint(url: String, endpoint: String): String {
    val base = url.trim().trimEnd('/')
        .removeSuffix("/chat/completions")
        .removeSuffix("/systemone")
        .removeSuffix("/models")
    return "$base/$endpoint"
}

private const val MAX_TARGETS_PER_REQUEST = 5
