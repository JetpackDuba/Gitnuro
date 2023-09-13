@file:OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.rebase.RebaseInteractiveState
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.*
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.EntryType
import com.jetpackduba.gitnuro.ui.context_menu.statusEntriesContextMenuItems
import com.jetpackduba.gitnuro.ui.dialogs.CommitAuthorDialog
import com.jetpackduba.gitnuro.viewmodels.CommitterDataRequestState
import com.jetpackduba.gitnuro.viewmodels.StageState
import com.jetpackduba.gitnuro.viewmodels.StatusViewModel
import org.eclipse.jgit.lib.RepositoryState

@Composable
fun UncommitedChanges(
    statusViewModel: StatusViewModel = gitnuroViewModel(),
    selectedEntryType: DiffEntryType?,
    repositoryState: RepositoryState,
    onStagedDiffEntrySelected: (StatusEntry?) -> Unit,
    onUnstagedDiffEntrySelected: (StatusEntry) -> Unit,
    onBlameFile: (String) -> Unit,
    onHistoryFile: (String) -> Unit,
) {
    val stageStatus = statusViewModel.stageState.collectAsState().value
    val swapUncommitedChanges by statusViewModel.swapUncommitedChanges.collectAsState()
    var commitMessage by remember(statusViewModel) { mutableStateOf(statusViewModel.savedCommitMessage.message) }
    val stagedListState by statusViewModel.stagedLazyListState.collectAsState()
    val unstagedListState by statusViewModel.unstagedLazyListState.collectAsState()
    val isAmend by statusViewModel.isAmend.collectAsState()
    val isAmendRebaseInteractive by statusViewModel.isAmendRebaseInteractive.collectAsState()
    val committerDataRequestState = statusViewModel.committerDataRequestState.collectAsState()
    val committerDataRequestStateValue = committerDataRequestState.value
    val rebaseInteractiveState = statusViewModel.rebaseInteractiveState.collectAsState().value

    val showSearchStaged by statusViewModel.showSearchStaged.collectAsState()
    val searchFilterStaged by statusViewModel.searchFilterStaged.collectAsState()
    val showSearchUnstaged by statusViewModel.showSearchUnstaged.collectAsState()
    val searchFilterUnstaged by statusViewModel.searchFilterUnstaged.collectAsState()

    val isAmenableRebaseInteractive =
        repositoryState.isRebasing && rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits && rebaseInteractiveState.isCurrentStepAmenable

    val staged: List<StatusEntry>
    val unstaged: List<StatusEntry>
    val isLoading: Boolean

    if (stageStatus is StageState.Loaded) {
        staged = stageStatus.stagedFiltered
        unstaged = stageStatus.unstagedFiltered
        isLoading = stageStatus.isPartiallyReloading
    } else {
        staged = listOf()
        unstaged = listOf() // return empty lists if still loading
        isLoading = true
    }

    val doCommit = {
        statusViewModel.commit(commitMessage)
        onStagedDiffEntrySelected(null)
        commitMessage = ""
    }

    val canCommit = commitMessage.isNotEmpty() && staged.isNotEmpty()
    val canAmend = commitMessage.isNotEmpty() && statusViewModel.hasPreviousCommits

    LaunchedEffect(statusViewModel) {
        statusViewModel.commitMessageChangesFlow.collect { newCommitMessage ->
            commitMessage = newCommitMessage
        }
    }

    if (committerDataRequestStateValue is CommitterDataRequestState.WaitingInput) {
        CommitAuthorDialog(
            committerDataRequestStateValue.authorInfo,
            onClose = { statusViewModel.onRejectCommitterData() },
            onAccept = { newAuthorInfo, persist ->
                statusViewModel.onAcceptCommitterData(newAuthorInfo, persist)
            },
        )
    }

    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxWidth(),
    ) {
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            val stagedView: @Composable () -> Unit = {
                EntriesList(
                    modifier = Modifier
                        .weight(5f)
                        .padding(bottom = 4.dp)
                        .fillMaxWidth(),
                    title = "Staged",
                    allActionTitle = "Unstage all",
                    actionTitle = "Unstage",
                    actionIcon = AppIcons.REMOVE_DONE,
                    selectedEntryType = if (selectedEntryType is DiffEntryType.StagedDiff) selectedEntryType else null,
                    actionColor = MaterialTheme.colors.error,
                    actionTextColor = MaterialTheme.colors.onError,
                    statusEntries = staged,
                    lazyListState = stagedListState,
                    onDiffEntrySelected = onStagedDiffEntrySelected,
                    showSearch = showSearchStaged,
                    searchFilter = searchFilterStaged,
                    onSearchFilterToggled = {
                        statusViewModel.onSearchFilterToggledStaged(it)
                    },
                    onSearchFilterChanged = {
                        statusViewModel.onSearchFilterChangedStaged(it)
                    },
                    onDiffEntryOptionSelected = {
                        statusViewModel.unstage(it)
                    },
                    onGenerateContextMenu = { statusEntry ->
                        statusEntriesContextMenuItems(
                            statusEntry = statusEntry,
                            entryType = EntryType.STAGED,
                            onBlame = { onBlameFile(statusEntry.filePath) },
                            onReset = { statusViewModel.resetStaged(statusEntry) },
                            onHistory = { onHistoryFile(statusEntry.filePath) },
                        )
                    },
                    onAllAction = {
                        statusViewModel.unstageAll()
                    },
                )
            }

            val unstagedView: @Composable () -> Unit = {
                EntriesList(
                    modifier = Modifier
                        .weight(5f)
                        .padding(bottom = 4.dp)
                        .fillMaxWidth(),
                    title = "Unstaged",
                    actionTitle = "Stage",
                    actionIcon = AppIcons.DONE,
                    selectedEntryType = if (selectedEntryType is DiffEntryType.UnstagedDiff) selectedEntryType else null,
                    actionColor = MaterialTheme.colors.primary,
                    actionTextColor = MaterialTheme.colors.onPrimary,
                    statusEntries = unstaged,
                    lazyListState = unstagedListState,
                    onDiffEntrySelected = onUnstagedDiffEntrySelected,
                    showSearch = showSearchUnstaged,
                    searchFilter = searchFilterUnstaged,
                    onSearchFilterToggled = {
                        statusViewModel.onSearchFilterToggledUnstaged(it)
                    },
                    onSearchFilterChanged = {
                        statusViewModel.onSearchFilterChangedUnstaged(it)
                    },
                    onDiffEntryOptionSelected = {
                        statusViewModel.stage(it)
                    },
                    onGenerateContextMenu = { statusEntry ->
                        statusEntriesContextMenuItems(
                            statusEntry = statusEntry,
                            entryType = EntryType.UNSTAGED,
                            onBlame = { onBlameFile(statusEntry.filePath) },
                            onHistory = { onHistoryFile(statusEntry.filePath) },
                            onReset = { statusViewModel.resetUnstaged(statusEntry) },
                            onDelete = {
                                statusViewModel.deleteFile(statusEntry)
                            },
                        )
                    },
                    onAllAction = {
                        statusViewModel.stageAll()
                    },
                    allActionTitle = "Stage all",
                )
            }

            if (swapUncommitedChanges) {
                unstagedView()
                stagedView()
            } else {
                stagedView()
                unstagedView()
            }
        }

        Column(
            modifier = Modifier
                .height(192.dp)
                .fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = true)
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.matchesBinding(KeybindingOption.TEXT_ACCEPT) && (canCommit || isAmend && canAmend)) {
                            doCommit()
                            true
                        } else
                            false
                    },
                value = commitMessage,
                onValueChange = {
                    commitMessage = it

                    statusViewModel.updateCommitMessage(it)
                },
                enabled = !repositoryState.isRebasing || isAmenableRebaseInteractive,
                label = {
                    val text = if (repositoryState.isRebasing && !isAmenableRebaseInteractive) {
                        "Commit message (read-only)"
                    } else {
                        "Write your commit message here"
                    }

                    Text(
                        text = text,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primaryVariant,
                    )
                },
                colors = textFieldColors(),
                textStyle = MaterialTheme.typography.body1,
            )

            when {
                repositoryState.isMerging -> MergeButtons(
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = {
                        statusViewModel.resetRepoState()
                        statusViewModel.updateCommitMessage("")
                    },
                    onMerge = { doCommit() }
                )

                repositoryState.isRebasing && rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits -> RebasingButtons(
                    canContinue = staged.isNotEmpty() || unstaged.isNotEmpty() || (isAmenableRebaseInteractive && isAmendRebaseInteractive && commitMessage.isNotEmpty()),
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = {
                        statusViewModel.abortRebase()
                        statusViewModel.updateCommitMessage("")
                    },
                    onContinue = { statusViewModel.continueRebase(commitMessage) },
                    onSkip = { statusViewModel.skipRebase() },
                    isAmendable = rebaseInteractiveState.isCurrentStepAmenable,
                    isAmend = isAmendRebaseInteractive,
                    onAmendChecked = { isAmend ->
                        statusViewModel.amendRebaseInteractive(isAmend)
                    }
                )

                repositoryState.isCherryPicking -> CherryPickingButtons(
                    haveConflictsBeenSolved = unstaged.isEmpty(),
                    onAbort = {
                        statusViewModel.resetRepoState()
                        statusViewModel.updateCommitMessage("")
                    },
                    onCommit = {
                        doCommit()
                    }
                )

                repositoryState.isReverting -> RevertingButtons(
                    haveConflictsBeenSolved = unstaged.none { it.statusType == StatusType.CONFLICTING },
                    canCommit = commitMessage.isNotBlank(),
                    onAbort = {
                        statusViewModel.resetRepoState()
                        statusViewModel.updateCommitMessage("")
                    },
                    onCommit = {
                        doCommit()
                    }
                )

                else -> UncommitedChangesButtons(
                    canCommit = canCommit,
                    canAmend = canAmend,
                    isAmend = isAmend,
                    onAmendChecked = { isAmend ->
                        statusViewModel.amend(isAmend)
                    },
                    onCommit = doCommit,
                )
            }
        }
    }

}

