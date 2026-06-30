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
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.core.calculateProbabilitiesAndRisks
import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.EdgeState
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


class ViewModel(
    val scope: CoroutineScope,
    val mainRepository: MainRepository
) {

    private val _state = MutableStateFlow<ViewState>(ViewState())
    val state = _state.asStateFlow()

    init {
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
            val mittigationAndAttackStageAvailable: Boolean = (
                    currentState.edges.size > 2 &&
                    currentState.edges.none { it.risk == null || it.probability == null } &&
                    state.value.edges.any { it.endNode == state.value.targetTechnique }
                    )
            val isAttackVectorMappingStageAvailable =
                currentState.selectedTechniquesId.size >= 3 && state.value.targetTechnique != null
            _state.update {
                it.copy(
                    isMitigationsAndAttacksStageAvailable = mittigationAndAttackStageAvailable,
                    isAttackVectorMappingStageAvailable = isAttackVectorMappingStageAvailable
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
        val allTechniques = state.value.tactics.map { it.techniques }.flatten()
//        selectedTechniques have to be sorted, otherwise the nodes might be places incorrectly on Y axis
        val nodes: List<Node> = state.value.selectedTechniquesId.sortedBy { it }.mapNotNull { selectedIds ->
            val selectedTechnique = allTechniques.find { it.id == selectedIds }
            if (selectedTechnique != null) {
                val tacticName = state.value.tactics.findLast { it.id == selectedTechnique.tacticId }?.name.orEmpty()
                val color = generateColorFromId(selectedTechnique.tacticId)
                Node(
                    id = selectedTechnique.id,
                    name = selectedTechnique.name,
                    description = selectedTechnique.description,
                    maturity = selectedTechnique.maturity,
                    tactic = NodeTactic(
                        id = selectedTechnique.tacticId,
                        name = tacticName,
                        color = color,
                    ),
                )
            } else null
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
                val mitigations = mainRepository.getMittigations(state.value.selectedTechniquesId)
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

    fun showAlphaValueDialog() {
        _state.update {
            it.copy(
                alphaValueDialogVisible = true
            )
        }
    }

    fun switchToMitigationsAndAttacks() {
        val updatedEdges = calculateProbabilitiesAndRisks(
            edges = state.value.edges,
            nodes = state.value.nodes,
            target = state.value.targetTechnique!!
        )
        val rootNodes: List<String> =
                state.value.nodes
                        .filter { _node ->
                            updatedEdges.none { _edge -> _edge.endNode == _node.id }
                        }
                        .map { it.id }

        val targetTechnique = state.value.targetTechnique
        val allEdges = updatedEdges
        val probablePaths: MutableList<Pair<List<Edge>, Double>> = mutableListOf()
        var optimalPath: Pair<List<Edge>, Double>? = null

        if (targetTechnique != null) {
            rootNodes.forEach { _rootNode ->
                val pathResult = findOptimalPath(
                    edges = allEdges,
                    start = _rootNode,
                    target = targetTechnique,
                    alpha = state.value.alphaValue
                )
                if (pathResult != null) {
                    probablePaths.add(pathResult)
                    if (optimalPath == null || pathResult.second < optimalPath.second) {
                        optimalPath = pathResult
                    }
                }
            }
        }

        val newEdges =
            if (optimalPath != null) {
                allEdges.map { _edge ->
                    if (optimalPath.first.contains(_edge)) {
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
            if (optimalPath != null) {
                logToUiConsole(getString(Res.string.optimal_path_label), freezeDisplay = true)
                logToUiConsole(formatPath(optimalPath!!), freezeDisplay = true)

                val otherProbablePaths = probablePaths.filter { it != optimalPath }
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
        _state.update {
            val techniqueAlreadySelected = it.selectedTechniquesId.contains(techniqueId)
            val newSelections = it.selectedTechniquesId.toMutableList()
            if (techniqueAlreadySelected) {
                newSelections.remove(techniqueId)
            } else {
                newSelections.add(techniqueId)
            }
            it.copy(
                selectedTechniquesId = newSelections,
            )
        }
    }

    fun clearTechniqueSelectoins(){
        _state.update {
            it.copy(
                selectedTechniquesId = listOf(),
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

    fun changeEdgePunishment(startNode: String, endNode: String, value: Float) {
        _state.update {
            val newEdges = it.edges.map {
                if (it.startNode == startNode && it.endNode == endNode) {
                    it.copy(
                        risk = value
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

    fun setAlphaValue(alpha: Float) {
        _state.update {
            it.copy(
                alphaValue = alpha,
                alphaValueDialogVisible = false
                )
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
        nodes.add(edges.first().startNode)
        nodes.addAll(edges.map { it.endNode })
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
