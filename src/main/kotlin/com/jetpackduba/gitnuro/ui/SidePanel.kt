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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.isLocal
import com.jetpackduba.gitnuro.extensions.isValid
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.ui.components.*
import com.jetpackduba.gitnuro.ui.context_menu.*
import com.jetpackduba.gitnuro.ui.dialogs.AddSubmodulesDialog
import com.jetpackduba.gitnuro.ui.dialogs.EditRemotesDialog
import com.jetpackduba.gitnuro.ui.dialogs.SetDefaultUpstreamBranchDialog
import com.jetpackduba.gitnuro.viewmodels.sidepanel.*
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.submodule.SubmoduleStatus

@Composable
fun SidePanel(
    sidePanelViewModel: SidePanelViewModel = gitnuroViewModel(),
    branchesViewModel: BranchesViewModel = sidePanelViewModel.branchesViewModel,
    remotesViewModel: RemotesViewModel = sidePanelViewModel.remotesViewModel,
    tagsViewModel: TagsViewModel = sidePanelViewModel.tagsViewModel,
    stashesViewModel: StashesViewModel = sidePanelViewModel.stashesViewModel,
    submodulesViewModel: SubmodulesViewModel = sidePanelViewModel.submodulesViewModel,
) {
    var filter by remember(sidePanelViewModel) { mutableStateOf(sidePanelViewModel.filter.value) }
    val selectedItem by sidePanelViewModel.selectedItem.collectAsState()

    val branchesState by branchesViewModel.branchesState.collectAsState()
    val remotesState by remotesViewModel.remoteState.collectAsState()
    val tagsState by tagsViewModel.tagsState.collectAsState()
    val stashesState by stashesViewModel.stashesState.collectAsState()
    val submodulesState by submodulesViewModel.submodules.collectAsState()

    var showEditRemotesDialog by remember { mutableStateOf(false) }
    val (branchToChangeUpstream, setBranchToChangeUpstream) = remember { mutableStateOf<Ref?>(null) }
    var showEditSubmodulesDialog by remember { mutableStateOf(false) }

    Column {
        FilterTextField(
            value = filter,
            onValueChange = { newValue ->
                filter = newValue
                sidePanelViewModel.newFilter(newValue)
            },
            modifier = Modifier
                .padding(start = 8.dp)
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
                onShowEditRemotesDialog = { showEditRemotesDialog = true },
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

    if (showEditRemotesDialog) {
        EditRemotesDialog(
            remotesViewModel = remotesViewModel,
            onDismiss = {
                showEditRemotesDialog = false
            },
        )
    }

    if (branchToChangeUpstream != null) {
        SetDefaultUpstreamBranchDialog(
            viewModel = gitnuroDynamicViewModel(),
            branch = branchToChangeUpstream,
            onClose = { setBranchToChangeUpstream(null) }
        )
    }

    if (showEditSubmodulesDialog) {
        AddSubmodulesDialog(
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
fun FilterTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    AdjustableOutlinedTextField(
        value = value,
        hint = "Search for branches, tags & more",
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = LocalTextStyle.current.copy(
            fontSize = MaterialTheme.typography.body2.fontSize,
            color = MaterialTheme.colors.onBackground,
        ),
        singleLine = true,
        leadingIcon = {
            Icon(
                painterResource(AppIcons.SEARCH),
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
                        painterResource(AppIcons.CLOSE),
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
                text = "Local branches",
                icon = painterResource(AppIcons.BRANCH),
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
                onPushToRemoteBranch = { branchesViewModel.pushToRemoteBranch(branch) },
                onPullFromRemoteBranch = { branchesViewModel.pullFromRemoteBranch(branch) }
            ) { onChangeDefaultUpstreamBranch(branch) }
        }
    }
}

fun LazyListScope.remotes(
    remotesState: RemotesState,
    remotesViewModel: RemotesViewModel,
    onShowEditRemotesDialog: () -> Unit,
) {
    val isExpanded = remotesState.isExpanded
    val remotes = remotesState.remotes

    item {
        SideMenuHeader(
            text = "Remotes",
            icon = painterResource(AppIcons.CLOUD),
            itemsCount = remotes.count(),
            hoverIcon = {
                IconButton(
                    onClick = onShowEditRemotesDialog,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(16.dp)
                        .handOnHover(),
                ) {
                    Icon(
                        painter = painterResource(AppIcons.SETTINGS),
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
                    onRemoteClicked = { remotesViewModel.onRemoteClicked(remote) }
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
                text = "Tags",
                icon = painterResource(AppIcons.TAG),
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
                onCheckoutTag = { tagsViewModel.checkoutRef(tag) },
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
                text = "Stashes",
                icon = painterResource(AppIcons.STASH),
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
                text = "Submodules",
                icon = painterResource(AppIcons.TOPIC),
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
                            painter = painterResource(AppIcons.ADD),
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
    onPushToRemoteBranch: () -> Unit,
    onPullFromRemoteBranch: () -> Unit,
    onChangeDefaultUpstreamBranch: () -> Unit,
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
                onPushToRemoteBranch = onPushToRemoteBranch,
                onPullFromRemoteBranch = onPullFromRemoteBranch,
                onChangeDefaultUpstreamBranch = onChangeDefaultUpstreamBranch
            )
        }
    ) {
        SideMenuSubentry(
            text = branch.simpleName,
            iconResourcePath = AppIcons.BRANCH,
            isSelected = isSelectedItem,
            onClick = onBranchClicked,
            onDoubleClick = onBranchDoubleClicked,
        ) {
            if (isCurrentBranch) {
                Text(
                    text = "HEAD",
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
    onRemoteClicked: () -> Unit,
) {
    SideMenuSubentry(
        text = remote.remoteInfo.remoteConfig.name,
        iconResourcePath = AppIcons.CLOUD,
        onClick = onRemoteClicked,
        isSelected = false,
    )
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
) {
    ContextMenu(
        items = {
            branchContextMenuItems(
                branch = remoteBranch,
                currentBranch = currentBranch,
                isCurrentBranch = false,
                isLocal = false,
                onCheckoutBranch = onCheckoutBranch,
                onMergeBranch = {},
                onDeleteBranch = {},
                onDeleteRemoteBranch = onDeleteBranch,
                onRebaseBranch = {},
                onPushToRemoteBranch = onPushRemoteBranch,
                onPullFromRemoteBranch = onPullRemoteBranch,
                onChangeDefaultUpstreamBranch = {},
            )
        }
    ) {
        SideMenuSubentry(
            text = remoteBranch.simpleName,
            extraPadding = 24.dp,
            isSelected = false,
            iconResourcePath = AppIcons.BRANCH,
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
            iconResourcePath = AppIcons.TAG,
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
            iconResourcePath = AppIcons.STASH,
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
            iconResourcePath = AppIcons.TOPIC,
            isSelected = false,
            onClick = {
                if (submodule.second.type.isValid()) {
                    onOpenSubmoduleInTab()
                }
            },
        ) {
            val stateName = submodule.second.type.toString()
            Tooltip(stateName) {
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