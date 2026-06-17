package com.pobedie.attackgraph.ui

import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.Tactic

data class ViewState(
    val stage: Stage = Stage.Import,
    val nodes: List<Node> = emptyList(),
    val edges: List<Edge> = emptyList(),

    // Import stage
    val filePath: String = "",
    val fileError: String? = null,
    val isProvidedAtlasDateSelected: Boolean = false,

    // Technique selectoin stage
    val isTechniqueSelectionStageAvailable: Boolean = false,
    val tactics: List<Tactic> = listOf(),
    val selectedTechniquesId: List<String> = listOf(),

    // Attack vector mapping stage
    val isAttackVectorMappingStageAvailable: Boolean = false,
    val selectedNode: String? = null,
    val selectedEdge: Pair<String, String>? = null // ids of start and end nodes
)

enum class Stage {
    Import,
    TechniqueSelection, // User selects techniques from tactics. Similar to MITRE ATLAS site
    AttackVectorsBuilding, // User draws edges between nodes and sets risk and penalty values
    MitigationsAndAttacks, // The program shows nodes that will be mitigated and edges that form a proven attack vector
    BestPath, // The program calculates the best path based on risks, penalties and mitigations
}
