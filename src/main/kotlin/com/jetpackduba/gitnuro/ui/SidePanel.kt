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
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.isLocal
import com.jetpackduba.gitnuro.extensions.isValid
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.generated.resources.*
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.side_pane_local_branches_title
import com.jetpackduba.gitnuro.generated.resources.side_pane_remotes_title
import com.jetpackduba.gitnuro.generated.resources.side_pane_search_hint
import com.jetpackduba.gitnuro.models.RemoteWrapper
import com.jetpackduba.gitnuro.models.newRemoteWrapper
import com.jetpackduba.gitnuro.models.toRemoteWrapper
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.SideMenuHeader
import com.jetpackduba.gitnuro.ui.components.SideMenuSubentry
import com.jetpackduba.gitnuro.ui.components.tooltip.DelayedTooltip
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.ui.dialogs.AddEditRemoteDialog
import com.jetpackduba.gitnuro.ui.dialogs.AddSubmodulesDialog
import com.jetpackduba.gitnuro.ui.dialogs.SetDefaultUpstreamBranchDialog
import com.jetpackduba.gitnuro.viewmodels.ChangeUpstreamBranchDialogViewModel
import com.jetpackduba.gitnuro.viewmodels.sidepanel.*
import kotlinx.coroutines.flow.collectLatest
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.submodule.SubmoduleStatus
import org.jetbrains.compose.resources.stringResource

@Composable
fun SidePanel(
    sidePanelViewModel: SidePanelViewModel,
    changeUpstreamBranchDialogViewModel: () -> ChangeUpstreamBranchDialogViewModel,
    submoduleDialogViewModel: () -> SubmoduleDialogViewModel,
    branchesViewModel: BranchesViewModel = sidePanelViewModel.branchesViewModel,
    remotesViewModel: RemotesViewModel = sidePanelViewModel.remotesViewModel,
    tagsViewModel: TagsViewModel = sidePanelViewModel.tagsViewModel,
    stashesViewModel: StashesViewModel = sidePanelViewModel.stashesViewModel,
    submodulesViewModel: SubmodulesViewModel = sidePanelViewModel.submodulesViewModel,
) {
    val filter by sidePanelViewModel.filter.collectAsState()
    val selectedItem by sidePanelViewModel.selectedItem.collectAsState()

    val branchesState by branchesViewModel.branchesState.collectAsState()
    val remotesState by remotesViewModel.remoteState.collectAsState()
    val tagsState by tagsViewModel.tagsState.collectAsState()
    val stashesState by stashesViewModel.stashesState.collectAsState()
    val submodulesState by submodulesViewModel.submodules.collectAsState()

    val (showAddEditRemote, setShowAddEditRemote) = remember { mutableStateOf<RemoteWrapper?>(null) }
    val (branchToChangeUpstream, setBranchToChangeUpstream) = remember { mutableStateOf<Ref?>(null) }
    var showEditSubmodulesDialog by remember { mutableStateOf(false) }
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
                branchesViewModel = branchesViewModel,
                onChangeDefaultUpstreamBranch = { setBranchToChangeUpstream(it) }
            )

            remotes(
                remotesState = remotesState,
                remotesViewModel = remotesViewModel,
                onShowAddEditRemoteDialog = { setShowAddEditRemote(it) },
            )

            tags(
                tagsState = tagsState,
                selectedItem = selectedItem,
                tagsViewModel = tagsViewModel,
            )

            stashes(
                stashesState = stashesState,
                selectedItem = selectedItem,
                stashesViewModel = stashesViewModel,
            )

            submodules(
                submodulesState = submodulesState,
                submodulesViewModel = submodulesViewModel,
                onShowEditSubmodulesDialog = { showEditSubmodulesDialog = true },
            )
        }
    }

    if (showAddEditRemote != null) {
        AddEditRemoteDialog(
            remotesViewModel = remotesViewModel,
            remoteWrapper = showAddEditRemote,
            onDismiss = {
                setShowAddEditRemote(null)
            },
        )
    }

    if (branchToChangeUpstream != null) {
        SetDefaultUpstreamBranchDialog(
            viewModel = changeUpstreamBranchDialogViewModel(),
            branch = branchToChangeUpstream,
            onClose = { setBranchToChangeUpstream(null) }
        )
    }

    if (showEditSubmodulesDialog) {
        AddSubmodulesDialog(
            viewModel = submoduleDialogViewModel(),
            onCancel = {
                showEditSubmodulesDialog = false
            },
            onAccept = { repository, directory ->
                submodulesViewModel.onCreateSubmodule(repository, directory)
                showEditSubmodulesDialog = false
            }
        )
    }
}

