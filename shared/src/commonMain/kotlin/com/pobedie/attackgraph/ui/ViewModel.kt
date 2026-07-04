package com.pobedie.attackgraph.ui

import androidx.compose.ui.graphics.Color
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.file_blank_error
import attackgraph.shared.generated.resources.file_not_found_error
import attackgraph.shared.generated.resources.no_optimal_path_found
import attackgraph.shared.generated.resources.optimal_path_label
import attackgraph.shared.generated.resources.path_cost_format
import attackgraph.shared.generated.resources.probable_paths_label
import attackgraph.shared.generated.resources.target_not_selected_error
import attackgraph.shared.generated.resources.unexpected_error
import attackgraph.shared.generated.resources.host_name_format
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.core.calculateProbabilitiesSimple
import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.EdgeState
import com.pobedie.attackgraph.core.entity.Host
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.NodeTactic
import com.pobedie.attackgraph.core.entity.Tactic
import com.pobedie.attackgraph.core.findOptimalPath
import java.io.File
import kotlin.math.absoluteValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import java.util.Locale
import java.util.UUID


class ViewModel(
    val scope: CoroutineScope,
    val mainRepository: MainRepository
) {

    private val _state = MutableStateFlow<ViewState>(ViewState())
    val state = _state.asStateFlow()

    init {
        scope.launch {
            val initialHostName = getString(Res.string.host_name_format, 1)
            _state.update {
                it.copy(
                    hosts = listOf(
                        Host(
                            name = initialHostName,
                            id = UUID.randomUUID().toString(),
                            techniques = emptyList()
                        )
                    )
                )
            }
        }
        // todo: maybe make it a function
        scope.launch {
            mainRepository.importState.collectLatest { isImportSuccessful ->
                if (isImportSuccessful) {
                    _state.update {
                        it.copy(isTechniqueSelectionStageAvailable = true)
                    }
                    switchToTechniqueSelectionStage()
                }
            }
        }
        // Handle side effects from state change
        state.onEach { currentState ->
            val isEdgeValueCalculationStageAvailable =
                currentState.edges.size >= 3 &&
                        state.value.targetTechnique != null &&
                        state.value.edges.any { it.endNode.endsWith(state.value.targetTechnique!!) }

            val mitigationAndAttackStageAvailable: Boolean = (
                    currentState.edges.size >= 3 &&
                            currentState.edges.none { it.probability == null } &&
                            state.value.edges.any { it.endNode.endsWith(state.value.targetTechnique!!) } &&
                            (currentState.stage == Stage.EdgeValueCalculation || currentState.stage == Stage.MitigationsAndAttacks)
                    )
            val isAttackVectorMappingStageAvailable =
                currentState.hosts.count { it.techniques.isNotEmpty() } >= 3 && state.value.targetTechnique != null
            _state.update {
                it.copy(
                    isMitigationsAndAttacksStageAvailable = mitigationAndAttackStageAvailable,
                    isAttackVectorMappingStageAvailable = isAttackVectorMappingStageAvailable,
                    isEdgeValueCalculationStageAvailable = isEdgeValueCalculationStageAvailable
                )
            }
        }.launchIn(scope)
    }

    fun switchToImportStage() {
        clearConsole()
        _state.update { it.copy(stage = Stage.Import) }
    }

    fun switchToTechniqueSelectionStage() {
        clearConsole()
        var tactics: List<Tactic>
        scope.launch {
            tactics = mainRepository.getTacticsWithTechniques()
            _state.update {
                it.copy(
                    stage = Stage.TechniqueSelection,
                    tactics = tactics
                )
            }
        }
    }

    fun switchToAttackVectorBuildingStage() {
        clearConsole()
        val nodes: List<Node> = state.value.hosts.flatMap { host ->
            host.techniques.map { technique ->
                val tacticName = state.value.tactics.findLast { it.id == technique.tacticId }?.name.orEmpty()
                val color = generateColorFromId(technique.tacticId)
                Node(
                    id = "${host.id}_${technique.id}",
                    techniqueId = technique.id,
                    hostId = host.id,
                    hostName = host.name,
                    name = technique.name,
                    description = technique.description,
                    maturity = technique.maturity,
                    severityScore = technique.severityScore,
                    tactic = NodeTactic(
                        id = technique.tacticId,
                        name = tacticName,
                        color = color,
                    ),
                )
            }
        }
//        User might build the graph then go back to SelectTechnique stage and deselect nodes.
//        If we don't handle this, we will have incorrect optimal path calculations
        val newEdges = state.value.edges.mapNotNull { _edge ->
            if (nodes.any { it.id == _edge.startNode } && nodes.any { it.id == _edge.endNode }) {
                _edge
            } else null
        }
        _state.update {
            it.copy(
                stage = Stage.AttackVectorsBuilding,
                nodes = nodes,
                edges = newEdges,
                selectedNode = null,
            )
        }
        // Since we know the target technique, we can already start case-study fetching
        if (state.value.targetTechnique != null) {
            scope.launch {
                val attackVectors = mainRepository.getAttackVectors(state.value.targetTechnique!!)
                val uniqueTechniqueIds = nodes.map { it.techniqueId }.distinct()
                val mitigations = mainRepository.getMittigations(uniqueTechniqueIds)
                _state.update {
                    it.copy(
                        attackVectors = attackVectors,
                        mitigations = mitigations
                    )
                }
            }
        } else {
            scope.launch {
                logToUiConsole(getString(Res.string.target_not_selected_error))
            }
        }
    }

    fun startTargetTechniqueSelection() {
        _state.update {
            it.copy(
                isTargetSelectionInProgress = true
            )
        }
    }

    fun selectTargetTechnique(target: String) {
        _state.update {
            it.copy(
                targetTechnique = target,
                isTargetSelectionInProgress = false
            )
        }
    }

    fun switchToEdgeValueCalculationStage() {
        val updatedEdges = calculateProbabilitiesSimple(
            edges = state.value.edges,
            nodes = state.value.nodes,
        )
        _state.update {
            it.copy(
                stage = Stage.EdgeValueCalculation,
                edges = updatedEdges
            )
        }
    }

    fun switchToMitigationsAndAttacks() {
        val rootNodes: List<String> =
                state.value.nodes
                        .filter { _node ->
                            state.value.edges.none { _edge -> _edge.endNode == _node.id }
                        }
                        .map { it.id }

        val targetTechnique = state.value.targetTechnique
        val allEdges = state.value.edges
        val probablePaths: MutableList<Pair<List<Edge>, Double>> = mutableListOf()
        var optimalPath: Pair<List<Edge>, Double>? = null

        if (targetTechnique != null) {
            val targetNodes = state.value.nodes.filter { it.techniqueId == targetTechnique }
            rootNodes.forEach { _rootNode ->
                targetNodes.forEach { targetNode ->
                    val pathResult = findOptimalPath(
                        edges = allEdges,
                        start = _rootNode,
                        target = targetNode.id
                    )
                    if (pathResult != null) {
                        probablePaths.add(pathResult)
                        val currentOptimal = optimalPath
                        if (currentOptimal == null || pathResult.second < currentOptimal.second) {
                            optimalPath = pathResult
                        }
                    }
                }
            }
        }

        val finalOptimalPath = optimalPath
        val newEdges =
            if (finalOptimalPath != null) {
                allEdges.map { _edge ->
                    if (finalOptimalPath.first.contains(_edge)) {
                        _edge.copy( state = EdgeState.MostOptimal )
                    } else if (
                        probablePaths.any {
                            it.first.contains(_edge)
                        }
                    ) {
                        _edge.copy( state = EdgeState.Probable )
                    }
                    else _edge
                }
            } else {
                allEdges
            }

        _state.update {
            it.copy(
                    stage = Stage.MitigationsAndAttacks,
                    edges = newEdges
            )
        }
        clearConsole()
        scope.launch {
            if (finalOptimalPath != null) {
                logToUiConsole(getString(Res.string.optimal_path_label), freezeDisplay = true)
                logToUiConsole(formatPath(finalOptimalPath), freezeDisplay = true)

                val otherProbablePaths = probablePaths.filter { it != finalOptimalPath }.take(4)
                if (otherProbablePaths.isNotEmpty()) {
                    logToUiConsole("\n" + getString(Res.string.probable_paths_label), freezeDisplay = true)
                    otherProbablePaths.forEach {
                        logToUiConsole(formatPath(it), freezeDisplay = true)
                    }
                }
            } else {
                logToUiConsole(getString(Res.string.no_optimal_path_found), freezeDisplay = true)
            }
        }
    }

    fun importAtlasData(){
        _state.update { it.copy(fileError = null) }
        scope.launch {
            try {
                var fileBinary: ByteArray? = null
                if (state.value.isProvidedAtlasDateSelected) {
                    fileBinary = Res.readBytes(PROVIDED_ATLAS_DATA_PATH)
                } else if (state.value.filePath.isNotBlank()) {
                    val file = File(state.value.filePath)
                    if (file.exists()) {
                        fileBinary = withContext(Dispatchers.IO) {
                            file.readBytes()
                        }
                    } else {
                        val error = getString(Res.string.file_not_found_error, state.value.filePath)
                        _state.update { it.copy(fileError = error) }
                        logToUiConsole("Error: $error")
                    }
                }

                if (fileBinary != null) {
                    val fileContent = fileBinary.decodeToString()
                    if (fileContent.isNotBlank()) {
                        mainRepository.importMitreAtlasData(fileContent)
                    } else {
                        val error = getString(Res.string.file_blank_error, state.value.filePath)
                        _state.update { it.copy(fileError = error) }
                        logToUiConsole("Error: $error")
                    }
                }
            } catch (e: Exception) {
                val error = getString(Res.string.unexpected_error, e.localizedMessage ?: "")
                _state.update { it.copy(fileError = error) }
                e.printStackTrace()
            }
        }
    }

    fun selectFile(
        path: String? = null,
        useDefault: Boolean = false
    ) {
        _state.update {
            it.copy(
                filePath = path ?: it.filePath,
                isProvidedAtlasDateSelected = useDefault
            )
        }
    }

    fun selectTechnique(techniqueId: String) {
        _state.update { state ->
            val techniqueAlreadySelected = state.selectedTechniquesId.contains(techniqueId)
            val newSelections = state.selectedTechniquesId.toMutableList()
            val newHosts = state.hosts.toMutableList()
            val currentHost = newHosts[state.currentHostIndex]

            if (techniqueAlreadySelected) {
                newSelections.remove(techniqueId)
                val newHostTechniques = currentHost.techniques.filter { it.id != techniqueId }
                newHosts[state.currentHostIndex] = currentHost.copy(techniques = newHostTechniques)
            } else {
                newSelections.add(techniqueId)
                val allTechniques = state.tactics.flatMap { it.techniques }
                val technique = allTechniques.find { it.id == techniqueId }
                if (technique != null) {
                    val newHostTechniques = currentHost.techniques.toMutableList()
                    newHostTechniques.add(technique)
                    newHosts[state.currentHostIndex] = currentHost.copy(techniques = newHostTechniques)
                }
            }
            state.copy(
                selectedTechniquesId = newSelections,
                hosts = newHosts
            )
        }
    }

    fun nextHost() {
        _state.update {
            if (it.currentHostIndex < it.hosts.size - 1) {
                val nextIndex = it.currentHostIndex + 1
                it.copy(
                    currentHostIndex = nextIndex,
                    selectedTechniquesId = it.hosts[nextIndex].techniques.map { it.id }
                )
            } else {
                it
            }
        }
    }

    fun previousHost() {
        _state.update {
            if (it.currentHostIndex > 0) {
                val prevIndex = it.currentHostIndex - 1
                it.copy(
                    currentHostIndex = prevIndex,
                    selectedTechniquesId = it.hosts[prevIndex].techniques.map { it.id }
                )
            } else {
                it
            }
        }
    }

    fun addHost() {
        scope.launch {
            val newIndex = state.value.hosts.size + 1
            val newHostName = getString(Res.string.host_name_format, newIndex)
            _state.update {
                val newHosts = it.hosts.toMutableList()
                newHosts.add(
                    Host(
                        newHostName,
                        UUID.randomUUID().toString(),
                        emptyList()
                    )
                )
                it.copy(
                    hosts = newHosts,
                    currentHostIndex = newHosts.size - 1,
                    selectedTechniquesId = emptyList()
                )
            }
        }
    }

    fun deleteHost(hostId: String) {
        _state.update { state ->
            if (state.hosts.size <= 1) return@update state // Keep at least one host

            val newHosts = state.hosts.filter { it.id != hostId }
            val newIndex = if (state.currentHostIndex >= newHosts.size) {
                newHosts.size - 1
            } else {
                state.currentHostIndex
            }

            state.copy(
                hosts = newHosts,
                currentHostIndex = newIndex,
                selectedTechniquesId = newHosts[newIndex].techniques.map { it.id }
            )
        }
    }

    fun updateHostName(hostId: String, newName: String) {
        _state.update { state ->
            val newHosts = state.hosts.map {
                if (it.id == hostId) it.copy(name = newName) else it
            }
            state.copy(hosts = newHosts)
        }
    }

    fun updateTechniqueSeverityScore(hostId: String, techniqueId: String, score: Int) {
        _state.update { state ->
            val newHosts = state.hosts.map { host ->
                if (host.id == hostId) {
                    val newTechniques = host.techniques.map { tech ->
                        if (tech.id == techniqueId) tech.copy(severityScore = score) else tech
                    }
                    host.copy(techniques = newTechniques)
                } else host
            }
            state.copy(hosts = newHosts)
        }
    }

    fun removeTechniqueFromHost(hostId: String, techniqueId: String) {
        _state.update { state ->
            val newHosts = state.hosts.map { host ->
                if (host.id == hostId) {
                    val newTechniques = host.techniques.filter { it.id != techniqueId }
                    host.copy(techniques = newTechniques)
                } else host
            }
            val newSelections = state.selectedTechniquesId.toMutableList()
            newSelections.remove(techniqueId)
            state.copy(
                hosts = newHosts,
                selectedTechniquesId = newSelections
            )
        }
    }

    fun clearTechniqueSelectoins(){
        _state.update {
            it.copy(
                selectedTechniquesId = listOf(),
                hosts = it.hosts.map { host -> host.copy(techniques = emptyList()) },
                targetTechnique = null,
                isTargetSelectionInProgress = false,
                isAttackVectorMappingStageAvailable = false
            )
        }
    }

    fun setNodeConnection(selectedNode: String) {
        _state.update { state ->
            if (state.selectedNode == null) {
                state.copy(
                    selectedNode = selectedNode
                )
            } else if (
                selectedNode == state.selectedNode ||
                state.edges.find { edge ->
                    (edge.startNode == state.selectedNode && edge.endNode == selectedNode)
                } != null
            ) {
                state.copy(
                    selectedNode = null
                )
            } else {
                val newEdges = state.edges.toMutableList()
                newEdges.add(
                    Edge(
                        startNode = state.selectedNode,
                        endNode = selectedNode,
                    )
                )
                state.copy(
                    edges = newEdges,
                    selectedNode = null
                )
            }
        }
    }

    fun selectEdge(startNode: String, endNode: String) {
        _state.update {
            it.copy(
                selectedEdge = Pair(startNode, endNode)
            )
        }
    }

    fun clearEdgeSelection() {
        _state.update {
            it.copy(
                selectedEdge = null
            )
        }
    }

    fun clearNodeSelection() {
        _state.update {
            it.copy(
                selectedNode = null
            )
        }
    }

    fun deleteEdge(startNode: String, endNode: String) {
        _state.update {
            val newEdges = it.edges.mapNotNull {
                if (it.startNode == startNode && it.endNode == endNode) null else it
            }
            it.copy(edges = newEdges)
        }
    }

    fun changeEdgeProbability(startNode: String, endNode: String, value: Float) {
        _state.update {
            val newEdges = it.edges.map {
                if (it.startNode == startNode && it.endNode == endNode) {
                    it.copy(
                        probability = value
                    )
                } else it
            }
            it.copy(edges = newEdges)
        }
    }

    fun toggleMitigationRelevance(mitigation: String) {
        _state.update {
            val newMitigations = it.mitigations.map {
                if (it.id == mitigation) it.copy(isRelevant = !it.isRelevant) else it
            }
            it.copy( mitigations = newMitigations )
        }
    }

    fun changeLanguage(language: Language) {
        _state.update {
            it.copy(language = language)
        }
        // Workaround for Desktop to update locale for Compose Resources
        try {
            val locale = Locale(language.code)
            Locale.setDefault(locale)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun logToUiConsole(message: String, freezeDisplay: Boolean = false) {
        _state.update {
            it.copy(
                consoleText = it.consoleText + (if (it.consoleText.isEmpty()) "" else "\n") + message,
                isConsoleFrozen = freezeDisplay
            )
        }
    }

    fun clearConsole() {
        _state.update {
            it.copy(consoleText = "")
        }
    }

    private suspend fun formatPath(pathResult: Pair<List<Edge>, Double>): String {
        val edges = pathResult.first
        val cost = pathResult.second
        val formattedCost = try {
            "%.3f".format(cost)
        } catch (e: Exception) {
            cost.toString()
        }
        if (edges.isEmpty()) return getString(Res.string.path_cost_format, "", formattedCost)
        val nodes = mutableListOf<String>()
        val firstTechnique = state.value.nodes.find { it.hostId+"_"+it.techniqueId == edges.first().startNode }
        if (firstTechnique != null) nodes.add(firstTechnique.hostName + "_" + firstTechnique.techniqueId)
        nodes.addAll(edges.mapNotNull { _edge ->
            val hostTechnique = state.value.nodes.find { it.hostId+"_"+it.techniqueId == _edge.startNode }
                ?: return@mapNotNull null
            return@mapNotNull hostTechnique.hostName + "_" + hostTechnique.techniqueId
        })
        return getString(Res.string.path_cost_format, nodes.joinToString(" -> "), formattedCost)
    }

    private fun generateColorFromId(id: String): Color {
        val hash = id.hashCode() * 999
        val hue = (hash.absoluteValue % 360).toFloat()
        val saturation = 0.5f + (hash.absoluteValue % 30) / 100f
        val value = 0.45f
        return Color.hsv(hue, saturation, value)
    }
}

private const val PROVIDED_ATLAS_DATA_PATH = "files/ATLAS-2026.05.yaml"
