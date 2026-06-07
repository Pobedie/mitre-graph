package com.pobedie.attackgraph.ui

import androidx.compose.ui.graphics.Color
import com.pobedie.attackgraph.core.entity.Node
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class ViewModel() {

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

    fun importAtlasData(){

    }

}