@Composable
fun FilterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
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
    branchesViewModel: BranchesViewModel,
    onChangeDefaultUpstreamBranch: (Ref) -> Unit,
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
                onExpand = { branchesViewModel.onExpand() }
            )
        }
    }

    if (isExpanded) {
        items(branches, key = { it.name }) { branch ->
            Branch(
                branch = branch,
                isSelectedItem = selectedItem is SelectedItem.Ref && selectedItem.ref == branch,
                currentBranch = currentBranch,
                onBranchClicked = { branchesViewModel.selectBranch(branch) },
                onBranchDoubleClicked = { branchesViewModel.checkoutRef(branch) },
                onCheckoutBranch = { branchesViewModel.checkoutRef(branch) },
                onMergeBranch = { branchesViewModel.mergeBranch(branch) },
                onRebaseBranch = { branchesViewModel.rebaseBranch(branch) },
                onDeleteBranch = { branchesViewModel.deleteBranch(branch) },
                onChangeDefaultUpstreamBranch = { onChangeDefaultUpstreamBranch(branch) },
                onCopyBranchNameToClipboard = { branchesViewModel.copyBranchNameToClipboard(branch) },
            )
        }
    }
}

fun LazyListScope.remotes(
    remotesState: RemotesState,
    remotesViewModel: RemotesViewModel,
    onShowAddEditRemoteDialog: (RemoteWrapper) -> Unit,
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
                    onClick = { onShowAddEditRemoteDialog(newRemoteWrapper()) },
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
            onExpand = { remotesViewModel.onExpand() }
        )
    }

    if (isExpanded) {
        for (remote in remotes) {
            item {
                Remote(
                    remote = remote,
                    onEditRemote = {
                        val wrapper = remote.remoteInfo.remoteConfig.toRemoteWrapper()
                        onShowAddEditRemoteDialog(wrapper)
                    },
                    onDeleteRemote = { remotesViewModel.deleteRemote(remote.remoteInfo.remoteConfig.name) },
                    onRemoteClicked = { remotesViewModel.onRemoteClicked(remote) },
                )
            }

            if (remote.isExpanded) {
                items(remote.remoteInfo.branchesList) { remoteBranch ->
                    RemoteBranches(
                        remoteBranch = remoteBranch,
                        currentBranch = remotesState.currentBranch,
                        onBranchClicked = { remotesViewModel.selectBranch(remoteBranch) },
                        onCheckoutBranch = { remotesViewModel.checkoutRemoteBranch(remoteBranch) },
                        onDeleteBranch = { remotesViewModel.deleteRemoteBranch(remoteBranch) },
                        onPushRemoteBranch = { remotesViewModel.pushToRemoteBranch(remoteBranch) },
                        onPullRemoteBranch = { remotesViewModel.pullFromRemoteBranch(remoteBranch) },
                        onRebaseRemoteBranch = { remotesViewModel.rebaseBranch(remoteBranch) },
                        onMergeRemoteBranch = { remotesViewModel.mergeBranch(remoteBranch) },
                        onCopyBranchNameToClipboard = { remotesViewModel.copyBranchNameToClipboard(remoteBranch) }
                    )
                }
            }
        }
    }
}