@Composable
fun UncommitedChangesButtons(
    canCommit: Boolean,
    canAmend: Boolean,
    isAmend: Boolean,
    onAmendChecked: (Boolean) -> Unit,
    onCommit: () -> Unit
) {
    val buttonText = if (isAmend)
        "Amend"
    else
        "Commit"

    Column {
        CheckboxText(
            value = isAmend,
            onCheckedChange = { onAmendChecked(!isAmend) },
            text = "Amend previous commit"
        )
        Row(
            modifier = Modifier
                .padding(top = 2.dp)
        ) {
            ConfirmationButton(
                text = buttonText,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                onClick = {
                    onCommit()
                },
                enabled = canCommit || (canAmend && isAmend),
                shape = RoundedCornerShape(4.dp)
            )
        }
    }
}

@Composable
fun MergeButtons(
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onMerge: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .fillMaxHeight(),
            onClick = onAbort
        )

        ConfirmationButton(
            text = "Merge",
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = haveConflictsBeenSolved,
            onClick = onMerge,
        )
    }
}

@Composable
fun CherryPickingButtons(
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onCommit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .fillMaxHeight(),
            onClick = onAbort
        )

        ConfirmationButton(
            text = "Commit",
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun RebasingButtons(
    canContinue: Boolean,
    isAmendable: Boolean,
    isAmend: Boolean,
    onAmendChecked: (Boolean) -> Unit,
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
) {
    Column {
        if (isAmendable) {
            CheckboxText(
                value = isAmend,
                onCheckedChange = { onAmendChecked(!isAmend) },
                text = "Amend previous commit"
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(36.dp)
        ) {
            AbortButton(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .fillMaxHeight(),
                onClick = onAbort
            )

            if (canContinue) {
                ConfirmationButton(
                    text = "Continue",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .fillMaxHeight(),
                    enabled = haveConflictsBeenSolved,
                    onClick = onContinue,
                )
            } else {
                ConfirmationButton(
                    text = "Skip",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .fillMaxHeight(),
                    onClick = onSkip,
                )
            }
        }
    }
}

@Composable
fun RevertingButtons(
    canCommit: Boolean,
    haveConflictsBeenSolved: Boolean,
    onAbort: () -> Unit,
    onCommit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .height(36.dp)
    ) {
        AbortButton(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            onClick = onAbort
        )

        ConfirmationButton(
            text = "Continue",
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .fillMaxHeight(),
            enabled = canCommit && haveConflictsBeenSolved,
            onClick = onCommit,
        )
    }
}

@Composable
fun AbortButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clickable { onClick() }
            .focusable(false)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colors.abortButton),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Abort",
            style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.onError),
        )
    }
}

