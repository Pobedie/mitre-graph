package com.pobedie.attackgraph.ui

import androidx.compose.ui.graphics.Color
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.path_cost_format
import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.Node
import org.jetbrains.compose.resources.getString
import kotlin.math.absoluteValue

fun generateColorFromId(id: String): Color {
    val hash = id.hashCode() * 999
    val hue = (hash.absoluteValue % 360).toFloat()
    val saturation = 0.5f + (hash.absoluteValue % 30) / 100f
    val value = 0.45f
    return Color.hsv(hue, saturation, value)
}

suspend fun formatPath(
    pathResult: Pair<List<Edge>, Double>,
    nodes: List<Node>
): String {
    val edges = pathResult.first
    val cost = pathResult.second
    val formattedCost = try {
        "%.3f".format(cost)
    } catch (e: Exception) {
        cost.toString()
    }
    if (edges.isEmpty()) return getString(Res.string.path_cost_format, "", formattedCost)
    
    val nodeNames = mutableListOf<String>()
    val firstNode = nodes.find { it.id == edges.first().startNode }
    if (firstNode != null) nodeNames.add("${firstNode.hostName}_${firstNode.techniqueId}")
    
    nodeNames.addAll(edges.mapNotNull { edge ->
        val node = nodes.find { it.id == edge.endNode } ?: return@mapNotNull null
        "${node.hostName}_${node.techniqueId}"
    })
    
    return getString(Res.string.path_cost_format, nodeNames.joinToString(" -> "), formattedCost)
}
