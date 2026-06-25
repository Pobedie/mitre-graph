package com.pobedie.attackgraph.core.entity

data class Mitigation(
    val id: String,
    val name: String,
    val mitigationDescription: String,
    val relationshipDescription: String,
    val targetTechnique: String,
    val categories: List<String>,
    val lifecyclePhases: List<String>,
    val isRelevant: Boolean
)