package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.viewmodels.SquashCommitsState
import com.jetpackduba.gitnuro.viewmodels.SquashCommitsViewModel

@Composable
fun SquashCommits(
    squashCommitsViewModel: SquashCommitsViewModel
) {
    val state = squashCommitsViewModel.squashState.collectAsState()
    val stateValue = state.value

    Box(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxSize(),
    ) {
        when (stateValue) {
            is SquashCommitsState.Failed -> {}
            is SquashCommitsState.Loaded -> {
                SquashCommitsLoaded(
                    squashCommitsViewModel,
                    stateValue,
                )
            }

            SquashCommitsState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun SquashCommitsLoaded(
    viewModel: SquashCommitsViewModel,
    stateValue: SquashCommitsState.Loaded,
) {
    Column {
        Text(
            text = "Edit message for squashed commits",
            color = MaterialTheme.colors.onBackground,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            fontSize = 20.sp,
        )

        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            AdjustableOutlinedTextField(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp),
                value = stateValue.message,
                onValueChange = {
                    viewModel.editMessage(it)
                },
                textStyle = MaterialTheme.typography.body2,
                backgroundColor = MaterialTheme.colors.background
            )
        }

        Row(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            PrimaryButton(
                text = "Cancel",
                modifier = Modifier.padding(end = 8.dp),
                onClick = {
                    viewModel.cancel()
                },
                backgroundColor = Color.Transparent,
                textColor = MaterialTheme.colors.onBackground,
            )
            PrimaryButton(
                modifier = Modifier.padding(end = 16.dp),
                onClick = {
                    viewModel.continueSquash()
                },
                text = "OK"
            )
        }
    }
}