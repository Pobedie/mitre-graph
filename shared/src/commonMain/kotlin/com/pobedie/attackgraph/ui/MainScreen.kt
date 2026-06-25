package com.pobedie.attackgraph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pobedie.attackgraph.ui.Stages.AttackGraph
import com.pobedie.attackgraph.ui.Stages.ImportStage
import com.pobedie.attackgraph.ui.Stages.TechniqueSelection
import com.pobedie.attackgraph.ui.components.AlphaValueDialog
import com.pobedie.attackgraph.ui.components.StageArrow
import com.pobedie.attackgraph.ui.components.StageButton
import kotlinx.coroutines.launch


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(
    viewModel: ViewModel
){

    val state by viewModel.state.collectAsStateWithLifecycle()

    println("INFO: imported ${state.nodes.size} nodes")

    Column(
        modifier = Modifier
            .background(Color(20, 20, 20))
            .fillMaxSize(),
    ) {
        val scrollState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        LazyRow(
            state = scrollState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp)
                .background(Color.DarkGray)
                // by default to scroll horizontaly you need to use Shift+MouseWheel which is a bad UX in this case
                .onPointerEvent(PointerEventType.Scroll) {
                    val delta = it.changes.first().scrollDelta
                    coroutineScope.launch {
                        scrollState.scrollBy(delta.y * 40f)
                    }
                },

            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                StageButton(
                    onClick = {
                        viewModel.switchToImportStage()
                        viewModel.importAtlasData()
                              },
                    buttonText = "Import",
                    hintText = "Import a YAML file with MITRE ATLAS data.\n\n" +
                            "You can find this file at https://github.com/mitre-atlas/atlas-data/blob/main/dist/ or" +
                            " use the included one (it might be not relevant)",
                    isHighlighted = state.stage == Stage.Import,
                    isEnabled = true
                )
            }
            StageArrow()
            item {
                StageButton(
                    onClick = {
                        viewModel.switchToTechniqueSelectionStage()
                    },
                    buttonText = "Select techniques",
                    hintText = "Select techniques for each tactic",
                    isHighlighted = state.stage == Stage.TechniqueSelection,
                    isEnabled = state.isTechniqueSelectionStageAvailable
                )
            }
            StageArrow()
            item {
                StageButton(
                    onClick = {
                        viewModel.switchToAttackVectorBuildingStage()
                    },
                    buttonText = "Build attack vectors",
                    hintText = "Show attack vectors by drawing edges between nodes, then set penalty and risk values (0.0-1.0)",
                    isHighlighted = state.stage == Stage.AttackVectorsBuilding,
                    isEnabled = state.isAttackVectorMappingStageAvailable
                )
            }
            StageArrow()
            item {
                StageButton(
                    onClick = {
                        viewModel.showAlphaValueDialog()
                    },
                    buttonText = "Attack vectors and mitigations",
                    hintText = "Show proven by case-studies attack vectors and mitigations",
                    isHighlighted = state.stage == Stage.MitigationsAndAttacks,
                    isEnabled = state.isMitigationsAndAttacksStageAvailable
                )
            }

        }

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state.stage) {
                Stage.Import -> ImportStage(viewModel, state)
                Stage.TechniqueSelection -> TechniqueSelection(viewModel, state)
                Stage.AttackVectorsBuilding,
                Stage.MitigationsAndAttacks,
                Stage.BestPath -> AttackGraph(viewModel, state)
            }

            if (state.alphaValueDialogVisible) {
                AlphaValueDialog(
                    alpha = state.alphaValue,
                    onClick ={
                        viewModel.setAlphaValue(it)
                        viewModel.switchToMitigationsAndAttacks()
                    }
                )
            }
        }
    }
}
