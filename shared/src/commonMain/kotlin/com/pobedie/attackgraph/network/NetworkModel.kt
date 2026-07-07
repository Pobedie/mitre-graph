package com.pobedie.attackgraph.network
import kotlinx.serialization.Serializable

@Serializable
data class Request(
    val model: String,
    val messages: List<Message>,
    val response_format: ResponseFormat? = null
)

@Serializable
data class Message(val role: String,val content: String)

@Serializable
data class ResponseFormat(val type: String = "json_object")

@Serializable
data class DecisionOutput(
    val sourceId: String,
    val targetId: String,
    val confidence: Float,
)