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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastCoerceAtMost
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.ic_info
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
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.ui.ViewModel
import com.pobedie.attackgraph.ui.ViewState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun AttackGraph(
    viewModel: ViewModel,
    state: ViewState
) {
    val edges = remember(state.edges) {
        state.edges.map { _edge ->
            _edge.startNode to _edge.endNode
        }
    }
    val tactics = state.nodes.map { it.tactic }.toSet().sortedBy { it.id }
    // map of TacticIds to list of TechniquesIds
    val tacticToTechniques: Map<String, List<String>> = tactics.map { tactic ->
        val techList: MutableList<String> = mutableListOf()
        state.nodes.map { techNode ->
            if (techNode.tactic.id == tactic.id) techList.add(techNode.id)
        }
        return@map tactic.id to techList.toList().sortedBy { it }
    }.toMap()

    // Create graph structure
    val kuiver = remember(edges, state.nodes) {
        buildKuiver {
            nodes( ids = state.nodes.map { it.id } )
            edges( *edges.toTypedArray() )
        }
    }

    val tacticLayout: LayoutProvider = remember(state.nodes, tacticToTechniques) {
        { kuiver, _ ->
            val nodeList = kuiver.nodes.values.toList()

            val nodeColumnHeightSums: MutableMap<String, Float> = mutableMapOf()
            val updatedNodes = nodeList.mapIndexed { index, node ->
//            Fallback if index not found (technically this can't happen)
                val randInt = Random(index).nextInt().fastCoerceAtMost(20)
                val currentTechniqueNode = state.nodes[index]
                val currentTechniqueIndex = tacticToTechniques
                    .get(currentTechniqueNode.tactic.id)
                    ?.indexOf(currentTechniqueNode.id) ?: randInt

                val positionX =
                    (tacticToTechniques.keys.indexOf(currentTechniqueNode.tactic.id) * COLUMN_X_SPACING.toFloat())
                // todo: Try to fix error in calculation of node height (seems to be a lib's problem)
                val positionY =
                    ((nodeColumnHeightSums.get(currentTechniqueNode.tactic.id) ?: 0f) +
                            (currentTechniqueIndex * COLUMN_Y_SPACING.toFloat()))

                nodeColumnHeightSums.merge(
                    currentTechniqueNode.tactic.id,
                    (node.dimensions?.height?.value ?: 80f),
                    Float::plus
                )

                node.copy(
                    id = currentTechniqueNode.id,
                    position = Offset(positionX, positionY)
                )
            }
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
            val index = tacticToTechniques.keys.indexOf(tactic.id)
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
                    modifier = Modifier.width(180.dp),
                    node = node,
                    isSelected = node.id == state.selectedNode,
                    onClick = {
                        println("DEBUGG add node: ${node.id}")
                        viewModel.setNodeConnection(node.id)
                    }
                )
            },
            edgeContent = { libEdge, from, to ->
                // Customize edge appearance
                val _edge = state.edges.find { it.startNode == libEdge.fromId && it.endNode == libEdge.toId }
                EdgeContentWithLabel(
                    from,
                    to,
                    color = Color.Gray,
                    strokeWidth = 2f,
                    arrowDrawer = ArrowStyle,
                    enableCurve = true,
                    labelPlacement = LabelPlacement.END,
                    label = "test",
                    labelContent = { text ->
                        if (_edge != null) {
                            val isSelected = _edge.startNode == state.selectedEdge?.first &&
                                    _edge.endNode == state.selectedEdge.second
                            TechniqueEdge(
                                probability = _edge.probability,
                                punishment = _edge.punishment,
                                isSelected = isSelected,
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
        )

        Text(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
            ,
            text = "Right click anywhere to deselect",
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun TechniqueNode(
    node: Node,
    isSelected: Boolean,
    onClick: () -> Unit,
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
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(1.dp, Color.White, RoundedCornerShape(4.dp))
                } else Modifier
            )
        ,
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            text = node.name,
            color = Color.White,
        )

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
                        Text("ID: ${node.id}\n\n${node.description}")
                    }
                }
            },
            state = techniqueTooltipState,
            onDismissRequest = {techniqueShowTooltip = false}
        ) {
            Icon(
                modifier = Modifier
                    .padding(2.dp)
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .alpha(if (isTechniqueInfoIconVisible) 1f else 0f)
                    // todo: remove onHover highlighting
                    .clickable {
                        techniqueShowTooltip = true
                    },
                painter = painterResource(Res.drawable.ic_info),
                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f),
                contentDescription = "Technique description"
            )
        }
    }
}

@Composable
private fun TechniqueEdge(
    probability: Float?,
    punishment: Float?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDismissed: () -> Unit,
    onDelete: () -> Unit,
    onProbabilityChange: (Float) -> Unit,
    onPunishmentChange: (Float) -> Unit,
) {
    val labelColor = if (probability == null || punishment == null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    Column(
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(labelColor)
                .padding(if (isSelected) 2.dp else 1.dp)
                .clickable(onClick = onClick),
        ) {
            if (isSelected) {
                FloatInputField(
                    value = probability,
                    onValueChange = { onProbabilityChange(it) },
                    onDismiss = onDismissed,
                    label = "P",
                    enabled = true,
                    modifier = Modifier.width(60.dp)
                )
                FloatInputField(
                    value = punishment,
                    onValueChange = { onPunishmentChange(it) },
                    onDismiss = onDismissed,
                    label = "R",
                    enabled = true,
                    modifier = Modifier.width(60.dp)
                )
            } else {
                Text(
                    modifier = Modifier
                        .padding(1.dp),
                    text = "P:${probability ?: "???"} R:${punishment ?: "???"}"
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
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .clickable(onClick = onDelete),
            ) {
                Icon(
                    modifier = Modifier.padding(2.dp),
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete connection"
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

@Composable
private fun FloatInputField(
    value: Float?,
    label: String,
    onValueChange: (Float) -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp),
    backgroundColor: Color = Color.White,
    shape: RoundedCornerShape = RoundedCornerShape(2.dp)
) {
    var text by remember { mutableStateOf(value?.toString() ?: "") }

    LaunchedEffect(value) {
        val currentFloat = text.toFloatOrNull()
        if (value != null && !value.isNaN()) {
            if (currentFloat != value) {
                text = value.toString()
            }
        } else if (value == null) {
            text = ""
        }
    }

    BasicTextField(
        value = text,
        onValueChange = { newText ->
            val floatValue = newText.toFloatOrNull()
            when {
                floatValue == null -> {
                    text = newText
                }
                floatValue in 0f..1f -> {
                    text = newText
                    onValueChange(floatValue)
                }
                floatValue > 1f -> {
                    text = "1"
                    onValueChange(1f)
                }
                floatValue < 0f -> {
                    text = "0"
                    onValueChange(0f)
                }
            }
        },
        modifier = modifier
            .background(backgroundColor, shape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        enabled = enabled,
        textStyle = textStyle,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = { onDismiss() }
        ),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$label:",
                    style = textStyle,
                    color = if (enabled) Color.Black else Color.Gray
                )
                Box(modifier = Modifier.weight(1f)) {
                    innerTextField()
                }
            }
        }
    )
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
private const val COLUMN_Y_SPACING = 60
private const val COLUMN_X_WIDTH = 230
