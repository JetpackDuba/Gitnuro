package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.LocalTabFocusRequester
import com.jetpackduba.gitnuro.Screen
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.domain.extensions.isValid
import com.jetpackduba.gitnuro.app.generated.resources.*
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.ui.SelectedItem
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.Tag
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.SideMenuHeader
import com.jetpackduba.gitnuro.ui.components.SideMenuSubentry
import com.jetpackduba.gitnuro.ui.components.tooltip.DelayedTooltip
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.viewmodels.sidepanel.*
import kotlinx.coroutines.flow.collectLatest
import org.eclipse.jgit.submodule.SubmoduleStatus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SidePanel(
    sidePanelViewModel: SidePanelViewModel,
    onNavigate: (Screen) -> Unit,
) {
    val filter by sidePanelViewModel.filter.collectAsState()
    val selectedItem by sidePanelViewModel.selectedItem.collectAsState()

    val branchesState by sidePanelViewModel.branchesState.collectAsState()
    val remotesState by sidePanelViewModel.remoteState.collectAsState()
    val tagsState by sidePanelViewModel.tagsState.collectAsState()
    val stashesState by sidePanelViewModel.stashesState.collectAsState()
    val submodulesState by sidePanelViewModel.submodules.collectAsState()

    val searchFocusRequester = remember { FocusRequester() }
    val tabFocusRequester = LocalTabFocusRequester.current

    LaunchedEffect(sidePanelViewModel) {
        sidePanelViewModel.freeSearchFocusFlow.collectLatest {
            tabFocusRequester.requestFocus()
        }
    }

    Column {
        FilterTextField(
            value = filter,
            onValueChange = { newValue ->
                sidePanelViewModel.newFilter(newValue)
            },
            modifier = Modifier
                .padding(start = 8.dp)
                .focusRequester(searchFocusRequester)
                .onFocusChanged {
                    if (it.isFocused) {
                        sidePanelViewModel.addSidePanelSearchToCloseables()
                    } else {
                        sidePanelViewModel.removeSidePanelSearchFromCloseables()
                    }
                }
        )

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
        ) {
            localBranches(
                branchesState = branchesState,
                selectedItem = selectedItem,
                viewModel = sidePanelViewModel,
                onChangeDefaultUpstreamBranch = { onNavigate(Screen.BranchChangeUpstream(it)) },
                onRenameBranch = { onNavigate(Screen.BranchRename(it)) },
            )

            remotes(
                remotesState = remotesState,
                viewModel = sidePanelViewModel,
                onShowAddEditRemoteDialog = { onNavigate(Screen.AddEditRemote(it)) },
            )

            tags(
                tagsState = tagsState,
                selectedItem = selectedItem,
                viewModel = sidePanelViewModel,
            )

            stashes(
                stashesState = stashesState,
                selectedItem = selectedItem,
                viewModel = sidePanelViewModel,
            )

            submodules(
                submodulesState = submodulesState,
                viewModel = sidePanelViewModel,
                onAddSubmodule = { onNavigate(Screen.SubmoduleAdd) },
            )
        }
    }
}

@Composable
fun FilterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
) {
    AdjustableOutlinedTextField(
        value = value,
        hint = stringResource(Res.string.side_pane_search_hint),
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(
            fontSize = MaterialTheme.typography.body2.fontSize,
            color = MaterialTheme.colors.onBackground,
        ),
        singleLine = true,
        leadingIcon = {
            Icon(
                painterResource(Res.drawable.search),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (value.isEmpty()) MaterialTheme.colors.onBackgroundSecondary else MaterialTheme.colors.onBackground
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier
                        .size(16.dp)
                        .handOnHover(),
                ) {
                    Icon(
                        painterResource(Res.drawable.close),
                        contentDescription = null,
                        tint = if (value.isEmpty()) MaterialTheme.colors.onBackgroundSecondary else MaterialTheme.colors.onBackground
                    )
                }
            }
        }
    )
}

