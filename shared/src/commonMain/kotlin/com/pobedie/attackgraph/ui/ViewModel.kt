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
import com.pobedie.attackgraph.core.entity.UserSettings
import com.pobedie.attackgraph.core.findOptimalPath
import com.pobedie.attackgraph.network.LlmService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
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
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.getString
import java.util.Locale
import java.util.UUID
import kotlin.collections.mutableSetOf


class ViewModel(
    val scope: CoroutineScope,
    val mainRepository: MainRepository
) {

    private val _state = MutableStateFlow<ViewState>(ViewState())
    val state = _state.asStateFlow()

    private val httpClient = HttpClient {
        install(HttpTimeout) {
            connectTimeoutMillis = 20_000 // 20 seconds
            socketTimeoutMillis = 6000_000 // 60 seconds
            requestTimeoutMillis = 3000_000 // 5 minutes
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    private val llmClient = LlmService(httpClient)

    init {
        scope.launch {
            val initialHostName = getString(Res.string.host_name_format, 1)
            val savedSettings = mainRepository.getUserSettings()
            _state.update {
                it.copy(
                    hosts = listOf(
                        Host(
                            name = initialHostName,
                            id = UUID.randomUUID().toString(),
                            techniques = emptyList()
                        )
                    ),
                    llmUrl = savedSettings?.llmUrl ?: "",
                    llmApiKey = savedSettings?.llmApiKey ?: "",
                    llmModel = savedSettings?.llmModel ?: "",
                    isLlmAdded = savedSettings != null
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
            val resolvedTargets = getResolvedTargetTechniques()
            val isEdgeValueCalculationStageAvailable =
                currentState.edges.size >= 3 &&
                        resolvedTargets.isNotEmpty() &&
                        state.value.edges.any { edge -> resolvedTargets.any { target -> edge.endNode.endsWith(target) } }

            val mitigationAndAttackStageAvailable: Boolean = (
                    currentState.edges.size >= 3 &&
                            currentState.edges.none { it.probability == null } &&
                            state.value.edges.any { edge -> resolvedTargets.any { target -> edge.endNode.endsWith(target) } } &&
                            (currentState.stage == Stage.EdgeValueCalculation || currentState.stage == Stage.MitigationsAndAttacks)
                    )
            val isAttackVectorMappingStageAvailable =
                currentState.hosts.count { it.techniques.isNotEmpty() } >= 3 && 
                        resolvedTargets.isNotEmpty() &&
                        currentState.llmConnectionStatus != LlmConnectionStatus.Connecting
            _state.update {
                it.copy(
                    isMitigationsAndAttacksStageAvailable = mitigationAndAttackStageAvailable,
                    isAttackVectorMappingStageAvailable = isAttackVectorMappingStageAvailable,
                    isEdgeValueCalculationStageAvailable = isEdgeValueCalculationStageAvailable
                )
            }
        }.launchIn(scope)
    }

    fun updateLlmUrl(url: String) {
        _state.update { it.copy(llmUrl = url, llmConnectionStatus = LlmConnectionStatus.None) }
    }

    fun updateLlmApiKey(key: String) {
        _state.update { it.copy(llmApiKey = key, llmConnectionStatus = LlmConnectionStatus.None) }
    }

    fun updateLlmModel(model: String) {
        _state.update { it.copy(llmModel = model, llmConnectionStatus = LlmConnectionStatus.None) }
    }

    fun checkLlmConnection() {
        val currentState = state.value
        val settings = UserSettings(
            llmUrl = currentState.llmUrl,
            llmApiKey = currentState.llmApiKey,
            llmModel = currentState.llmModel
        )

        _state.update { it.copy(llmConnectionStatus = LlmConnectionStatus.Connecting) }

        scope.launch {
            mainRepository.saveUserSettings(settings)
            try {
                // todo: call to /v1/models to allow user to select models
                val response = httpClient.get(settings.llmUrl)
                _state.update { 
                    it.copy(
                        llmConnectionStatus = LlmConnectionStatus.Connected,
                        isLlmAdded = true
                    ) 
                }
            } catch (e: Exception) {
                _state.update { it.copy(llmConnectionStatus = LlmConnectionStatus.Failed) }
                e.printStackTrace()
            }
        }
    }

    fun findAttackVectorsViaLLM() {

        scope.launch {
            _state.update { it.copy(isGenerationInProgress = true) }
            try {
                val response = llmClient.fetchDecision(
                    url = state.value.llmUrl,
                    apiKey = state.value.llmApiKey,
                    model = state.value.llmModel,
                    techniques = state.value.nodes,
                    mitigations = state.value.mitigations,
                    attackVectors = state.value.attackVectors
                )

                val llmEdges = response.flatMapTo(mutableSetOf()) { _decision ->
                    val startHosts = state.value.hosts.mapNotNull { _host ->
                        if (_host.techniques.any { it.id == _decision.sourceId }) _host.id else null
                    }
                    val endHosts = state.value.hosts.mapNotNull { _host ->
                        if (_host.techniques.any { it.id == _decision.targetId }) _host.id else null
                    }
                    val edges: MutableList<Edge> = mutableListOf()
                    startHosts.forEach { _start ->
                        endHosts.forEach { _end ->
                            edges.add(Edge(
                                startNode = _start + "_" + _decision.sourceId,
                                endNode = _end + "_" + _decision.targetId,
                                llmConfidence = _decision.confidence
                            )
                            )
                        }
                    }
                    return@flatMapTo edges.toSet()
                }
                llmEdges.addAll(state.value.edges)
                val newEdges = calculateProbabilitiesSimple(llmEdges.toList(), state.value.nodes)

                _state.update {
                    it.copy(
                        edges = newEdges
                    )
                }
            } catch (e: Throwable) {
                _state.update { it.copy(llmConnectionStatus = LlmConnectionStatus.Failed) }
                e.printStackTrace()
            }
            _state.update { it.copy(isGenerationInProgress = false) }
        }
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
            fetchMitigations()
        }
    }

    fun switchToAttackVectorBuildingStage() {
        clearConsole()
        val currentState = state.value
        val nodes: List<Node> = currentState.hosts.flatMap { host ->
            host.techniques.mapNotNull { technique ->
                val tactic = currentState.tactics.findLast { it.id == technique.tacticId } ?: return@mapNotNull null
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
                        name = tactic.name,
                        color = color,
                        position = tactic.position
                    ),
                )
            }
        }

        val resolvedTargets = getResolvedTargetTechniques()
        val selectedTechniqueIds = currentState.hosts.flatMap { host -> host.techniques.map { it.id } }.distinct()

        // Fetch case studies for all selected techniques to build possible paths
        scope.launch {
            val attackVectors = if (selectedTechniqueIds.isNotEmpty()) {
                mainRepository.getAttackVectors(selectedTechniqueIds)
            } else emptyList()

            val autoEdges = mutableListOf<Edge>()
            val avByCaseStudy = attackVectors.groupBy { it.caseStudyId }

            for ((_, steps) in avByCaseStudy) {
                val sortedSteps = steps.sortedBy { it.step }
                for (j in sortedSteps.indices) {
                    val currentStep = sortedSteps[j]

                    // Option 1: Explicit leadsTo
                    val targetStepIds = currentStep.leadsToStep
                    val targets = if (targetStepIds.isNotEmpty()) {
                        steps.filter { it.stepId in targetStepIds }
                    } else if (j + 1 < sortedSteps.size) {
                        // Option 2: Next step in sequence (fallback)
                        listOf(sortedSteps[j + 1])
                    } else emptyList()

                    for (targetStep in targets) {
                        val sourceTechId = currentStep.targetTechnique
                        val targetTechId = targetStep.targetTechnique

                        val sourceNodes = nodes.filter { it.techniqueId == sourceTechId }
                        val targetNodes = nodes.filter { it.techniqueId == targetTechId }

                        for (u in sourceNodes) {
                            for (v in targetNodes) {
                                if (u.id == v.id) continue

                                val hasRelevantMitigation = currentState.mitigations.any {
                                    it.targetTechnique == v.techniqueId && it.isRelevant
                                }

                                if (!hasRelevantMitigation) {
                                    autoEdges.add(
                                        Edge(
                                            startNode = u.id,
                                            endNode = v.id,
                                            state = EdgeState.CaseStudyProven
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Keep valid existing edges
            val validExistingEdges = currentState.edges.filter { edge ->
                nodes.any { it.id == edge.startNode } && nodes.any { it.id == edge.endNode }
            }
            val allEdges = (validExistingEdges + autoEdges).distinctBy { it.startNode to it.endNode }
            val calculatedEdges = calculateProbabilitiesSimple(allEdges, nodes)

            _state.update {
                it.copy(
                    stage = Stage.AttackVectorsBuilding,
                    nodes = nodes,
                    edges = calculatedEdges,
                    selectedNode = null,
                    targetTechniques = resolvedTargets,
                    attackVectors = attackVectors
                )
            }

            if (resolvedTargets.isEmpty()) {
                logToUiConsole(getString(Res.string.target_not_selected_error))
            }
        }
    }

    fun setTargetGoal(goal: TargetGoal) {
        _state.update {
            it.copy(
                targetGoal = goal,
                targetTechniques = emptyList(),
                isTargetSelectionInProgress = goal == TargetGoal.Specific
            )
        }
    }

    fun selectTargetTechnique(target: String) {
        _state.update {
            it.copy(
                targetTechniques = listOf(target),
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

        val targetTechniques = getResolvedTargetTechniques()
        val allEdges = state.value.edges
        val probablePaths: MutableList<Pair<List<Edge>, Double>> = mutableListOf()
        var optimalPath: Pair<List<Edge>, Double>? = null

        if (targetTechniques.isNotEmpty()) {
            val targetNodes = state.value.nodes.filter { it.techniqueId in targetTechniques }.map { it.id }
            rootNodes.forEach { _rootNode ->
                val pathResult = findOptimalPath(
                    edges = allEdges,
                    start = _rootNode,
                    targets = targetNodes
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
                targetTechniques = emptyList(),
                targetGoal = TargetGoal.HighestSeverity,
                isTargetSelectionInProgress = false,
                isAttackVectorMappingStageAvailable = false,
                mitigations = emptyList()
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

    private fun fetchMitigations() {
        scope.launch {
            val mitigations = mainRepository.getAllMitigations()
            _state.update { it.copy(mitigations = mitigations) }
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

    private fun getResolvedTargetTechniques(): List<String> {
        val currentState = state.value
        return when (currentState.targetGoal) {
            TargetGoal.Specific -> currentState.targetTechniques
            TargetGoal.HighestSeverity -> {
                val allTechniques = currentState.hosts.flatMap { it.techniques }
                if (allTechniques.isEmpty()) return emptyList()

                val tactics = currentState.tactics
                val maxPos = allTechniques.maxOfOrNull { tech ->
                    tactics.find { it.id == tech.tacticId }?.position ?: 0
                } ?: 0

                val techsInMaxTactic = allTechniques.filter { tech ->
                    (tactics.find { it.id == tech.tacticId }?.position ?: 0) == maxPos
                }

                val maxSeverity = techsInMaxTactic.maxOfOrNull { it.severityScore } ?: 0

                techsInMaxTactic.filter { it.severityScore == maxSeverity }
                    .map { it.id }
                    .distinct()
            }
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
