package com.pobedie.attackgraph.core.entity

import androidx.compose.ui.graphics.Color

data class Node(
    val name: String,
    val id: String,
    val color: Color,
    val connectedNodes: List<String>
)