fun LazyListScope.localBranches(
    branchesState: BranchesState,
    selectedItem: SelectedItem,
    viewModel: SidePanelViewModel,
    onChangeDefaultUpstreamBranch: (Branch) -> Unit,
    onRenameBranch: (Branch) -> Unit,
) {
    val isExpanded = branchesState.isExpanded
    val branches = branchesState.branches
    val currentBranch = branchesState.currentBranch

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_local_branches_title),
                icon = painterResource(Res.drawable.branch),
                itemsCount = branches.count(),
                hoverIcon = null,
                isExpanded = isExpanded,
                onExpand = { viewModel.onExpandBranches() }
            )
        }
    }

    if (isExpanded) {
        items(branches, key = { it.name }) { branch ->
            Branch(
                branch = branch,
                isSelectedItem = selectedItem is SelectedItem.Ref && selectedItem.ref == branch,
                currentBranch = currentBranch,
                onBranchClicked = { viewModel.selectBranch(branch) },
                onBranchDoubleClicked = { viewModel.checkoutBranch(branch) },
                onCheckoutBranch = { viewModel.checkoutBranch(branch) },
                onMergeBranch = { viewModel.mergeBranch(branch) },
                onRebaseBranch = { viewModel.rebaseBranch(branch) },
                onDeleteBranch = { viewModel.deleteBranch(branch) },
                onChangeDefaultUpstreamBranch = { onChangeDefaultUpstreamBranch(branch) },
                onRenameBranch = { onRenameBranch(branch) },
                onCopyBranchNameToClipboard = { viewModel.copyBranchNameToClipboard(branch) },
            )
        }
    }
}

fun LazyListScope.remotes(
    remotesState: RemotesState,
    viewModel: SidePanelViewModel,
    onShowAddEditRemoteDialog: (Remote?) -> Unit,
) {
    val isExpanded = remotesState.isExpanded
    val remotes = remotesState.remotes

    item {
        SideMenuHeader(
            text = stringResource(Res.string.side_pane_remotes_title),
            icon = painterResource(Res.drawable.cloud),
            itemsCount = remotes.count(),
            hoverIcon = {
                IconButton(
                    onClick = { onShowAddEditRemoteDialog(null) },
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(16.dp)
                        .handOnHover(),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.add),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize(),
                        tint = MaterialTheme.colors.onBackground,
                    )
                }
            },
            isExpanded = isExpanded,
            onExpand = { viewModel.onExpandRemotes() }
        )
    }

    if (isExpanded) {
        for (remote in remotes) {
            item {
                Remote(
                    remote = remote,
                    onEditRemote = {
                        val wrapper = remote.remoteInfo.remote
                        onShowAddEditRemoteDialog(wrapper)
                    },
                    onDeleteRemote = { viewModel.deleteRemote(remote.remoteInfo) },
                    onRemoteClicked = { viewModel.onRemoteClicked(remote) },
                    onFetchBranches = { viewModel.onFetchRemoteBranches(remote) },
                )
            }

            if (remote.isExpanded) {
                items(remote.remoteInfo.branchesList) { remoteBranch ->
                    RemoteBranches(
                        remoteBranch = remoteBranch,
                        currentBranch = remotesState.currentBranch,
                        onBranchClicked = { viewModel.selectBranch(remoteBranch) },
                        onCheckoutBranch = { viewModel.checkoutRemoteBranch(remoteBranch) },
                        onDeleteBranch = { viewModel.deleteRemoteBranch(remoteBranch) },
                        onPushRemoteBranch = { viewModel.pushToRemoteBranch(remoteBranch) },
                        onPullRemoteBranch = { viewModel.pullFromRemoteBranch(remoteBranch) },
                        onRebaseRemoteBranch = { viewModel.rebaseBranch(remoteBranch) },
                        onMergeRemoteBranch = { viewModel.mergeBranch(remoteBranch) },
                        onCopyBranchNameToClipboard = { viewModel.copyBranchNameToClipboard(remoteBranch) }
                    )
                }
            }
        }
    }
}


fun LazyListScope.tags(
    tagsState: TagsState,
    viewModel: SidePanelViewModel,
    selectedItem: SelectedItem,
) {
    val isExpanded = tagsState.isExpanded
    val tags = tagsState.tags

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_tags_title),
                icon = painterResource(Res.drawable.tag),
                itemsCount = tags.count(),
                hoverIcon = null,
                isExpanded = isExpanded,
                onExpand = { viewModel.onExpandTags() }
            )
        }
    }

    if (isExpanded) {
        items(tags, key = { it.name }) { tag ->
            Tag(
                tag,
                isSelected = selectedItem is SelectedItem.Ref && selectedItem.ref == tag,
                onTagClicked = { viewModel.selectTag(tag) },
                onCheckoutTag = { viewModel.checkoutTagCommit(tag) },
                onDeleteTag = { viewModel.deleteTag(tag) }
            )
        }
    }
}

