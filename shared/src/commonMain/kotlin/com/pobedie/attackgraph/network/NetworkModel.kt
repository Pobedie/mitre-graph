package com.pobedie.attackgraph.network
import kotlinx.serialization.Serializable

@Serializable
data class DmRequest(
    val model: String,
    val state: Map<String, String>,
    val questions: Map<String, Question>
)

@Serializable
data class Question(
    val type: String,
    val instructions: String,
    val criteria: Map<String, String>
)

@Serializable
data class DmResponse(
    val model: String? = null,
    val answers: Map<String, QuestionAnswer>,
    val usage: UsageData? = null
)

@Serializable
data class QuestionAnswer(
    val type: String? = null,
    val choice: String? = null,
    val probabilities: Map<String, Float> = emptyMap(),
    val confidence: Float? = null
)

@Serializable
data class UsageData(
    val input_tokens: Int? = null,
    val output_tokens: Int? = null
)

@Serializable
data class DecisionOutput(
    val sourceId: String,
    val targetId: String,
    val confidence: Float,
)

@Serializable
data class ModelsResponse(
    val data: List<ModelData>
)

@Serializable
data class ModelData(
    val id: String
)