fun LazyListScope.tags(
    tagsState: TagsState,
    tagsViewModel: TagsViewModel,
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
                onExpand = { tagsViewModel.onExpand() }
            )
        }
    }

    if (isExpanded) {
        items(tags, key = { it.name }) { tag ->
            Tag(
                tag,
                isSelected = selectedItem is SelectedItem.Ref && selectedItem.ref == tag,
                onTagClicked = { tagsViewModel.selectTag(tag) },
                onCheckoutTag = { tagsViewModel.checkoutTagCommit(tag) },
                onDeleteTag = { tagsViewModel.deleteTag(tag) }
            )
        }
    }
}

fun LazyListScope.stashes(
    stashesState: StashesState,
    stashesViewModel: StashesViewModel,
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
                onExpand = { stashesViewModel.onExpand() }
            )
        }
    }

    if (isExpanded) {
        items(stashes, key = { it.name }) { stash ->
            Stash(
                stash,
                isSelected = selectedItem is SelectedItem.Stash && selectedItem.revCommit == stash,
                onClick = { stashesViewModel.selectStash(stash) },
                onApply = { stashesViewModel.applyStash(stash) },
                onPop = { stashesViewModel.popStash(stash) },
                onDelete = { stashesViewModel.deleteStash(stash) },
            )
        }
    }
}

fun LazyListScope.submodules(
    submodulesState: SubmodulesState,
    submodulesViewModel: SubmodulesViewModel,
    onShowEditSubmodulesDialog: () -> Unit,
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
                        onClick = onShowEditSubmodulesDialog,
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
                onExpand = { submodulesViewModel.onExpand() }
            )
        }
    }

    if (isExpanded) {
        items(submodules, key = { it.first }) { submodule ->
            Submodule(
                submodule = submodule,
                onInitializeSubmodule = { submodulesViewModel.initializeSubmodule(submodule.first) },
//                onDeinitializeSubmodule = { submodulesViewModel.onDeinitializeSubmodule(submodule.first) },
                onSyncSubmodule = { submodulesViewModel.onSyncSubmodule(submodule.first) },
                onUpdateSubmodule = { submodulesViewModel.onUpdateSubmodule(submodule.first) },
                onOpenSubmoduleInTab = { submodulesViewModel.onOpenSubmoduleInTab(submodule.first) },
                onDeleteSubmodule = { submodulesViewModel.onDeleteSubmodule(submodule.first) },
            )
        }
    }
}

@Composable
private fun Branch(
    branch: Ref,
    currentBranch: Ref?,
    isSelectedItem: Boolean,
    onBranchClicked: () -> Unit,
    onBranchDoubleClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onMergeBranch: () -> Unit,
    onRebaseBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onChangeDefaultUpstreamBranch: () -> Unit,
    onCopyBranchNameToClipboard: () -> Unit,
) {
    val isCurrentBranch = currentBranch?.name == branch.name

    ContextMenu(
        items = {
            branchContextMenuItems(
                branch = branch,
                currentBranch = currentBranch,
                isCurrentBranch = isCurrentBranch,
                isLocal = branch.isLocal,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = onMergeBranch,
                onDeleteBranch = onDeleteBranch,
                onRebaseBranch = onRebaseBranch,
                onPushToRemoteBranch = {},
                onPullFromRemoteBranch = {},
                onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch,
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
    onRemoteClicked: () -> Unit,
) {
    ContextMenu(
        items = {
            remoteContextMenu(
                onEdit = onEditRemote,
                onDelete = onDeleteRemote,
            )
        }
    ) {
        SideMenuSubentry(
            text = remote.remoteInfo.remoteConfig.name,
            iconResourcePath = Res.drawable.cloud,
            onClick = onRemoteClicked,
            isSelected = false,
        )
    }
}


@Composable
private fun RemoteBranches(
    remoteBranch: Ref,
    currentBranch: Ref?,
    onBranchClicked: () -> Unit,
    onCheckoutBranch: () -> Unit,
    onDeleteBranch: () -> Unit,
    onPushRemoteBranch: () -> Unit,
    onPullRemoteBranch: () -> Unit,
    onRebaseRemoteBranch: () -> Unit,
    onMergeRemoteBranch: () -> Unit,
    onCopyBranchNameToClipboard: () -> Unit
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
    tag: Ref,
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
    stash: RevCommit,
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