@Composable
fun ConfirmationButton(
    text: String,
    modifier: Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: () -> Unit,
) {
    val (backgroundColor, contentColor) = if (enabled) {
        (MaterialTheme.colors.primary to MaterialTheme.colors.onPrimary)
    } else {
        (MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            .compositeOver(MaterialTheme.colors.surface) to MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled))
    }

    Box(
        modifier = modifier
            .clickable { if (enabled) onClick() }
            .focusable(false) // TODO this and the abort button should be focusable (show some kind of border when focused?)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.body1.copy(color = contentColor),
        )
    }
}

@Composable
private fun EntriesList(
    modifier: Modifier,
    title: String,
    actionTitle: String,
    actionColor: Color,
    actionTextColor: Color,
    actionIcon: String,
    showSearch: Boolean,
    searchFilter: TextFieldValue,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    statusEntries: List<StatusEntry>,
    lazyListState: LazyListState,
    onDiffEntrySelected: (StatusEntry) -> Unit,
    onDiffEntryOptionSelected: (StatusEntry) -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
    onAllAction: () -> Unit,
    allActionTitle: String,
    selectedEntryType: DiffEntryType?,
) {
    val searchFocusRequester = remember { FocusRequester() }
    val headerHoverInteraction = remember { MutableInteractionSource() }
    val isHeaderHovered by headerHoverInteraction.collectIsHoveredAsState()

    /**
     * State used to prevent the text field from getting the focus when returning from another tab
     */
    var requestFocus by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .height(34.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.colors.tertiarySurface)
                .hoverable(headerHoverInteraction),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp)
                    .weight(1f),
                text = title,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Left,
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )

            IconButton(
                onClick = {
                    onSearchFilterToggled(!showSearch)

                    if (!showSearch)
                        requestFocus = true
                },
                modifier = Modifier.handOnHover()
            ) {
                Icon(
                    painter = painterResource(AppIcons.SEARCH),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colors.onBackground,
                )
            }

            SecondaryButtonCompactable(
                text = allActionTitle,
                icon = actionIcon,
                isParentHovered = isHeaderHovered,
                backgroundButton = actionColor,
                onBackgroundColor = actionTextColor,
                onClick = onAllAction,
                modifier = Modifier.padding(start = 4.dp, end = 16.dp),
            )
        }

        if (showSearch) {
            SearchTextField(
                searchFilter = searchFilter,
                onSearchFilterChanged = onSearchFilterChanged,
                searchFocusRequester = searchFocusRequester,
                onClose = { onSearchFilterToggled(false) },
            )
        }

        LaunchedEffect(showSearch, requestFocus) {
            if (showSearch && requestFocus) {
                searchFocusRequester.requestFocus()
                requestFocus = false
            }
        }

        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            state = lazyListState,
        ) {
            items(statusEntries, key = { it.filePath }) { statusEntry ->
                val isEntrySelected = selectedEntryType != null &&
                        selectedEntryType is DiffEntryType.UncommitedDiff && // Added for smartcast
                        selectedEntryType.statusEntry == statusEntry
                FileEntry(
                    statusEntry = statusEntry,
                    isSelected = isEntrySelected,
                    actionTitle = actionTitle,
                    actionColor = actionColor,
                    onClick = {
                        onDiffEntrySelected(statusEntry)
                    },
                    onButtonClick = {
                        onDiffEntryOptionSelected(statusEntry)
                    },
                    onGenerateContextMenu = onGenerateContextMenu,
                )
            }
        }
    }
}

