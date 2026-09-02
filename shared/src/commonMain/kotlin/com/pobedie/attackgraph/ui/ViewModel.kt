package com.pobedie.attackgraph.ui

import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.dashed_edges_firewall_hint
import attackgraph.shared.generated.resources.file_blank_error
import attackgraph.shared.generated.resources.file_not_found_error
import attackgraph.shared.generated.resources.firewall_host_rule_exists_error
import attackgraph.shared.generated.resources.no_optimal_path_found
import attackgraph.shared.generated.resources.optimal_path_label
import attackgraph.shared.generated.resources.probable_paths_label
import attackgraph.shared.generated.resources.target_not_selected_error
import attackgraph.shared.generated.resources.unexpected_error
import attackgraph.shared.generated.resources.host_name_format
import com.pobedie.attackgraph.core.calculateProbabilitiesSimple
import com.pobedie.attackgraph.core.MainRepository
import com.pobedie.attackgraph.core.AttackVectorService
import com.pobedie.attackgraph.core.PathfinderService
import com.pobedie.attackgraph.core.entity.Edge
import com.pobedie.attackgraph.core.entity.EdgeState
import com.pobedie.attackgraph.core.entity.FirewallRule
import com.pobedie.attackgraph.core.entity.Host
import com.pobedie.attackgraph.core.entity.UserSettings
import com.pobedie.attackgraph.core.isEdgeAllowed
import com.pobedie.attackgraph.network.LlmService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
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
import java.io.File
import kotlinx.serialization.json.Json

