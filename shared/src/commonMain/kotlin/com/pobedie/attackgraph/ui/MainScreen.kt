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
import attackgraph.shared.generated.resources.Res
import attackgraph.shared.generated.resources.build_attack_vectors_button
import attackgraph.shared.generated.resources.build_attack_vectors_hint
import attackgraph.shared.generated.resources.import_button
import attackgraph.shared.generated.resources.import_hint
import attackgraph.shared.generated.resources.mitigations_and_attacks_button
import attackgraph.shared.generated.resources.mitigations_and_attacks_hint
import attackgraph.shared.generated.resources.select_techniques_button
import attackgraph.shared.generated.resources.select_techniques_hint
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource


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
                    buttonText = stringResource(Res.string.import_button),
                    hintText = stringResource(Res.string.import_hint),
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
                    buttonText = stringResource(Res.string.select_techniques_button),
                    hintText = stringResource(Res.string.select_techniques_hint),
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
                    buttonText = stringResource(Res.string.build_attack_vectors_button),
                    hintText = stringResource(Res.string.build_attack_vectors_hint),
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
                    buttonText = stringResource(Res.string.mitigations_and_attacks_button),
                    hintText = stringResource(Res.string.mitigations_and_attacks_hint),
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
