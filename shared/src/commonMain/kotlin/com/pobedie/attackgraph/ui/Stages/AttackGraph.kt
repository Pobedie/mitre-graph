package com.pobedie.attackgraph.ui.Stages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.delete_connection_content_desc
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
import attackgraph.shared.generated.resources.r_label
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
import com.dk.kuiver.ui.ArrowDrawer
import com.dk.kuiver.ui.EdgeContentWithLabel
import com.dk.kuiver.ui.LabelPlacement
import com.pobedie.attackgraph.core.entity.EdgeState
import com.pobedie.attackgraph.core.entity.Mitigation
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.core.entity.TechniqueMaturity
import com.pobedie.attackgraph.ui.Stage
import com.pobedie.attackgraph.ui.ViewModel
import com.pobedie.attackgraph.ui.ViewState
import com.pobedie.attackgraph.ui.components.FloatInputField
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AttackGraph(
    viewModel: ViewModel,
    state: ViewState
) {
    val tactics = remember(state.nodes) {
        state.nodes.map { it.tactic }.toSet().sortedBy { it.id }
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
                val x = tacticIndex * COLUMN_X_SPACING.toFloat()
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
                                position = Offset(x + NODE_X_OFFSET, currentY)
                            )
                        )
                        currentY += (libNode.dimensions?.height?.value ?: DEFAULT_NODE_HEIGHT) + NODE_Y_SPACING
                    }

                    newHostContainers.add(
                        HostContainerData(
                            name = hostName,
                            rect = Rect(
                                Offset(x, hostStartY),
                                Size(
                                    NODE_WIDTH + HOST_CONTAINER_WIDTH_PADDING,
                                    currentY - hostStartY - HOST_CONTAINER_BOTTOM_PADDING
                                )
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
        tactics.forEach { tactic ->
            val index = tactics.indexOf(tactic)
            val vsOffset = viewerState.offset.x
            val vsScale = viewerState.scale
            // shifting coords from center to the first column
            val coordsOffset = ((tactics.size / 2f * COLUMN_X_SPACING - COLUMN_X_SPACING / 2f) * vsScale)
            val columnXOffset = ((vsOffset - coordsOffset) + (index * COLUMN_X_SPACING * vsScale)).toInt()
            TacticColumn(
                xOffset = columnXOffset,
                width = vsScale.dp * COLUMN_X_WIDTH,
                tacticName = tactic.name,
                color = tactic.color
            )
        }

        // Render the host containers behind the graph
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .drawBehind {
                    val vsScale = viewerState.scale
                    val vsOffset = viewerState.offset

                    val minNodeY = (hostContainers.minOfOrNull { it.rect.top } ?: 0f) + HOST_TITLE_HEIGHT
                    val maxNodeY = (hostContainers.maxOfOrNull { it.rect.bottom } ?: 0f) + HOST_CONTAINER_BOTTOM_PADDING - NODE_Y_SPACING
                    val graphCenterY = (minNodeY + maxNodeY) / 2f

                    val coordsOffsetX = (tactics.size / 2f * COLUMN_X_SPACING - COLUMN_X_SPACING / 2f)
                    val graphCenterX = coordsOffsetX + (NODE_X_OFFSET + NODE_WIDTH / 2f)

                    hostContainers.forEach { container ->
                        val nodeOffsetX = container.rect.left - graphCenterX
                        val nodeOffsetY = container.rect.top - graphCenterY
                        val translatedRect = Rect(
                            offset = Offset(
                                center.x + (vsOffset.x + nodeOffsetX * vsScale),
                                center.y + (vsOffset.y + nodeOffsetY * vsScale)
                            ),
                            size = container.rect.size * vsScale
                        )

                        drawRoundRect(
                            color = HostContainerBackground,
                            topLeft = translatedRect.topLeft,
                            size = translatedRect.size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * vsScale)
                        )
                        drawRoundRect(
                            color = HostContainerBorder,
                            topLeft = translatedRect.topLeft,
                            size = translatedRect.size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f * vsScale),
                            style = Stroke(width = 1f * vsScale)
                        )
                        drawText(
                            textMeasurer = textMeasurer,
                            size = Size(height = HOST_TITLE_HEIGHT*vsScale, width = container.rect.width*vsScale),
                            text = container.name,
                            topLeft = translatedRect.topLeft + Offset(8f * vsScale, 4f * vsScale),
                            style = TextStyle(
                                color = PrimaryTextColor,
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
                edgeAnimationSpec = snap(),
                zoomConditionDesktop = { true }
            ),
            nodeContent = { libNode ->
                val node = state.nodes.findLast { it.id == libNode.id } ?: return@KuiverViewer
                TechniqueNode(
                    modifier = Modifier.width(NODE_WIDTH.dp),
                    node = node,
                    isSelected = node.id == state.selectedNode,
                    isTarget = state.targetTechnique == node.techniqueId,
                    isEnabled = state.stage == Stage.AttackVectorsBuilding,
                    onClick = {
                        viewModel.setNodeConnection(node.id)
                    },
                    areMitigationsShown = state.stage == Stage.MitigationsAndAttacks,
                    mitigations = state.mitigations.filter { it.targetTechnique == node.techniqueId },
                    onToggleMitigationRelevance = {
                        viewModel.toggleMitigationRelevance(it)
                    }
                )
            },
            edgeContent = { libEdge, from, to ->
                // Customize edge appearance
                val _edge = state.edges.find { it.startNode == libEdge.fromId && it.endNode == libEdge.toId }
                val edgeColor =
                    when {
                        state.stage == Stage.AttackVectorsBuilding ||
                        _edge == null -> EdgeDefault
                        _edge.state == EdgeState.MostOptimal -> EdgeOptimal
                        _edge.state == EdgeState.Probable -> EdgeProbable
                        else -> EdgeDefault
                    }
                val isSelected = state.selectedEdge?.let {
                    _edge != null && _edge.startNode == it.first && _edge.endNode == it.second
                } ?: false
                Box(modifier = Modifier.zIndex(if (isSelected) 1000f else 0f)) {
                    EdgeContentWithLabel(
                        from,
                        to,
                        color = edgeColor,
                        strokeWidth = 2f,
                        arrowDrawer = ArrowStyle,
                        enableCurve = true,
                        labelPlacement = LabelPlacement.END,
                        label = "there must be anything for the lable to show up, even if it's not being used",
                        labelContent = { _ ->
                            if (_edge != null) {
                                TechniqueEdge(
                                    probability = _edge.probability,
                                    risk = _edge.risk,
                                    isSelected = isSelected,
                                    isEnabled = state.stage == Stage.AttackVectorsBuilding,
                                    onClick = { viewModel.selectEdge(_edge.startNode, _edge.endNode) },
                                    onDismissed = { viewModel.clearEdgeSelection() },
                                    onDelete = { viewModel.deleteEdge(_edge.startNode, _edge.endNode) },
                                    onProbabilityChange = {
                                        viewModel.changeEdgeProbability(_edge.startNode, _edge.endNode, it)
                                    },
                                    onPunishmentChange = {
                                        viewModel.changeEdgePunishment(_edge.startNode, _edge.endNode, it)
                                    },
                                )
                            }
                        }
                    )
                }
            }
        )

        AnimatedVisibility(
            visible = state.stage == Stage.AttackVectorsBuilding,
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Text(
                modifier = Modifier.padding(4.dp),
                text = stringResource(Res.string.deselect_hint),
                color = DeselectHint
            )
        }
    }
}

data class HostContainerData(
    val name: String,
    val rect: Rect
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TechniqueNode(
    node: Node,
    isSelected: Boolean,
    isTarget: Boolean,
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
                            .border(1.dp, SelectedBorderColor, RoundedCornerShape(4.dp))
                            .border(3.dp, NodeBorderTarget, RoundedCornerShape(4.dp))

                    isTarget ->
                        Modifier.border(3.dp, NodeBorderTarget, RoundedCornerShape(4.dp))

                    isSelected ->
                        Modifier.border(1.dp, SelectedBorderColor, RoundedCornerShape(4.dp))

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
                color = PrimaryTextColor,
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
    risk: Float?,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    onDismissed: () -> Unit,
    onDelete: () -> Unit,
    onProbabilityChange: (Float) -> Unit,
    onPunishmentChange: (Float) -> Unit,
) {
    val labelColor = if (probability == null || risk == null) {
        ErrorColor.copy(alpha = 0.8f)
    } else if (!isEnabled){
        EdgeLabelDisabled
    } else {
        SecondaryContainerColor.copy(alpha = 0.8f)
    }
    Column(
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(labelColor)
                .padding(if (isSelected) 2.dp else 1.dp)
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
                    modifier = Modifier.width(60.dp)
                )
                FloatInputField(
                    value = risk,
                    onValueChange = { onPunishmentChange(it) },
                    onDismiss = onDismissed,
                    label = stringResource(Res.string.r_label),
                    enabled = true,
                    modifier = Modifier.width(60.dp)
                )
            } else {
                Text(
                    modifier = Modifier
                        .padding(1.dp),
                    text = stringResource(
                        Res.string.edge_probability_risk_format,
                        probability ?: stringResource(Res.string.unknown_value),
                        risk ?: stringResource(Res.string.unknown_value)
                    )
                )
            }
        }
        AnimatedVisibility(
            visible = isSelected ,
            enter = slideInVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ErrorContainerColor)
                    .clickable(onClick = onDelete),
            ) {
                Icon(
                    modifier = Modifier.padding(2.dp),
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete_connection_content_desc)
                )
            }
        }
    }
}

