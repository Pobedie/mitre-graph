package com.pobedie.attackgraph.ui

import androidx.compose.ui.graphics.Color
import attackgraph.shared.generated.resources.Res
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.NodeTactic
import com.pobedie.attackgraph.core.entity.Tactic
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
import java.io.File
import kotlin.math.absoluteValue


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
                        it.copy( isTechniqueSelectionStageAvailable = true )
                    }
                    switchToTechniqueSelectionStage()
                }
            }
        }
        // Handle side effects from state change
        state.onEach { currentState ->
            val mittigationAndAttackStageAvailable: Boolean =
                (currentState.edges.size > 2 && currentState.edges.none { it.punishment == null || it.probability == null })
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
        _state.update { it.copy(stage = Stage.Import) }
    }

    fun switchToTechniqueSelectionStage() {
        var tactics: List<Tactic> = emptyList()
        scope.launch {
            tactics = mainRepository.getTactics()
            _state.update {
                it.copy(
                    stage = Stage.TechniqueSelection,
                    tactics = tactics
                )
            }
        }
    }


    fun switchToAttackVectorBuildingStage() {
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
                    tactic = NodeTactic(
                        id = selectedTechnique.tacticId,
                        name = tacticName,
                        color = color
                    )
                )
            } else null
        }
        _state.update {
            it.copy(
                stage = Stage.AttackVectorsBuilding,
                nodes = nodes,
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
            println("ERROR: target technique is not selected")
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

    fun switchToMittigationsAndAttacks() {
        _state.update {
            it.copy(
                stage = Stage.MitigationsAndAttacks,
            )
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
                        _state.update { it.copy(fileError = "File not found at ${state.value.filePath}") }
                        println("Error: File not found at ${state.value.filePath}")
                    }
                }

                if (fileBinary != null) {
                    val fileContent = fileBinary.decodeToString()
                    if (fileContent.isNotBlank()) {
                        mainRepository.importMitreAtlasData(fileContent)
                    } else {
                        _state.update { it.copy(fileError = "File ${state.value.filePath} seems to be blank") }
                        println("Error: File ${state.value.filePath} seems to be blank")
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(fileError = "Unexpected error: ${e.localizedMessage}") }
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
                        punishment = value
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


    private fun generateColorFromId(id: String): Color {
        val hash = id.hashCode() * 999
        val hue = (hash.absoluteValue % 360).toFloat()
        val saturation = 0.5f + (hash.absoluteValue % 30) / 100f
        val value = 0.45f
        return Color.hsv(hue, saturation, value)
    }
}

private const val PROVIDED_ATLAS_DATA_PATH = "files/ATLAS-2026.05.yaml"