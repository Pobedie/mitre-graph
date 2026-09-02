package com.pobedie.attackgraph.ui.stages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.width
import androidx.compose.ui.zIndex
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.description_maturity_severity_format
import attackgraph.shared.generated.resources.deselect_hint
import attackgraph.shared.generated.resources.edge_probability_risk_format
import attackgraph.shared.generated.resources.ic_info
import attackgraph.shared.generated.resources.ic_shield
import attackgraph.shared.generated.resources.maturity_demonstrated
import attackgraph.shared.generated.resources.maturity_feasible
import attackgraph.shared.generated.resources.maturity_realized
import attackgraph.shared.generated.resources.maturity_unknown
import attackgraph.shared.generated.resources.mitigation_full_description_format
import attackgraph.shared.generated.resources.p_label
import attackgraph.shared.generated.resources.set_as_irrelevant
import attackgraph.shared.generated.resources.set_as_relevant
import attackgraph.shared.generated.resources.show_mitigation_info_content_desc
import attackgraph.shared.generated.resources.technique_description_content_desc
import attackgraph.shared.generated.resources.unknown_value
import com.pobedie.attackgraph.ui.theme.*
import com.dk.kuiver.model.KuiverNode
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.buildKuiverWithClassifiedEdges
import com.dk.kuiver.model.edges
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.layout.LayoutProvider
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.renderer.KuiverViewerConfig
import com.dk.kuiver.ui.EdgeContentWithLabel
import com.dk.kuiver.ui.LabelPlacement
import com.pobedie.attackgraph.core.entity.EdgeState
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.TechniqueMaturity
import com.pobedie.attackgraph.ui.LlmConnectionStatus
import com.pobedie.attackgraph.ui.Stage
import com.pobedie.attackgraph.ui.ViewModel
import com.pobedie.attackgraph.ui.ViewState
import com.pobedie.attackgraph.ui.components.DeleteButton
import com.pobedie.attackgraph.ui.components.FloatInputField
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AttackGraph(
    viewModel: ViewModel,
    state: ViewState
) {
    val tactics = remember(state.nodes) {
        state.nodes.map { it.tactic }.toSet().sortedBy { it.position }
    }

    val tacticToHosts = remember(state.nodes, tactics) {
        tactics.associate { tactic ->
            tactic.id to state.nodes
                .filter { it.tactic.id == tactic.id }
                .map { it.hostId }
                .distinct()
        }
    }

    val edges = remember(state.edges) {
        state.edges.map { _edge ->
            _edge.startNode to _edge.endNode
        }
    }

    // Create graph structure
    val kuiver = remember(edges, state.nodes) {
        buildKuiver {
            nodes(ids = state.nodes.map { it.id })
            edges(*edges.toTypedArray())
        }
    }

    var hostContainers by remember { mutableStateOf<List<HostContainerData>>(emptyList()) }

    val tacticLayout: LayoutProvider = remember(state.nodes, tacticToHosts) {
        { kuiver, _ ->
            val updatedNodes = mutableListOf<KuiverNode>()
            val newHostContainers = mutableListOf<HostContainerData>()

            tactics.forEachIndexed { tacticIndex, tactic ->
                val x = COLUMN_X_SPACING * tacticIndex
                var currentY = INITIAL_Y_PADDING

                tacticToHosts[tactic.id]?.forEach { hostId ->
                    val hostNodes = state.nodes.filter { it.tactic.id == tactic.id && it.hostId == hostId }
                    val hostName = hostNodes.first().hostName

                    val hostStartY = currentY
                    currentY += HOST_TITLE_HEIGHT

                    hostNodes.forEach { nodeData ->
                        val libNode = kuiver.nodes[nodeData.id] ?: return@forEach
                        updatedNodes.add(
                            libNode.copy(
                                position = DpOffset(x + NODE_X_OFFSET, currentY)
                            )
                        )
                        currentY += (libNode.dimensions?.height ?: DEFAULT_NODE_HEIGHT) + NODE_Y_SPACING
                    }

                    newHostContainers.add(
                        HostContainerData(
                            name = hostName,
                            rect = DpRect(
                                left = x,
                                top = hostStartY,
                                right = x + NODE_WIDTH + HOST_CONTAINER_WIDTH_PADDING,
                                bottom = currentY - HOST_CONTAINER_BOTTOM_PADDING
                            )
                        )
                    )
                    currentY += HOST_SPACING
                }
            }
            hostContainers = newHostContainers
            buildKuiverWithClassifiedEdges(updatedNodes, kuiver.edges)
        }
    }

    // Configure layout
    val layoutConfig = remember(tacticLayout) {
        LayoutConfig.Custom(
            provider = tacticLayout
        )
    }
    val viewerState = rememberKuiverViewerState(
        initialKuiver = kuiver,
        layoutConfig = layoutConfig
    )

    LaunchedEffect(kuiver) {
        viewerState.updateKuiver(kuiver)
    }

    var zoomDelta by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(zoomDelta) {
        if (zoomDelta > 1f) {
            viewerState.zoomIn()
        } else if (zoomDelta < -1f) {
            viewerState.zoomOut()
        }
        delay(2.seconds)
        zoomDelta = 0f
    }
    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        if (event.type == PointerEventType.Release) {
                            val wasConsumed = event.changes.any { it.isConsumed }
                            if (!wasConsumed) {
                                viewModel.clearEdgeSelection()
                                viewModel.clearNodeSelection()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        tactics.forEachIndexed { index, tactic ->
            val vsOffset = viewerState.offset.x
            val vsScale = viewerState.scale
            // shifting coords from center to the first column
            val coordsOffset = COLUMN_X_SPACING * (tactics.size / 2f) - COLUMN_X_SPACING / 2f
            val columnXOffset = vsOffset + (COLUMN_X_SPACING * index - coordsOffset) * vsScale
            TacticColumn(
                xOffset = columnXOffset,
                width = COLUMN_X_WIDTH * vsScale,
                tacticName = tactic.name,
                color = tactic.color
            )
        }

        // Render the host containers behind the graph
        val hostContainerBackground = HostContainerBackground
        val hostContainerBorder = HostContainerBorder
        val primaryTextColor = PrimaryTextColor
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .drawBehind {
                    val vsScale = viewerState.scale
                    val vsOffset = viewerState.offset

                    val minNodeY = (hostContainers.minOfOrNull { it.rect.top } ?: 0.dp) + HOST_TITLE_HEIGHT
                    val maxNodeY = (hostContainers.maxOfOrNull { it.rect.bottom } ?: 0.dp) + HOST_CONTAINER_BOTTOM_PADDING - NODE_Y_SPACING
                    val graphCenterY = (minNodeY + maxNodeY) / 2f

                    val coordsOffsetX = COLUMN_X_SPACING * (tactics.size / 2f) - COLUMN_X_SPACING / 2f
                    val graphCenterX = coordsOffsetX + (NODE_X_OFFSET + NODE_WIDTH / 2f)

                    hostContainers.forEach { container ->
                        val nodeOffsetX = (container.rect.left - graphCenterX) * vsScale
                        val nodeOffsetY = (container.rect.top - graphCenterY) * vsScale
                        val translatedRect = Rect(
                            offset = Offset(
                                center.x + (vsOffset.x + nodeOffsetX).toPx(),
                                center.y + (vsOffset.y + nodeOffsetY).toPx()
                            ),
                            size = Size(
                                width = (container.rect.width * vsScale).toPx(),
                                height = (container.rect.height * vsScale).toPx()
                            )
                        )

                        drawRoundRect(
                            color = hostContainerBackground,
                            topLeft = translatedRect.topLeft,
                            size = translatedRect.size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius((8.dp * vsScale).toPx())
                        )
                        drawRoundRect(
                            color = hostContainerBorder,
                            topLeft = translatedRect.topLeft,
                            size = translatedRect.size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius((8.dp * vsScale).toPx()),
                            style = Stroke(width = (1.dp * vsScale).toPx())
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            size = Size(
                                height = (HOST_TITLE_HEIGHT * vsScale).toPx(),
                                width = (container.rect.width * vsScale).toPx()
                            ),
                            text = container.name,
                            topLeft = translatedRect.topLeft + Offset((8.dp * vsScale).toPx(), (4.dp * vsScale).toPx()),
                            style = TextStyle(
                                color = primaryTextColor,
                                fontSize = 12.sp * vsScale,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
        )

        // Render the graph
        KuiverViewer(
            state = viewerState,
            config = KuiverViewerConfig(
                layoutAnimationSpec = snap(),
                zoomConditionDesktop = { true }
            ),
            nodeContent = { libNode ->
                val node = state.nodes.findLast { it.id == libNode.id } ?: return@KuiverViewer
                TechniqueNode(
                    modifier = Modifier.width(NODE_WIDTH),
                    node = node,
                    isSelected = node.id == state.selectedNode,
                    isTarget = state.targetTechniques.any { it.first == node.techniqueId && it.second == node.hostId },
                    isRoot = state.rootTechniques.any { it.first == node.techniqueId && it.second == node.hostId },
                    isEnabled = state.stage == Stage.AttackVectorsBuilding && !state.isGenerationInProgress,
                    onClick = {
                        viewModel.setNodeConnection(node.id)
                    },
                    areMitigationsShown = state.stage == Stage.PossibleAttackVectors,
                    mitigations = state.mitigations.filter { it.targetTechnique == node.techniqueId },
                    onToggleMitigationRelevance = {
                        viewModel.toggleMitigationRelevance(it)
                    }
                )
            },
            edgeContent = { libEdge, from, to ->
                // Customize edge appearance
                val _edge = state.edges.find { it.startNode == libEdge.fromId && it.endNode == libEdge.toId }
                val (edgeColor, edgeWidth) =
                    when {
                        _edge?.state == EdgeState.Blocked -> Pair(ErrorColor.copy(alpha = 0.5f), 1.dp)
                        state.stage == Stage.AttackVectorsBuilding ||
                                state.stage == Stage.EdgeValueCalculation ||
                                _edge == null -> Pair(EdgeDefault, 2.dp)
                        _edge.state == EdgeState.MostOptimal -> Pair(EdgeOptimal, 4.dp)
                        _edge.state == EdgeState.Probable -> Pair(EdgeProbable, 3.dp)
                        else -> Pair(EdgeDefault, 2.dp)
                    }
                val isSelected = state.selectedEdge?.let {
                    _edge != null && _edge.startNode == it.first && _edge.endNode == it.second
                } ?: false
                val isDisallowed = _edge?.state == EdgeState.Blocked
                Box(
                    modifier = Modifier.zIndex(if (isSelected) 1000f else 0f)
                ) {
                    EdgeContentWithLabel(
                        from,
                        to,
                        color = edgeColor,
                        strokeWidth = edgeWidth,
                        enableCurve = true,
                        dashed = isDisallowed,
                        labelPlacement = LabelPlacement.END,
                        label = "label",
                        labelContent = { _ ->
                            if (_edge != null) {
                                if (isDisallowed) {
                                    DeleteButton(
                                        onClick = {
                                            viewModel.deleteEdge(
                                                _edge.startNode,
                                                _edge.endNode
                                            )
                                        },
                                        tint = Color.Black
                                    )
                                } else {
                                    TechniqueEdge(
                                        probability = _edge.probability,
                                        isSelected = isSelected,
                                        isEnabled = (state.stage == Stage.AttackVectorsBuilding ||
                                                state.stage == Stage.EdgeValueCalculation) &&
                                                !state.isGenerationInProgress,
                                        onClick = {
                                            viewModel.selectEdge(
                                                _edge.startNode,
                                                _edge.endNode
                                            )
                                        },
                                        onDismissed = { viewModel.clearEdgeSelection() },
                                        onDelete = {
                                            viewModel.deleteEdge(
                                                _edge.startNode,
                                                _edge.endNode
                                            )
                                        },
                                        onProbabilityChange = {
                                            viewModel.changeEdgeProbability(
                                                _edge.startNode,
                                                _edge.endNode,
                                                it
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    )
                }
            }
        )

        Column(
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            AnimatedVisibility(
                visible = state.stage == Stage.AttackVectorsBuilding && state.llmConnectionStatus == LlmConnectionStatus.Connected,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    enabled = state.llmConnectionStatus == LlmConnectionStatus.Connected && !state.isGenerationInProgress,
                    modifier = Modifier.padding(horizontal = 4.dp),
                    onClick = { viewModel.findAttackVectorsViaLLM() }

                ) {
                    if (state.isGenerationInProgress) {
                        Text("Generating vector attacks")
                    } else {
                        Text("Let LLM find vector attacks")
                    }
                }
            }

            AnimatedVisibility(
                visible = state.stage == Stage.AttackVectorsBuilding || state.stage == Stage.EdgeValueCalculation,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    modifier = Modifier.padding(4.dp),
                    text = stringResource(Res.string.deselect_hint),
                    color = DeselectHint
                )
            }
        }

        AnimatedVisibility(
            visible = state.isGenerationInProgress,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column (
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Generating vector attacks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        }
    }
}

data class HostContainerData(
    val name: String,
    val rect: DpRect
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TechniqueNode(
    node: Node,
    isSelected: Boolean,
    isTarget: Boolean,
    isRoot: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    areMitigationsShown: Boolean,
    mitigations: List<Mitigation>,
    onToggleMitigationRelevance: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val techniqueTooltipState = rememberTooltipState(isPersistent = true)
    var techniqueShowTooltip by remember{ mutableStateOf(false) }
    var isTechniqueInfoIconVisible by remember{ mutableStateOf(false) }
    LaunchedEffect(techniqueShowTooltip) {
        if (techniqueShowTooltip) {
            techniqueTooltipState.show()
        } else {
            techniqueTooltipState.dismiss()
        }
    }

    Box(
        modifier = modifier
            .background(
                color = node.tactic.color,
                shape = RoundedCornerShape(4.dp)
            )
            .onPointerEvent(PointerEventType.Enter) {
                isTechniqueInfoIconVisible = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                isTechniqueInfoIconVisible = false
            }
            .clickable(enabled = isEnabled) { onClick() }
            .then(
                when {
                    isTarget && isSelected ->
                        Modifier
                            .border(2.dp, SelectedBorderColor, RoundedCornerShape(4.dp))
                            .border(3.dp, NodeBorderTarget, RoundedCornerShape(4.dp))

                    isTarget ->
                        Modifier.border(3.dp, NodeBorderTarget, RoundedCornerShape(4.dp))

                    isSelected ->
                        Modifier.border(2.dp, SelectedBorderColor, RoundedCornerShape(4.dp))

                    isRoot ->
                        Modifier.border(3.dp, NodeBorderRoot, RoundedCornerShape(4.dp))

                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                text = node.name,
                color = NodeTextColor,
            )

            if (areMitigationsShown && mitigations.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .padding(4.dp)
                ) {
                    mitigations.forEach {
                        val mitigationTooltipState = rememberTooltipState(isPersistent = true)
                        var mitigationShowTooltip by remember { mutableStateOf("") }
                        LaunchedEffect(mitigationShowTooltip) {
                            if (mitigationShowTooltip == it.id) {
                                mitigationTooltipState.show()
                            } else {
                                mitigationTooltipState.dismiss()
                            }
                        }

                        val backgroundColor = if (it.isRelevant) {
                            ErrorContainerColor
                        } else {
                            MitigationIrrelevant
                        }
                        TooltipBox(
                            modifier = Modifier
                                .padding(end = 2.dp)
                                .size(16.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(backgroundColor)
                                .clickable(
                                    onClick = {
                                        mitigationShowTooltip = it.id
                                    }
                                ),
                            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                                positioning = TooltipAnchorPosition.Above,
                                spacingBetweenTooltipAndAnchor = 0.dp
                            ),
                            enableUserInput = false,
                            tooltip = {
                                PlainTooltip(
                                    maxWidth = 400.dp,
                                ) {
                                    val mitigationDescription = stringResource(
                                        Res.string.mitigation_full_description_format,
                                        it.id,
                                        it.relationshipDescription,
                                        it.mitigationDescription
                                    )
                                    SelectionContainer {
                                        Column {
                                            Text(mitigationDescription)
                                            Button(
                                                onClick = { onToggleMitigationRelevance(it.id) },
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = if (it.isRelevant)
                                                        stringResource(Res.string.set_as_irrelevant)
                                                    else
                                                        stringResource(Res.string.set_as_relevant)
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            state = mitigationTooltipState,
                            onDismissRequest = { mitigationShowTooltip = "" }
                        ) {
                            val iconColor = if (it.isRelevant) {
                                OnErrorContainerColor
                            } else {
                                OnTertiaryColor
                            }

                            Icon(
                                modifier = Modifier
                                    .scale(0.7f),
                                painter = painterResource(Res.drawable.ic_shield),
                                tint = iconColor,
                                contentDescription = stringResource(Res.string.show_mitigation_info_content_desc)
                            )
                        }
                    }

                }
            }
        }

        TooltipBox(
            positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                positioning = TooltipAnchorPosition.Above,
                spacingBetweenTooltipAndAnchor = 0.dp
            ),
            enableUserInput = false,
            tooltip = {
                PlainTooltip(
                    maxWidth = 400.dp,
                    containerColor = TooltipBackgroundColor,
                    contentColor = TooltipContentColor
                ) {
                    SelectionContainer {
                        val maturityString = stringResource(
                            when (node.maturity) {
                                TechniqueMaturity.Demonstrated -> Res.string.maturity_demonstrated
                                TechniqueMaturity.Feasible -> Res.string.maturity_feasible
                                TechniqueMaturity.Realized -> Res.string.maturity_realized
                                TechniqueMaturity.Unknown -> Res.string.maturity_unknown
                            }
                        )
                        Text(
                            stringResource(
                                Res.string.description_maturity_severity_format,
                                node.techniqueId,
                                maturityString,
                                node.severityScore,
                                node.description
                            )
                        )
                    }
                }
            },
            state = techniqueTooltipState,
            onDismissRequest = { techniqueShowTooltip = false }
        ) {
            Icon(
                modifier = Modifier
                    .size(32.dp)
                    .scale(0.5f)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .padding(2.dp)
                    .alpha(if (isTechniqueInfoIconVisible) 1f else 0f)
                    // todo: remove onHover highlighting
                    .clickable {
                        techniqueShowTooltip = true
                    },
                painter = painterResource(Res.drawable.ic_info),
                tint = InfoIconColor,
                contentDescription = stringResource(Res.string.technique_description_content_desc)
            )
        }
    }
}

@Composable
private fun TechniqueEdge(
    probability: Float?,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    onDismissed: () -> Unit,
    onDelete: () -> Unit,
    onProbabilityChange: (Float) -> Unit,
) {
    val labelColor = if (probability == null) {
        ErrorColor.copy(alpha = 0.8f)
    } else if (!isEnabled){
        SecondaryContainerColor.copy(alpha = 0.8f)
    } else {
        EdgeLabelEnabled
    }
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(labelColor)
                .padding(if (isSelected) 2.dp else 1.dp)
                .animateContentSize(spring())
                .clickable(
                    enabled = isEnabled,
                    onClick = onClick),
        ) {
            if (isSelected) {
                FloatInputField(
                    value = probability,
                    onValueChange = { onProbabilityChange(it) },
                    onDismiss = onDismissed,
                    label = stringResource(Res.string.p_label),
                    enabled = true,
                    modifier = Modifier.widthIn(max = 100.dp)
                )
            } else {
                val displayProbability = probability?.let {
                    (round(it * 1000f) / 1000f).toString()
                } ?: stringResource(Res.string.unknown_value)
                Text(
                    modifier = Modifier
                        .padding(1.dp),
                    text = stringResource(
                        Res.string.edge_probability_risk_format,
                        displayProbability
                    ),
                    color = NodeTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        AnimatedVisibility(
            visible = isSelected ,
            enter = slideInVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            DeleteButton(
                onClick = onDelete,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun TacticColumn(
    xOffset: Dp,
    width: Dp,
    tacticName: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .offset(x = xOffset)
            .sideBorders(2.dp, color),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            modifier = Modifier
                .rotateLayout90()
                .padding(horizontal = 16.dp)
            ,
            text = tacticName.uppercase(),
            color = color,
            fontWeight = FontWeight.Black,
            maxLines = 1,
            fontSize = 20.sp,
            textAlign = TextAlign.Start,
            softWrap = false
        )
    }
}


private fun Modifier.rotateLayout90() = layout { measurable, constraints ->
    val childConstraints = constraints.copy(
        minWidth = constraints.minHeight,
        maxWidth = constraints.maxHeight,
        minHeight = constraints.minWidth,
        maxHeight = constraints.maxWidth
    )

    val placeable = measurable.measure(childConstraints)

    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2),
            layerBlock = {
                rotationZ = 90f
            }
        )
    }
}

fun Modifier.sideBorders(width: Dp, color: Color): Modifier = this.drawBehind {
    val strokeWidthPx = width.toPx()
    val halfWidth = strokeWidthPx / 2f
    // Left Border
    drawLine(
        color = color,
        start = Offset(x = halfWidth, y = 0f),
        end = Offset(x = halfWidth, y = size.height),
        strokeWidth = strokeWidthPx
    )
    // Right Border
    drawLine(
        color = color,
        start = Offset(x = size.width - halfWidth, y = 0f),
        end = Offset(x = size.width - halfWidth, y = size.height),
        strokeWidth = strokeWidthPx
    )
}


// Kuiver's graph coordinate space is dp throughout, so these are density-independent
private val COLUMN_X_SPACING = 400.dp
private val COLUMN_X_WIDTH = 230.dp

private val INITIAL_Y_PADDING = 50.dp
private val HOST_TITLE_HEIGHT = 40.dp
private val HOST_SPACING = 40.dp
private val NODE_X_OFFSET = 10.dp
private val DEFAULT_NODE_HEIGHT = 80.dp
private val NODE_Y_SPACING = 20.dp

private val HOST_CONTAINER_WIDTH_PADDING = 20.dp
private val HOST_CONTAINER_BOTTOM_PADDING = 10.dp

private val NODE_WIDTH = 170.dp