fun LazyListScope.stashes(
    stashesState: StashesState,
    viewModel: SidePanelViewModel,
    selectedItem: SelectedItem,
) {
    val isExpanded = stashesState.isExpanded
    val stashes = stashesState.stashes

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_stashes_title),
                icon = painterResource(Res.drawable.stash),
                itemsCount = stashes.count(),
                hoverIcon = null,
                isExpanded = isExpanded,
                onExpand = { viewModel.onExpandSubmodules() }
            )
        }
    }

    if (isExpanded) {
        items(stashes, key = { it.hash }) { stash ->
            Stash(
                stash,
                isSelected = selectedItem is SelectedItem.Stash && selectedItem.commit == stash,
                onClick = { viewModel.selectStash(stash) },
                onApply = { viewModel.applyStash(stash) },
                onPop = { viewModel.popStash(stash) },
                onDelete = { viewModel.deleteStash(stash) },
            )
        }
    }
}

fun LazyListScope.submodules(
    submodulesState: SubmodulesState,
    viewModel: SidePanelViewModel,
    onAddSubmodule: () -> Unit,
) {
    val isExpanded = submodulesState.isExpanded
    val submodules = submodulesState.submodules

    item {
        ContextMenu(
            items = { emptyList() }
        ) {
            SideMenuHeader(
                text = stringResource(Res.string.side_pane_submodules_title),
                icon = painterResource(Res.drawable.topic),
                itemsCount = submodules.count(),
                hoverIcon = {
                    IconButton(
                        onClick = onAddSubmodule,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(16.dp)
                            .handOnHover(),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.add),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize(),
                            tint = MaterialTheme.colors.onBackground,
                        )
                    }
                },
                isExpanded = isExpanded,
                onExpand = { viewModel.onExpandSubmodules() }
            )
        }
    }

    if (isExpanded) {
        items(submodules, key = { it.first }) { submodule ->
            Submodule(
                submodule = submodule,
                onInitializeSubmodule = { viewModel.initializeSubmodule(submodule.first) },
//                onDeinitializeSubmodule = { submodulesViewModel.onDeinitializeSubmodule(submodule.first) },
                onSyncSubmodule = { viewModel.syncSubmodule(submodule.first) },
                onUpdateSubmodule = { viewModel.updateSubmodule(submodule.first) },
                onOpenSubmoduleInTab = { viewModel.onOpenSubmoduleInTab(submodule.first) },
                onDeleteSubmodule = { viewModel.deleteSubmodule(submodule.first) },
            )
        }
    }
}

