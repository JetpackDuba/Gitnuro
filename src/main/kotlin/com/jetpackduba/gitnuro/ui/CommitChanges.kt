package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.tertiarySurface
import com.jetpackduba.gitnuro.ui.components.*
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.committedChangesEntriesContextMenuItems
import com.jetpackduba.gitnuro.ui.tree_files.TreeItem
import com.jetpackduba.gitnuro.viewmodels.CommitChangesStateUi
import com.jetpackduba.gitnuro.viewmodels.CommitChangesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun CommitChanges(
    commitChangesViewModel: CommitChangesViewModel = gitnuroViewModel(),
    selectedItem: SelectedItem.CommitBasedItem,
    onDiffSelected: (DiffEntry) -> Unit,
    diffSelected: DiffEntryType?,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
) {
    LaunchedEffect(selectedItem) {
        commitChangesViewModel.loadChanges(selectedItem.revCommit)
    }

    val commitChangesStatus = commitChangesViewModel.commitChangesStateUi.collectAsState().value
    val showSearch by commitChangesViewModel.showSearch.collectAsState()
    val changesListScroll by commitChangesViewModel.changesLazyListState.collectAsState()
    val textScroll by commitChangesViewModel.textScroll.collectAsState()
    val showAsTree by commitChangesViewModel.showAsTree.collectAsState()

    var searchFilter by remember(commitChangesViewModel, showSearch, commitChangesStatus) {
        mutableStateOf(commitChangesViewModel.searchFilter.value)
    }

    when (commitChangesStatus) {
        CommitChangesStateUi.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        is CommitChangesStateUi.Loaded -> {
            CommitChangesView(
                diffSelected = diffSelected,
                commitChangesStatus = commitChangesStatus,
                onBlame = onBlame,
                onHistory = onHistory,
                showSearch = showSearch,
                showAsTree = showAsTree,
                changesListScroll = changesListScroll,
                textScroll = textScroll,
                searchFilter = searchFilter,
                onDiffSelected = onDiffSelected,
                onSearchFilterToggled = { visible ->
                    commitChangesViewModel.onSearchFilterToggled(visible)
                },
                onSearchFilterChanged = { filter ->
                    searchFilter = filter
                    commitChangesViewModel.onSearchFilterChanged(filter)
                },
                onDirectoryClicked = { commitChangesViewModel.onDirectoryClicked(it.fullPath) },
                onAlternateShowAsTree = { commitChangesViewModel.alternateShowAsTree() },
            )
        }
    }
}

@Composable
private fun CommitChangesView(
    commitChangesStatus: CommitChangesStateUi.Loaded,
    diffSelected: DiffEntryType?,
    changesListScroll: LazyListState,
    textScroll: ScrollState,
    showSearch: Boolean,
    showAsTree: Boolean,
    searchFilter: TextFieldValue,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
    onDiffSelected: (DiffEntry) -> Unit,
    onSearchFilterToggled: (Boolean) -> Unit,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onDirectoryClicked: (TreeItem.Dir) -> Unit,
    onAlternateShowAsTree: () -> Unit,
) {
    val commit = commitChangesStatus.commit

    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxSize(),
    ) {

        Column(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
                .weight(1f, fill = true)
                .background(MaterialTheme.colors.background)
        ) {
            Header(
                showSearch,
                searchFilter,
                onSearchFilterChanged,
                onSearchFilterToggled,
                showAsTree = showAsTree,
                onAlternateShowAsTree = onAlternateShowAsTree,
            )

            when (commitChangesStatus) {
                is CommitChangesStateUi.ListLoaded -> {
                    val changes = commitChangesStatus.changes

                    ListCommitLogChanges(
                        diffSelected = diffSelected,
                        changesListScroll = changesListScroll,
                        diffEntries = changes,
                        onDiffSelected = onDiffSelected,
                        onGenerateContextMenu = { diffEntry ->
                            committedChangesEntriesContextMenuItems(
                                diffEntry,
                                onBlame = { onBlame(diffEntry.filePath) },
                                onHistory = { onHistory(diffEntry.filePath) },
                            )
                        }
                    )
                }

                is CommitChangesStateUi.TreeLoaded -> {
                    TreeCommitLogChanges(
                        diffSelected = diffSelected,
                        changesListScroll = changesListScroll,
                        treeItems = commitChangesStatus.changes,
                        onDiffSelected = onDiffSelected,
                        onGenerateContextMenu = { diffEntry ->
                            committedChangesEntriesContextMenuItems(
                                diffEntry,
                                onBlame = { onBlame(diffEntry.filePath) },
                                onHistory = { onHistory(diffEntry.filePath) },
                            )
                        },
                        onDirectoryClicked = onDirectoryClicked,
                    )
                }
            }

        }

        MessageAuthorFooter(commit, textScroll)
    }
}

