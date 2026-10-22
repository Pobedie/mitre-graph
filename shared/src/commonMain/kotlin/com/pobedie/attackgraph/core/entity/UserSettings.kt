package com.pobedie.attackgraph.core.entity

data class UserSettings(
    val dmUrl: String,
    val dmApiKey: String,
    val dmModel: String,
    val dmConfidenceThreshold: Float = 0.5f
)