class ViewModel(
    val scope: CoroutineScope,
    val mainRepository: MainRepository,
    private val attackVectorService: AttackVectorService,
    private val pathfinderService: PathfinderService,
    private val llmService: LlmService
) {

    companion object {
        fun create(
            scope: CoroutineScope,
            mainRepository: MainRepository
        ): ViewModel {
            val httpClient = HttpClient {
                install(HttpTimeout) {
                    connectTimeoutMillis = 20_000
                    socketTimeoutMillis = 6000_000
                    requestTimeoutMillis = 3000_000
                }
                install(ContentNegotiation) {
                    json(Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    })
                }
            }
            return ViewModel(
                scope = scope,
                mainRepository = mainRepository,
                attackVectorService = AttackVectorService(),
                pathfinderService = PathfinderService(),
                llmService = LlmService(httpClient)
            )
        }
    }

    private val _state = MutableStateFlow<ViewState>(ViewState())
    val state = _state.asStateFlow()

    init {
        loadInitialState()
        observeImportState()
        observeStateChanges()
    }

    private fun loadInitialState() {
        scope.launch {
            val initialHostName = getString(Res.string.host_name_format, 1)
            val savedSettings = mainRepository.getUserSettings()
            _state.update {
                it.copy(
                    hosts = listOf(
                        Host(
                            name = initialHostName,
                            id = UUID.randomUUID().toString(),
                            techniquesIds = emptyList()
                        )
                    ),
                    llmUrl = savedSettings?.llmUrl ?: "",
                    llmApiKey = savedSettings?.llmApiKey ?: "",
                    llmModel = savedSettings?.llmModel ?: "",
                    isLlmAdded = savedSettings != null
                )
            }
        }
    }

    private fun observeImportState() {
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
    }

    private fun observeStateChanges() {
        state.onEach { currentState ->
            val resolvedTargets = getResolvedTargetTechniques()
            val possibleAttackVectorsStageAvailable = (
                    currentState.edges.size >= 3 &&
                            currentState.edges.none { it.probability == null } &&
                            currentState.edges.any { edge -> 
                                resolvedTargets.any { target -> edge.endNode == "${target.second}_${target.first}" } 
                            }
                    )
            val isAttackVectorMappingStageAvailable =
                        resolvedTargets.isNotEmpty() &&
                        currentState.llmConnectionStatus != LlmConnectionStatus.Connecting
            val isFirewallMappingStageAvailable = currentState.hosts.count { it.techniquesIds.isNotEmpty() } >= 2

            if (currentState.rootNodeGoal == RootNodeGoal.Automatic) {
                val autoRootPairs = currentState.nodes
                    .filter { node ->
                        currentState.edges.none { it.endNode == node.id } &&
                                currentState.edges.any { it.startNode == node.id }
                    }
                    .map { it.techniqueId to it.hostId }

                if (autoRootPairs != currentState.rootTechniques) {
                    _state.update { it.copy(rootTechniques = autoRootPairs) }
                }
            }

            _state.update {
                it.copy(
                    isPossibleAttackVectorsStageAvailable = possibleAttackVectorsStageAvailable,
                    isAttackVectorMappingStageAvailable = isAttackVectorMappingStageAvailable,
                    isFirewallMappingStageAvailable = isFirewallMappingStageAvailable
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
        _state.update { it.copy(llmModel = model) }
    }

    fun fetchLlmModels() {
        val currentState = state.value
        if (currentState.llmUrl.isBlank()) return

        _state.update { it.copy(isLlmModelsLoading = true) }

        scope.launch {
            try {
                val models = llmService.fetchModels(currentState.llmUrl, currentState.llmApiKey)
                _state.update {
                    it.copy(
                        availableLlmModels = models,
                        isLlmModelsLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLlmModelsLoading = false) }
                e.printStackTrace()
            }
        }
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
                // For connection check, we still use the injected service's client or similar logic
                // Here we might need a way to just check connection if llmService doesn't expose it
                // For now, I'll keep the direct httpClient call if it's just a health check
                // but usually llmService should handle it.
                // Assuming llmService has a way to check connection or we just use fetchModels as a check.
                llmService.fetchModels(settings.llmUrl, settings.llmApiKey)
                
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
                val currentState = state.value
                val decisions = llmService.fetchDecision(
                    url = currentState.llmUrl,
                    apiKey = currentState.llmApiKey,
                    model = currentState.llmModel,
                    techniques = currentState.nodes,
                    mitigations = currentState.mitigations,
                    attackVectors = currentState.attackVectors
                )

                val newEdges = attackVectorService.processLlmDecisions(
                    decisions = decisions,
                    hosts = currentState.hosts,
                    existingEdges = currentState.edges,
                    nodes = currentState.nodes
                )
                
                val updatedEdges = calculateProbabilitiesSimple(newEdges, currentState.nodes)

                _state.update {
                    it.copy(
                        edges = updatedEdges
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
        scope.launch {
            val (techniques, tactics) = mainRepository.getTacticsWithTechniques()
            _state.update {
                it.copy(
                    stage = Stage.TechniqueSelection,
                    techniques = techniques,
                    tactics = tactics
                )
            }
            fetchMitigations()
        }
    }

    fun switchToFirewallMappingStage() {
        clearConsole()
        _state.update {
            it.copy(
                stage = Stage.FirewallMapping,
                selectedNode = null,
                selectedEdge = null
            )
        }
    }

    fun switchToAttackVectorBuildingStage() {
        clearConsole()
        val currentState = state.value
        
        val nodes = attackVectorService.buildNodes(
            hosts = currentState.hosts,
            tactics = currentState.tactics,
            allTechniques = currentState.techniques
        )

        val resolvedTargets = getResolvedTargetTechniques()
        val selectedTechniqueIds = currentState.hosts.flatMap { it.techniquesIds }.distinct()

        scope.launch {
            val attackVectors = if (selectedTechniqueIds.isNotEmpty()) {
                mainRepository.getAttackVectors(selectedTechniqueIds)
            } else emptyList()

            val autoEdges = attackVectorService.buildEdgesFromCaseStudies(
                nodes = nodes,
                attackVectors = attackVectors,
                mitigations = currentState.mitigations,
                firewallRules = currentState.firewallRules
            )

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
            if (calculatedEdges.any { it.state == EdgeState.Blocked }) {
                logToUiConsole(getString(Res.string.dashed_edges_firewall_hint))
            }
        }
    }

    fun setTargetGoal(goal: TargetGoal) {
        _state.update {
            val wasAlreadySpecific = it.targetGoal == TargetGoal.Specific && goal == TargetGoal.Specific
            it.copy(
                targetGoal = goal,
                targetTechniques = if (it.targetGoal != goal) emptyList() else it.targetTechniques,
                isTargetSelectionInProgress = if (wasAlreadySpecific) !it.isTargetSelectionInProgress else goal == TargetGoal.Specific,
                isRootSelectionInProgress = false
            )
        }
    }

    fun selectTargetTechnique(techniqueId: String) {
        _state.update { state ->
            val hostId = state.hosts[state.currentHostIndex].id
            val targetPair = techniqueId to hostId
            val isAdding = !state.targetTechniques.contains(targetPair)
            val newTargets = if (isAdding) {
                state.targetTechniques + targetPair
            } else {
                state.targetTechniques.filter { it != targetPair }
            }
            
            var newState = state.copy(
                targetTechniques = newTargets,
                isTargetSelectionInProgress = true
            )
            
            if (isAdding) {
                newState = newState.addTechniqueIfMissing( hostId, techniqueId)
            }
            newState
        }
    }

    fun setRootNodeGoal(goal: RootNodeGoal) {
        _state.update {
            val wasAlreadyManual = it.rootNodeGoal == RootNodeGoal.Manual && goal == RootNodeGoal.Manual
            it.copy(
                rootNodeGoal = goal,
                rootTechniques = if (it.rootNodeGoal != goal) emptyList() else it.rootTechniques,
                isRootSelectionInProgress = if (wasAlreadyManual) !it.isRootSelectionInProgress else goal == RootNodeGoal.Manual,
                isTargetSelectionInProgress = false
            )
        }
    }

    fun selectRootTechnique(techniqueId: String) {
        _state.update { state ->
            val hostId = state.hosts[state.currentHostIndex].id
            val rootPair = techniqueId to hostId
            val isAdding = !state.rootTechniques.contains(rootPair)
            val newRootTechniques = if (isAdding) {
                state.rootTechniques + rootPair
            } else {
                state.rootTechniques.filter { it != rootPair }
            }
            
            var newState = state.copy(
                rootTechniques = newRootTechniques,
                isRootSelectionInProgress = true
            )
            
            if (isAdding) {
                newState = newState.addTechniqueIfMissing(hostId,techniqueId)
            }
            newState
        }
    }

    private fun ViewState.addTechniqueIfMissing(hostId: String, techniqueId: String): ViewState {
        if (selectedTechniquesIds.contains(techniqueId)) return this
        return this.copy(
            hosts = hosts.map { host ->
                if (host.id == hostId) {
                    host.copy(
                        techniquesIds = host.techniquesIds + techniqueId
                    )
                } else host
            }
        )
    }

    fun cancelSelectionMode() {
        _state.update {
            it.copy(
                isTargetSelectionInProgress = false,
                isRootSelectionInProgress = false
            )
        }
    }

    fun switchToPossibleAttackVectors() {
        val currentState = state.value
        val result = pathfinderService.calculatePaths(
            nodes = currentState.nodes,
            edges = currentState.edges,
            rootTechniques = currentState.rootTechniques,
            targetTechniques = getResolvedTargetTechniques()
        )

        _state.update {
            it.copy(
                stage = Stage.PossibleAttackVectors,
                edges = result.edges
            )
        }
        clearConsole()
        scope.launch {
            if (result.optimalPaths.isNotEmpty()) {
                logToUiConsole(getString(Res.string.optimal_path_label), freezeDisplay = true)
                result.optimalPaths.forEach {
                    logToUiConsole(formatPath(it, currentState.nodes), freezeDisplay = true)
                }
                val otherProbablePaths = result.probablePaths.take(4)
                if (otherProbablePaths.isNotEmpty()) {
                    logToUiConsole("\n" + getString(Res.string.probable_paths_label), freezeDisplay = true)
                    otherProbablePaths.forEach {
                        logToUiConsole(formatPath(it, currentState.nodes), freezeDisplay = true)
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

    fun toggleTechnique(selectedTechniqueId: String) {
        _state.update { state ->
            val isTechniqueAlreadySelected = state.hosts[state.currentHostIndex].techniquesIds.contains(selectedTechniqueId)

            state.copy(
                hosts = state.hosts.mapIndexed { index, host ->
                    if (index == state.currentHostIndex) {
                        if (isTechniqueAlreadySelected) {
                            host.copy(
                                techniquesIds = host.techniquesIds.filter { it != selectedTechniqueId }
                            )
                        } else {
                            host.copy(
                                techniquesIds = host.techniquesIds + selectedTechniqueId
                            )
                        }
                    } else host
                },
            ).let { newState ->
                if (isTechniqueAlreadySelected) {
                    val currentHostId = newState.hosts[newState.currentHostIndex].id
                    newState.copy(
                        rootTechniques = newState.rootTechniques.filter { it != (selectedTechniqueId to currentHostId) },
                        targetTechniques = newState.targetTechniques.filter { it != (selectedTechniqueId to currentHostId) }
                    )
                } else newState
            }
        }
    }

    fun nextHost() {
        _state.update {
            if (it.currentHostIndex < it.hosts.size - 1) {
                val nextIndex = it.currentHostIndex + 1
                it.copy(
                    currentHostIndex = nextIndex,
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
            val newTechniques = state.techniques.map { tech ->
                if (tech.id == techniqueId) tech.copy(severityScore = score) else tech
            }
            state.copy(techniques = newTechniques)
        }
    }

    fun removeTechniqueFromHost(hostId: String, techniqueId: String) {
        _state.update { state ->
            val newHosts = state.hosts.map { host ->
                if (host.id == hostId) {
                    val newTechniquesIds = host.techniquesIds.filter { it != techniqueId }
                    host.copy(techniquesIds = newTechniquesIds)
                } else host
            }
            
            state.copy(
                hosts = newHosts,
                rootTechniques = state.rootTechniques.filter { it != (techniqueId to hostId) },
                targetTechniques = state.targetTechniques.filter { it != (techniqueId to hostId) }
            )
        }
    }

    fun clearTechniqueSelections(){
        _state.update {
            it.copy(
                hosts = it.hosts.map { host -> host.copy(techniquesIds = emptyList()) },
                targetTechniques = emptyList(),
                rootTechniques = emptyList(),
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
                val edge =
                    Edge(
                        startNode = state.selectedNode,
                        endNode = selectedNode,
                        )
                val edgeState = if (isEdgeAllowed(edge, state.firewallRules, state.nodes)) {
                    EdgeState.Idle
                } else {
                    EdgeState.Blocked
                }
                newEdges.add(
                    edge.copy(state = edgeState)
                )
                val updatedEdges = calculateProbabilitiesSimple(newEdges, state.nodes)
                state.copy(
                    edges = updatedEdges,
                    selectedNode = null
                )
            }
        }
    }

    fun setFirewallConnection(
        sourceHostId: String,
        sourceTechniqueId: String?,
        targetHostId: String
    ) {

        val isHostSource = sourceTechniqueId == null
        val newRule = FirewallRule(sourceHostId, sourceTechniqueId, targetHostId)

        if (!isHostSource) {
            val hostRuleExists = state.value.firewallRules.any {
                it.sourceHostId == sourceHostId && it.sourceTechniqueId == null && it.targetHostId == targetHostId
            }
            if (hostRuleExists) {
                val sourceHostName = state.value.hosts.find { it.id == sourceHostId }?.name ?: sourceHostId
                val targetHostName = state.value.hosts.find { it.id == targetHostId }?.name ?: targetHostId
                scope.launch {
                    logToUiConsole(getString(Res.string.firewall_host_rule_exists_error, sourceHostName, targetHostName))
                }
                return
            }
        }

        _state.update { state ->
            val existingRule = state.firewallRules.find { it == newRule }
            val newRules = if (existingRule != null) {
                state.firewallRules.filter { it != existingRule }
            } else {
                val baseRules = state.firewallRules + newRule
                if (isHostSource) {
                    baseRules.filter { rule ->
                        !(rule.sourceHostId == sourceHostId && rule.sourceTechniqueId != null && rule.targetHostId == targetHostId)
                    }
                } else {
                    baseRules
                }
            }
            state.copy(firewallRules = newRules)
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

    fun selectSourceNode(id: String) {
        _state.update { it.copy(selectedNode = id) }
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

    fun toggleTheme() {
        _state.update {
            it.copy(isDarkMode = !it.isDarkMode)
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

    private fun getResolvedTargetTechniques(): List<Pair<String, String>> {
        val currentState = state.value
        return when (currentState.targetGoal) {
            TargetGoal.Specific -> currentState.targetTechniques
            TargetGoal.HighestSeverity -> {
                val allTechniquesWithHost = currentState.hosts.flatMap { host ->
                    host.techniquesIds.mapNotNull { techId ->
                        val tech = currentState.techniques.find { it.id == techId } ?: return@mapNotNull null
                        tech to host.id
                    }
                }
                if (allTechniquesWithHost.isEmpty()) return emptyList()

                val tactics = currentState.tactics
                val maxPos = allTechniquesWithHost.maxOfOrNull { (tech, _) ->
                    tactics.find { it.id == tech.tacticId }?.position ?: 0
                } ?: 0

                val techsInMaxTactic = allTechniquesWithHost.filter { (tech, _) ->
                    (tactics.find { it.id == tech.tacticId }?.position ?: 0) == maxPos
                }

                val maxSeverity = techsInMaxTactic.maxOfOrNull { it.first.severityScore } ?: 0

                techsInMaxTactic.filter { it.first.severityScore == maxSeverity }
                    .map { it.first.id to it.second }
                    .distinct()
            }
        }
    }

}

private const val PROVIDED_ATLAS_DATA_PATH = "files/ATLAS-2026.05.yaml"
