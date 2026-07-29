package com.pobedie.attackgraph.ui.stages

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
import androidx.compose.foundation.gestures.onDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.delete_connection_content_desc
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
import com.pobedie.attackgraph.core.entity.Host
import com.pobedie.attackgraph.ui.ViewModel
import com.pobedie.attackgraph.ui.ViewState
import com.pobedie.attackgraph.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import kotlin.collections.emptyList
import kotlin.collections.mutableSetOf

@Composable
fun FirewallMapping(
    viewModel: ViewModel,
    state: ViewState
) {
    val hostNodes = remember(state.hosts) {
        state.hosts.map { it.id }
    }

    val edges = remember(state.firewallRules) {
        state.firewallRules.map { rule ->
            val startHostId = rule.sourceHostId
            startHostId to rule.targetHostId
        }.distinct()
    }


    val kuiver = remember(hostNodes, edges) {
        buildKuiver {
            nodes(ids = hostNodes)
            edges(*edges.toTypedArray())
        }
    }

    var nodePositions: MutableSet<Pair<String, Offset>> by remember {
        mutableStateOf(
            kuiver.nodes.values.mapIndexedTo(mutableSetOf()) { index, node ->
                Pair(node.id, Offset(index * HOST_HORIZONTAL_SPACING, 0f))
            }
        )
    }

    val hostLayout: LayoutProvider = remember(state.hosts, nodePositions) {
        { kuiver, _ ->
            val updatedNodes = kuiver.nodes.values.mapIndexed { index, node ->
                val posOverride = nodePositions.find { it.first == node.id }
                if (posOverride != null) {
                    node.copy(position = posOverride.second)
                } else {
                    node.copy(position = Offset.Zero)
                }
            }
            buildKuiverWithClassifiedEdges(updatedNodes, kuiver.edges)
        }
    }

    val layoutConfig = remember(hostLayout) {
        LayoutConfig.Custom(provider = hostLayout)
    }


    val viewerState = rememberKuiverViewerState(
        initialKuiver = kuiver,
        layoutConfig = layoutConfig
    )

    LaunchedEffect(kuiver) {
        viewerState.updateKuiver(kuiver)
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
        KuiverViewer(
            state = viewerState,
            config = KuiverViewerConfig(
                nodeAnimationSpec = snap(),
                edgeAnimationSpec = snap(),
                zoomConditionDesktop = { true }
            ),
            nodeContent = { libNode ->
                val host = state.hosts.find { it.id == libNode.id } ?: return@KuiverViewer
                val selectedNode = state.selectedNode
                val isSelectedSource = selectedNode == host.id
                val selectedSourceTechId = if (selectedNode?.startsWith("${host.id}_") == true)
                    selectedNode.substringAfter("${host.id}_")
                else null

                val sourceHostId = selectedNode?.substringBefore("_")
                val sourceTechId = if (selectedNode?.contains("_") == true)
                    selectedNode.substringAfter("_")
                else null

                HostNode(
                    host = host,
                    isSelectedSource = isSelectedSource,
                    selectedTechniqueId = selectedSourceTechId,
                    onHostClick = {
                        if (selectedNode == null) {
                            viewModel.selectSourceNode(host.id)
                        } else {
                            if (selectedNode == host.id) {
                                viewModel.clearNodeSelection()
                            } else {
                                sourceHostId?.let {
                                    viewModel.setFirewallConnection(it, sourceTechId, host.id)
                                }
                                viewModel.clearNodeSelection()
                            }
                        }
                    },
                    onTechniqueClick = { techId ->
                        val fullId = "${host.id}_$techId"
                        if (selectedNode == fullId) {
                            viewModel.clearNodeSelection()
                        } else if (selectedNode == null) {
                            viewModel.selectSourceNode(fullId)
                        } else {
                            sourceHostId?.let {
                                viewModel.setFirewallConnection(it, sourceTechId, host.id)
                            }
                            viewModel.clearNodeSelection()
                        }
                    },
                    onDrag = { _offset ->
                        nodePositions = nodePositions.mapTo(mutableSetOf()) { _node ->
                            if (libNode.id == _node.first) {
                                _node.copy(second = _node.second + _offset)
                            } else _node
                        }
                    }
                )
            },
            edgeContent = { libEdge, from, to ->
                val relevantRules = state.firewallRules.filter {
                    it.sourceHostId == libEdge.fromId &&
                            it.targetHostId == libEdge.toId
                }

                if (relevantRules.isNotEmpty()) {
                    val isEdgeSelected = state.selectedEdge?.let {
                        it.first == libEdge.fromId && it.second == libEdge.toId
                    } ?: false

                    val sourceIndex = hostNodes.indexOf(libEdge.fromId)
                    val targetIndex = hostNodes.indexOf(libEdge.toId)
                    val isForwardArrow = sourceIndex < targetIndex
                    val verticalOffset = if (isForwardArrow) 80f else 0f
                    val adjustedFrom = from.copy(y = from.y + verticalOffset)
                    val adjustedTo = to.copy(y = to.y + verticalOffset)

                    Box(modifier = Modifier.zIndex(if (isEdgeSelected) 1000f else 0f)) {
                        EdgeContentWithLabel(
                            adjustedFrom,
                            adjustedTo,
                            color = EdgeDefault,
                            strokeWidth = 2f,
                            arrowDrawer = ArrowStyle,
                            enableCurve = true,
                            labelPlacement = LabelPlacement.CENTER,
                            label = "rules_${libEdge.fromId}_${libEdge.toId}",
                            labelContent = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    relevantRules.forEach { rule ->
                                        val sourceTechniqueName = rule.sourceTechniqueId?.let { techId ->
                                            state.hosts.find { it.id == rule.sourceHostId }
                                                ?.techniques?.find { it.id == techId }?.name
                                        }

                                        FirewallEdge(
                                            sourceName = sourceTechniqueName ?: "Host",
                                            isSelected = isEdgeSelected,
                                            onClick = { viewModel.selectEdge(libEdge.fromId, libEdge.toId) },
                                            onDelete = {
                                                viewModel.setFirewallConnection(rule.sourceHostId, rule.sourceTechniqueId, rule.targetHostId)
                                            }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HostNode(
    host: Host,
    isSelectedSource: Boolean,
    selectedTechniqueId: String?,
    onHostClick: () -> Unit,
    onTechniqueClick: (String) -> Unit,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(250.dp)
            .background(FirewallHostBackground, RoundedCornerShape(8.dp))
            .border(
                width = if (isSelectedSource) 3.dp else 1.dp,
                color = if (isSelectedSource) FirewallTechniqueSelectedText else FirewallHostBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .onDrag{
                onDrag(it)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, FirewallHostHeaderBorder, RoundedCornerShape(4.dp))
                .clickable { onHostClick() }
                .padding(8.dp)
        ) {
            Text(
                text = host.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = FirewallHostHeaderText
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        host.techniques.forEach { tech ->
            val isTechSelected = selectedTechniqueId == tech.id
            Text(
                text = tech.name,
                fontSize = 11.sp,
                color = if (isTechSelected) FirewallTechniqueSelectedText else FirewallTechniqueText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isTechSelected) FirewallTechniqueSelectedBackground else Color.Transparent,
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onTechniqueClick(tech.id) }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun FirewallEdge(
    sourceName: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(2.dp))
                .background(EdgeLabelEnabled.copy(alpha = 0.8f))
                .widthIn(max = 100.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = sourceName,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = NodeTextColor,
                overflow = TextOverflow.MiddleEllipsis,
                maxLines = 1
            )
        }
        AnimatedVisibility(
            visible = isSelected,
            enter = slideInVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ErrorContainerColor)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.delete_connection_content_desc),
                    tint = OnErrorContainerColor
                )
            }
        }
    }
}

private const val HOST_HORIZONTAL_SPACING = 500f
