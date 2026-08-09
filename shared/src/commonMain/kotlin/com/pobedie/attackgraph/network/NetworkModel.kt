package com.pobedie.attackgraph.network
import kotlinx.serialization.Serializable

@Serializable
data class Request(
    val model: String,
    val messages: List<Message>,
    val response_format: String = "json_schema"
)

@Serializable
data class Message(val role: String,val content: String)

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