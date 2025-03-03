package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.branch
import com.jetpackduba.gitnuro.git.remotes.RemoteInfo
import com.jetpackduba.gitnuro.ui.components.FilterDropdown
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import com.jetpackduba.gitnuro.viewmodels.ChangeUpstreamBranchDialogViewModel
import com.jetpackduba.gitnuro.viewmodels.SetDefaultUpstreamBranchState
import org.eclipse.jgit.lib.ObjectIdRef
import org.eclipse.jgit.lib.Ref

@Preview
@Composable
fun SetDefaultUpstreamBranchDialogPreview() {
    SetDefaultUpstreamBranchDialogView(
        state = SetDefaultUpstreamBranchState.Loaded(
            ObjectIdRef.PeeledNonTag(null, "TestBranch", null),
            null,
            emptyList(),
            null,
            null
        ),
        onClose = {},
        setSelectedRemote = {},
        setSelectedBranch = {},
        changeDefaultUpstreamBranch = {}
    )
}

@Composable
fun SetDefaultUpstreamBranchDialog(
    viewModel: ChangeUpstreamBranchDialogViewModel,
    branch: Ref,
    onClose: () -> Unit,
) {
    LaunchedEffect(branch) {
        viewModel.init(branch)
    }

    val setDefaultUpstreamBranchState = viewModel.setDefaultUpstreamBranchState.collectAsState().value
    LaunchedEffect(setDefaultUpstreamBranchState) {
        if (setDefaultUpstreamBranchState is SetDefaultUpstreamBranchState.UpstreamChanged) {
            onClose()
        }
    }

    MaterialDialog(onCloseRequested = onClose) {
        SetDefaultUpstreamBranchDialogView(
            state = setDefaultUpstreamBranchState,
            onClose = onClose,
            setSelectedRemote = { viewModel.setSelectedRemote(it) },
            setSelectedBranch = { viewModel.setSelectedBranch(it) },
            changeDefaultUpstreamBranch = { viewModel.changeDefaultUpstreamBranch() }
        )
    }
}


@Composable
private fun SetDefaultUpstreamBranchDialogView(
    state: SetDefaultUpstreamBranchState,
    onClose: () -> Unit,
    setSelectedRemote: (RemoteInfo) -> Unit,
    setSelectedBranch: (Ref) -> Unit,
    changeDefaultUpstreamBranch: () -> Unit,
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(Res.drawable.branch),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(64.dp),
            tint = MaterialTheme.colors.onBackground,
        )

        Text(
            text = "Change upstream branch",
            modifier = Modifier
                .padding(bottom = 8.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "Set the upstream remote branch",
            modifier = Modifier
                .padding(bottom = 16.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            if (state is SetDefaultUpstreamBranchState.Loaded) {

                val remotesDropDown =
                    state.remotes.map { DropDownOption(it, it.remoteConfig.name) }

                val selectedRemote = state.selectedRemote
                val selectedRemoteOption = if (selectedRemote != null) {
                    DropDownOption(selectedRemote, selectedRemote.remoteConfig.name)
                } else {
                    null
                }

                val selectedBranch = state.selectedBranch
                val selectedBranchOption = if (selectedBranch != null) {
                    DropDownOption(selectedBranch, selectedBranch.simpleName)
                } else {
                    null
                }

                val branchesDropDown = remember(selectedRemote) {
                    selectedRemote?.branchesList?.map { ref ->
                        DropDownOption(ref, ref.simpleName)
                    }
                }

                Text(
                    text = "Remote",
                    modifier = Modifier
                        .padding(top = 8.dp),
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                )

                FilterDropdown(
                    remotesDropDown,
                    selectedRemoteOption,
                    width = 400.dp,
                    onOptionSelected = { setSelectedRemote(it.value) }
                )


                Text(
                    text = "Branch",
                    modifier = Modifier
                        .padding(top = 8.dp),
                    color = MaterialTheme.colors.onBackground,
                    style = MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                )

                FilterDropdown(
                    branchesDropDown ?: emptyList(),
                    selectedBranchOption,
                    width = 400.dp,
                    onOptionSelected = { setSelectedBranch(it.value) }
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.End)
        ) {
            PrimaryButton(
                text = "Cancel",
                modifier = Modifier.padding(end = 8.dp),
                onClick = onClose,
                backgroundColor = Color.Transparent,
                textColor = MaterialTheme.colors.onBackground,
            )
            PrimaryButton(
                onClick = {
                    changeDefaultUpstreamBranch()
                },
                text = "Change"
            )
        }
    }
}