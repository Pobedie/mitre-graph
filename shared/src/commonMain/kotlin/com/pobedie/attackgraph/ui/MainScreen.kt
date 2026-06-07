package com.pobedie.attackgraph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dk.kuiver.model.buildKuiver
import com.dk.kuiver.model.edges
import com.dk.kuiver.model.layout.LayoutConfig
import com.dk.kuiver.model.layout.LayoutDirection
import com.dk.kuiver.model.nodes
import com.dk.kuiver.rememberKuiverViewerState
import com.dk.kuiver.renderer.KuiverViewer
import com.dk.kuiver.ui.EdgeContent
import com.pobedie.attackgraph.core.entity.Node
import com.pobedie.attackgraph.ui.components.ArrowStyle
import com.pobedie.attackgraph.ui.components.MyGraphViewer


@Composable
fun MainScreen(){

    val viewModel = ViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    println("DEBUGG state.nodes :  ${state.nodes}")


    Row(
        modifier = Modifier
            .background(Color(100, 100, 100))
            .padding(8.dp)
            .background(Color(20, 20, 20))
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(Color.DarkGray)
        ) {
            Button(
                onClick = { viewModel.importAtlasData() }
            ) {
                Text("Import MITRE ATLAS data")
            }

        }

            MyGraphViewer(
                nodes = state.nodes
            )
        }


}