@Composable
private fun TacticColumn(
    xOffset: Int,
    width: Dp,
    tacticName: String,
    color: Color
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(width)
            .offset { IntOffset (xOffset, 0)}
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


val ArrowStyle: ArrowDrawer = { arrowTip, direction, color ->
    val angle = atan2(direction.y.toDouble(), direction.x.toDouble()).toFloat()
    val arrowSize = 10f
    val arrowOffset = 8f
    val arrowAngleSpread = 0.5f
    val arrowBasePoint = Offset(
        arrowTip.x - direction.x * arrowOffset,
        arrowTip.y - direction.y * arrowOffset
    )
    val arrowPath = Path().apply {
        moveTo(arrowBasePoint.x, arrowBasePoint.y)
        lineTo(
            arrowBasePoint.x - arrowSize * cos(angle - arrowAngleSpread),
            arrowBasePoint.y - arrowSize * sin(angle - arrowAngleSpread)
        )
        lineTo(
            arrowBasePoint.x - arrowSize * cos(angle + arrowAngleSpread),
            arrowBasePoint.y - arrowSize * sin(angle + arrowAngleSpread)
        )
        close()
    }
    drawPath(path = arrowPath, color = color.copy(alpha = 1.0f))
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


private const val COLUMN_X_SPACING = 400
private const val COLUMN_X_WIDTH = 230

private const val INITIAL_Y_PADDING = 50f
private const val HOST_TITLE_HEIGHT = 40f
private const val HOST_SPACING = 40f
private const val NODE_X_OFFSET = 10f
private const val DEFAULT_NODE_HEIGHT = 80f
private const val NODE_Y_SPACING = 20f

private const val HOST_CONTAINER_WIDTH_PADDING = 20f
private const val HOST_CONTAINER_BOTTOM_PADDING = 10f

private const val NODE_WIDTH = 170