@Composable
private fun Header(
    showSearch: Boolean,
    searchFilter: TextFieldValue,
    onSearchFilterChanged: (TextFieldValue) -> Unit,
    onSearchFilterToggled: (Boolean) -> Unit,
    showAsTree: Boolean,
    onAlternateShowAsTree: () -> Unit,
) {
    val searchFocusRequester = remember { FocusRequester() }

    /**
     * State used to prevent the text field from getting the focus when returning from another tab
     */
    var requestFocus by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colors.tertiarySurface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 16.dp),
            text = "Files changed",
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Left,
            color = MaterialTheme.colors.onBackground,
            maxLines = 1,
            style = MaterialTheme.typography.body2,
        )

        Box(modifier = Modifier.weight(1f))

        IconButton(
            onClick = {
                onAlternateShowAsTree()
            },
            modifier = Modifier.handOnHover()
        ) {
            Icon(
                painter = painterResource(if (showAsTree) AppIcons.LIST else AppIcons.TREE),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.onBackground,
            )
        }

        IconButton(
            onClick = {
                onSearchFilterToggled(!showSearch)

                if (!showSearch)
                    requestFocus = true
            },
            modifier = Modifier.handOnHover(),
        ) {
            Icon(
                painter = painterResource(AppIcons.SEARCH),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colors.onBackground,
            )
        }
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
}

@Composable
private fun MessageAuthorFooter(
    commit: RevCommit,
    textScroll: ScrollState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colors.background),
    ) {
        SelectionContainer {
            Text(
                text = commit.fullMessage,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(8.dp)
                    .verticalScroll(textScroll),
            )
        }

        Author(commit.shortName, commit.name, commit.authorIdent)
    }
}

@Composable
fun Author(
    shortName: String,
    name: String,
    author: PersonIdent,
) {
    var copied by remember(name) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colors.tertiarySurface),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(40.dp),
            personIdent = author,
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            TooltipText(
                text = author.name,
                maxLines = 1,
                style = MaterialTheme.typography.body2,
                tooltipTitle = author.emailAddress,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shortName,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.handMouseClickable {
                        scope.launch {
                            clipboard.setText(AnnotatedString(name))
                            copied = true
                            delay(2000) // 2s
                            copied = false
                        }
                    }
                )

                if (copied) {
                    Text(
                        text = "Copied!",
                        color = MaterialTheme.colors.primaryVariant,
                        maxLines = 1,
                        style = MaterialTheme.typography.caption,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }


                Spacer(modifier = Modifier.weight(1f, fill = true))

                val smartDate = remember(author) {
                    author.`when`.toSmartSystemString()
                }

                val systemDate = remember(author) {
                    author.`when`.toSystemDateTimeString()
                }

                TooltipText(
                    text = smartDate,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.body2,
                    tooltipTitle = systemDate
                )
            }
        }
    }
}

@Composable
fun ListCommitLogChanges(
    diffEntries: List<DiffEntry>,
    diffSelected: DiffEntryType?,
    changesListScroll: LazyListState,
    onDiffSelected: (DiffEntry) -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
) {
    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = changesListScroll,
    ) {
        items(items = diffEntries) { diffEntry ->
            FileEntry(
                icon = diffEntry.icon,
                iconColor = diffEntry.iconColor,
                parentDirectoryPath = diffEntry.parentDirectoryPath,
                fileName = diffEntry.fileName,
                isSelected = diffSelected is DiffEntryType.CommitDiff && diffSelected.diffEntry == diffEntry,
                onClick = { onDiffSelected(diffEntry) },
                onDoubleClick = {},
                onGenerateContextMenu = { onGenerateContextMenu(diffEntry) },
                trailingAction = null,
            )
        }
    }
}

@Composable
fun TreeCommitLogChanges(
    treeItems: List<TreeItem<DiffEntry>>,
    diffSelected: DiffEntryType?,
    changesListScroll: LazyListState,
    onDiffSelected: (DiffEntry) -> Unit,
    onDirectoryClicked: (TreeItem.Dir) -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
) {
    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = changesListScroll,
    ) {
        items(items = treeItems) { entry ->
            CommitTreeItemEntry(
                entry = entry,
                isSelected = entry is TreeItem.File &&
                        diffSelected is DiffEntryType.CommitDiff &&
                        diffSelected.diffEntry == entry.data,
                onFileClick = { onDiffSelected(it) },
                onDirectoryClick = { onDirectoryClicked(it) },
                onGenerateContextMenu = onGenerateContextMenu,
                onGenerateDirectoryContextMenu = { emptyList() },
            )
        }
    }
}


@Composable
private fun CommitTreeItemEntry(
    entry: TreeItem<DiffEntry>,
    isSelected: Boolean,
    onFileClick: (DiffEntry) -> Unit,
    onDirectoryClick: (TreeItem.Dir) -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
    onGenerateDirectoryContextMenu: (TreeItem.Dir) -> List<ContextMenuElement>,
) {
    when (entry) {
        is TreeItem.File -> CommitFileEntry(
            fileEntry = entry,
            isSelected = isSelected,
            onClick = { onFileClick(entry.data) },
            onGenerateContextMenu = onGenerateContextMenu,
        )

        is TreeItem.Dir -> DirectoryEntry(
            dirName = entry.displayName,
            onClick = { onDirectoryClick(entry) },
            depth = entry.depth,
            onGenerateContextMenu = { onGenerateDirectoryContextMenu(entry) },
        )
    }
}

@Composable
private fun CommitFileEntry(
    fileEntry: TreeItem.File<DiffEntry>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onGenerateContextMenu: (DiffEntry) -> List<ContextMenuElement>,
) {
    val diffEntry = fileEntry.data

    FileEntry(
        icon = diffEntry.icon,
        iconColor = diffEntry.iconColor,
        parentDirectoryPath = "",
        fileName = diffEntry.fileName,
        isSelected = isSelected,
        onClick = onClick,
        onDoubleClick = {},
        depth = fileEntry.depth,
        onGenerateContextMenu = { onGenerateContextMenu(diffEntry) },
        trailingAction = null,
    )
}