@Composable
private fun Branch(
    branch: Branch,
    currentBranch: Branch?,
    isSelectedItem: Boolean,
    onBranchClicked: () -> Unit,
    onBranchDoubleClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onChangeDefaultUpstreamBranch: () -> Unit,
    onRenameBranch: () -> Unit,
    onCopyBranchNameToClipboard: () -> Unit,
) {
    val isCurrentBranch = currentBranch?.name == branch.name

    ContextMenu(
        items = {
            branchContextMenuItems(
                branch = branch,
                currentBranch = currentBranch,
                isCurrentBranch = isCurrentBranch,
                isLocal = true,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeBranch,
                onDeleteBranch = onDeleteBranch,
                onRebaseBranch = onRebaseBranch,
                onPushToRemoteBranch = {},
                onPullFromRemoteBranch = {},
                onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
                onRenameBranch = onRenameBranch,
                onCopyBranchNameToClipboard = onCopyBranchNameToClipboard
            )
        }
    ) {
        SideMenuSubentry(
            text = branch.simpleName,
            fontWeight = if (isCurrentBranch) FontWeight.Bold else FontWeight.Normal,
            iconResourcePath = Res.drawable.branch,
            isSelected = isSelectedItem,
            onClick = onBranchClicked,
            onDoubleClick = onBranchDoubleClicked,
        ) {
            if (isCurrentBranch) {
                Text(
                    text = stringResource(Res.string.side_pane_local_branches_current_branch_label),
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}


@Composable
private fun Remote(
    remote: RemoteView,
    onEditRemote: () -> Unit,
    onDeleteRemote: () -> Unit,
    onFetchBranches: () -> Unit,
    onRemoteClicked: () -> Unit,
) {
    ContextMenu(
        items = {
            remoteContextMenu(
                onEdit = onEditRemote,
                onDelete = onDeleteRemote,
                onFetch = onFetchBranches,
            )
        }
    ) {
        SideMenuSubentry(
            text = remote.remoteInfo.remote.name,
            iconResourcePath = Res.drawable.cloud,
            onClick = onRemoteClicked,
            isSelected = false,
        )
    }
}


@Composable
private fun RemoteBranches(
    remoteBranch: Branch,
    currentBranch: Branch?,
    onBranchClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onPushRemoteBranch: () -> Unit,
    onPullRemoteBranch: () -> Unit,
    onRebaseRemoteBranch: () -> Unit,
    onMergeRemoteBranch: () -> Unit,
    onCopyBranchNameToClipboard: () -> Unit,
) {
    ContextMenu(
        items = {
            branchContextMenuItems(
                branch = remoteBranch,
                currentBranch = currentBranch,
                isCurrentBranch = false,
                isLocal = false,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeRemoteBranch,
                onDeleteBranch = {},
                onDeleteRemoteBranch = onDeleteBranch,
                onRebaseBranch = onRebaseRemoteBranch,
                onPushToRemoteBranch = onPushRemoteBranch,
                onPullFromRemoteBranch = onPullRemoteBranch,
                onChangeDefaultUpstreamBranch = {},
                onRenameBranch = {},
                onCopyBranchNameToClipboard = onCopyBranchNameToClipboard
            )
        }
    ) {
        SideMenuSubentry(
            text = remoteBranch.simpleName,
            extraPadding = 24.dp,
            isSelected = false,
            iconResourcePath = Res.drawable.branch,
            onClick = onBranchClicked,
            onDoubleClick = onCheckoutBranch,
        )
    }
}

@Composable
private fun Tag(
    tag: Tag,
    isSelected: Boolean,
    onTagClicked: () -> Unit,
    onCheckoutTag: () -> Unit,
    onDeleteTag: () -> Unit,
) {
    ContextMenu(
        items = {
            tagContextMenuItems(
                onCheckoutTag = onCheckoutTag,
                onDeleteTag = onDeleteTag,
            )
        }
    ) {
        SideMenuSubentry(
            text = tag.simpleName,
            isSelected = isSelected,
            iconResourcePath = Res.drawable.tag,
            onClick = onTagClicked,
        )
    }
}


@Composable
private fun Stash(
    stash: Commit,
    isSelected: Boolean,
    onClick: () -> Unit,
    onApply: () -> Unit,
    onPop: () -> Unit,
    onDelete: () -> Unit,
) {
    ContextMenu(
        items = {
            stashesContextMenuItems(
                onApply = onApply,
                onPop = onPop,
                onDelete = onDelete,
            )
        }
    ) {
        SideMenuSubentry(
            text = stash.shortMessage,
            isSelected = isSelected,
            iconResourcePath = Res.drawable.stash,
            onClick = onClick,
        )
    }
}

@Composable
private fun Submodule(
    submodule: Pair<String, SubmoduleStatus>,
    onInitializeSubmodule: () -> Unit,
//    onDeinitializeSubmodule: () -> Unit,
    onSyncSubmodule: () -> Unit,
    onUpdateSubmodule: () -> Unit,
    onOpenSubmoduleInTab: () -> Unit,
    onDeleteSubmodule: () -> Unit,
) {
    ContextMenu(
        items = {
            submoduleContextMenuItems(
                submodule.second,
                onInitializeSubmodule = onInitializeSubmodule,
//                onDeinitializeSubmodule = onDeinitializeSubmodule,
                onSyncSubmodule = onSyncSubmodule,
                onUpdateSubmodule = onUpdateSubmodule,
                onOpenSubmoduleInTab = onOpenSubmoduleInTab,
                onDeleteSubmodule = onDeleteSubmodule,
            )
        }
    ) {
        SideMenuSubentry(
            text = submodule.first,
            iconResourcePath = Res.drawable.topic,
            isSelected = false,
            onClick = {
                if (submodule.second.type.isValid()) {
                    onOpenSubmoduleInTab()
                }
            },
        ) {
            val stateName = submodule.second.type.toString()
            DelayedTooltip(stateName) {
                Text(
                    text = stateName.first().toString(),
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}