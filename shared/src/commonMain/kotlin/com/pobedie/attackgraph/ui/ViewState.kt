package com.pobedie.attackgraph.ui

import com.pobedie.attackgraph.core.entity.AttackVector
import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.FirewallRule
import com.pobedie.attackgraph.core.entity.Host
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.Tactic

data class ViewState(
    val stage: Stage = Stage.Import,
    val nodes: List<Node> = emptyList(),
    val edges: List<Edge> = emptyList(),
    val hosts: List<Host> = emptyList(),
    val targetGoal: TargetGoal = TargetGoal.HighestSeverity,
    val targetTechniques: List<Pair<String, String>> = emptyList(),
    val rootNodeGoal: RootNodeGoal = RootNodeGoal.Automatic,
    val rootTechniques: List<Pair<String, String>> = emptyList(),

    // Import stage
    val filePath: String = "",
    val fileError: String? = null,
    val isProvidedAtlasDateSelected: Boolean = false,

    // Technique selection stage
    val isTechniqueSelectionStageAvailable: Boolean = false,
    val tactics: List<Tactic> = listOf(),
    val selectedTechniquesId: List<String> = listOf(),
    val isTargetSelectionInProgress: Boolean = false,
    val isRootSelectionInProgress: Boolean = false,
    val currentHostIndex: Int = 0,

    //Firewall mapping
    val isFirewallMappingStageAvailable: Boolean = false,
    val firewallRules: List<FirewallRule> = emptyList(),

    // Attack vector mapping stage
    val isAttackVectorMappingStageAvailable: Boolean = false,
    val selectedNode: String? = null,
    val selectedEdge: Pair<String, String>? = null, // ids of start and end nodes

    // Possible attack vectors stage
    val isPossibleAttackVectorsStageAvailable: Boolean = false,
    val attackVectors: List<AttackVector> = emptyList(),
    val mitigations: List<Mitigation> = emptyList(),

    // App settings
    val isDarkMode: Boolean = false,
    val language: Language = Language.English,
    val consoleText: String = "",
    val isConsoleFrozen: Boolean = false,

    // LLM settings
    val llmUrl: String = "",
    val llmApiKey: String = "",
    val llmModel: String = "",
    val availableLlmModels: List<String> = emptyList(),
    val isLlmModelsLoading: Boolean = false,
    val llmConnectionStatus: LlmConnectionStatus = LlmConnectionStatus.None,
    val isGenerationInProgress: Boolean = false,
    val isLlmAdded: Boolean = false
)

enum class LlmConnectionStatus {
    None,
    Connecting,
    Connected,
    Failed
}

enum class Language(val code: String) {
    English("en"),
    Russian("ru")
}

enum class TargetGoal {
    HighestSeverity,
    Specific
}

enum class RootNodeGoal {
    Automatic,
    Manual
}

enum class Stage {
    Import,
    TechniqueSelection, // User selects techniques from tactics. Similar to MITRE ATLAS site
    FirewallMapping, // User maps allowed routes between hosts
    AttackVectorsBuilding, // User draws edges between nodes and sets risk and penalty values
    EdgeValueCalculation, // Calculates the values of the edges
    PossibleAttackVectors, // Shows nodes that will be mitigated and edges that form a proven attack vector
}
