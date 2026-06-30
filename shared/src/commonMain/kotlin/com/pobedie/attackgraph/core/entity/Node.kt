package com.pobedie.attackgraph.core.entity

import androidx.compose.ui.graphics.Color

data class Node(
    val id: String,
    val name: String,
    val description: String,
    val maturity: TechniqueMaturity,
    val tactic: NodeTactic,
)

data class NodeTactic(
    val id: String,
    val name: String,
    val color: Color,
)