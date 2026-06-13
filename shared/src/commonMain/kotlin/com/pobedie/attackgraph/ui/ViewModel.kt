package com.pobedie.attackgraph.ui

import androidx.compose.ui.graphics.Color
import attackgraph.shared.generated.resources.Res
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.Tactic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


class ViewModel(
    val scope: CoroutineScope,
    val mainRepository: MainRepository
) {

    private val _state = MutableStateFlow<ViewState>(ViewState())
    val state = _state.asStateFlow()

    init {
        val mockNodes = listOf(
            Node(
                name = "Node 1",
                id = "node_1",
                color = Color(255, 100, 100),
                connectedNodes = listOf("node_2", "node_3")
            ),
            Node(
                name = "Node 2",
                id = "node_2",
                color = Color(100, 255, 100),
                connectedNodes = listOf("node_1", "node_5")
            ),
            Node(
                name = "Node 3",
                id = "node_3",
                color = Color(100, 100, 255),
                connectedNodes = listOf("node_1", "node_4")
            ),
            Node(
                name = "Node 4",
                id = "node_4",
                color = Color(255, 255, 100),
                connectedNodes = listOf( "node_3")
            ),
            Node(
                name = "Node 5",
                id = "node_5",
                color = Color(255, 100, 255),
                connectedNodes = listOf("node_3")
            )
        )
        addNodes(mockNodes)

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
    }

    fun addNodes(newNodes: List<Node>) {
        _state.update {
            val _nodes = it.nodes.toMutableList()
            _nodes.addAll(newNodes)
            it.copy(
                nodes = _nodes.toList()
            )
        }
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

    fun importAtlasData(){
        _state.update { it.copy(fileError = null) }
        scope.launch {
            try {
                var fileBinary: ByteArray? = null
                if (state.value.isProvidedAtlasDateSelected) {
                    fileBinary = Res.readBytes("files/ATLAS-2026.05.yaml")
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
                selectedTechniquesId = newSelections
            )
        }
    }

}

private const val PROVIDED_ATLAS_DATA_PATH = "files/ATLAS-2026.05.yaml"