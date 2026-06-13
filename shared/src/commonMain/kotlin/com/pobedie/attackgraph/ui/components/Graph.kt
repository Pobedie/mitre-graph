package com.pobedie.attackgraph.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.edges
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.layout.LayoutDirection
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.ui.ArrowDrawer
import com.dk.kuiver.ui.EdgeContent
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.ui.Stage
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun AtlasGraphViewer(
    nodes: List<Node>
) {
    val edges = nodes.flatMap { _node ->
        _node.connectedNodes.map { _connectedNode ->
            _node.id to _connectedNode
        }
    }

    // Create graph structure
    val kuiver = remember {
        buildKuiver {
            // Add nodes
//            nodes("A", "B", "C")
            nodes(nodes.map { it.id })

            // Add edges
//            edges(
//                "A" to "B",
//                "B" to "C",
//                "A" to "C"
//            )
            edges( *edges.toTypedArray() )
        }
    }

    // Configure layout
    val layoutConfig = LayoutConfig.Hierarchical(
        direction = LayoutDirection.HORIZONTAL
    )

    val viewerState = rememberKuiverViewerState(
        initialKuiver = kuiver,
        layoutConfig = layoutConfig
    )

        // Render the graph
        KuiverViewer(
            state = viewerState,
            nodeContent = { libNode ->
                // Customize node appearance
                val title = nodes.findLast { it.id == libNode.id } ?: return@KuiverViewer
                Box(
                    modifier = Modifier
//                    .size(40.dp)
                        .background(
                            Color(50,100,80),
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 2.dp, horizontal = 8.dp)
                    ) {
                        Text( title.name, color = Color.White)
                    }
                }
            },
            edgeContent = { edge, from, to ->
                // Customize edge appearance
                EdgeContent(
                    from,
                    to,
                    color = Color.Gray,
                    strokeWidth = 2f,
                    arrowDrawer = ArrowStyle
                )
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
