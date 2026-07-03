package com.pobedie.attackgraph.core.entity

import androidx.compose.ui.graphics.Color

data class Node(
    val id: String, // format: hostId_techniqueId
    val techniqueId: String,
    val hostId: String,
    val hostName: String,
    val name: String,
    val description: String,
    val maturity: TechniqueMaturity,
    val severityScore: Int,
    val tactic: NodeTactic,
)

data class NodeTactic(
    val id: String,
    val name: String,
    val color: Color,
)