@Composable
private fun FileEntry(
    statusEntry: StatusEntry,
    isSelected: Boolean,
    actionTitle: String,
    actionColor: Color,
    onClick: () -> Unit,
    onButtonClick: () -> Unit,
    onGenerateContextMenu: (StatusEntry) -> List<ContextMenuElement>,
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .handMouseClickable { onClick() }
            .onDoubleClick(onButtonClick)
            .fillMaxWidth()
            .hoverable(hoverInteraction)
    ) {
        ContextMenu(
            items = {
                onGenerateContextMenu(statusEntry)
            },
        ) {
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .backgroundIf(isSelected, MaterialTheme.colors.backgroundSelected),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Icon(
                    imageVector = statusEntry.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(16.dp),
                    tint = statusEntry.iconColor,
                )

                if (statusEntry.parentDirectoryPath.isNotEmpty()) {
                    Text(
                        text = statusEntry.parentDirectoryPath.removeSuffix("/"),
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )

                    Text(
                        text = "/",
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.body2,
                        overflow = TextOverflow.Visible,
                        color = MaterialTheme.colors.onBackgroundSecondary,
                    )
                }
                Text(
                    text = statusEntry.fileName,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.CenterEnd),
            visible = isHovered,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            SecondaryButton(
                onClick = onButtonClick,
                text = actionTitle,
                backgroundButton = actionColor,
                modifier = Modifier
                    .padding(horizontal = 16.dp),
            )
        }
    }
}

internal fun placeRightOrBottom(
    totalSize: Int,
    size: IntArray,
    outPosition: IntArray,
    reverseInput: Boolean
) {
    val consumedSize = size.fold(0) { a, b -> a + b }
    var current = totalSize - consumedSize
    size.forEachIndexed(reverseInput) { index, it ->
        outPosition[index] = current
        current += it
    }
}

private inline fun IntArray.forEachIndexed(reversed: Boolean, action: (Int, Int) -> Unit) {
    if (!reversed) {
        forEachIndexed(action)
    } else {
        for (i in (size - 1) downTo 0) {
            action(i, get(i))
        }
    }
}