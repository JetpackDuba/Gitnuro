package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.branch
import com.jetpackduba.gitnuro.git.remotes.RemoteInfo
import com.jetpackduba.gitnuro.ui.components.FilterDropdown
import com.jetpackduba.gitnuro.ui.dialogs.base.IconBasedDialog
import com.jetpackduba.gitnuro.ui.dialogs.base.MaterialDialog
import com.jetpackduba.gitnuro.ui.dropdowns.DropDownOption
import com.jetpackduba.gitnuro.viewmodels.ChangeUpstreamBranchDialogViewModel
import com.jetpackduba.gitnuro.viewmodels.SetDefaultUpstreamBranchState
import org.eclipse.jgit.lib.ObjectIdRef
import org.eclipse.jgit.lib.Ref
import org.jetbrains.compose.resources.painterResource

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
        onDismiss = {},
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
            onDismiss = onClose,
            setSelectedRemote = { viewModel.setSelectedRemote(it) },
            setSelectedBranch = { viewModel.setSelectedBranch(it) },
            changeDefaultUpstreamBranch = { viewModel.changeDefaultUpstreamBranch() }
        )
    }
}


@Composable
private fun SetDefaultUpstreamBranchDialogView(
    state: SetDefaultUpstreamBranchState,
    onDismiss: () -> Unit,
    setSelectedRemote: (RemoteInfo) -> Unit,
    setSelectedBranch: (Ref) -> Unit,
    changeDefaultUpstreamBranch: () -> Unit,
) {
    IconBasedDialog(
        icon = painterResource(Res.drawable.branch),
        title = "Change upstream branch",
        subtitle = "Set the upstream remote branch",
        primaryActionText = "Accept",
        onDismiss = onDismiss,
        onPrimaryActionClicked = changeDefaultUpstreamBranch,
        beforeActionsFocusRequester = null,
        actionsFocusRequester = null,
        afterActionsFocusRequester = null,
    ) {
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
    }
}