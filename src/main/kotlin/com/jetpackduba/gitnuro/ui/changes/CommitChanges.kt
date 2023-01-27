package com.jetpackduba.gitnuro.ui.changes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.components.AvatarImage
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.TooltipText
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.commitedChangesEntriesContextMenuItems
import com.jetpackduba.gitnuro.viewmodels.CommitChanges
import com.jetpackduba.gitnuro.viewmodels.CommitChangesStatus
import com.jetpackduba.gitnuro.viewmodels.CommitChangesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.ObjectId
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

    val commitChangesStatusState = commitChangesViewModel.commitChangesStatus.collectAsState()

    when (val commitChangesStatus = commitChangesStatusState.value) {
        CommitChangesStatus.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        is CommitChangesStatus.Loaded -> {
            CommitChangesView(
                diffSelected = diffSelected,
                commit = commitChangesStatus.commit,
                changes = commitChangesStatus.changes,
                onDiffSelected = onDiffSelected,
                onBlame = onBlame,
                onHistory = onHistory,
            )
        }
    }
}

@Composable
fun CommitChangesView(
    commit: RevCommit,
    changes: List<DiffEntry>,
    onDiffSelected: (DiffEntry) -> Unit,
    diffSelected: DiffEntryType?,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxSize(),
    ) {
        val scroll = rememberScrollState(0)



        Column(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
                .weight(1f, fill = true)
                .background(MaterialTheme.colors.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(MaterialTheme.colors.tertiarySurface),
                contentAlignment = Alignment.CenterStart,
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
            }


            CommitLogChanges(
                diffSelected = diffSelected,
                diffEntries = changes,
                onDiffSelected = onDiffSelected,
                onBlame = onBlame,
                onHistory = onHistory,
            )
        }

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
                        .verticalScroll(scroll),
                )
            }

            Author(commit.id, commit.authorIdent)
        }
    }
}

@Composable
fun Author(
    id: ObjectId,
    author: PersonIdent,
) {
    var copied by remember(id) { mutableStateOf(false) }
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
                    text = id.abbreviate(7).name(),
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.handMouseClickable {
                        scope.launch {
                            clipboard.setText(AnnotatedString(id.name))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommitLogChanges(
    diffEntries: List<DiffEntry>,
    onDiffSelected: (DiffEntry) -> Unit,
    diffSelected: DiffEntryType?,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
) {
    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(items = diffEntries) { diffEntry ->
            ContextMenu(
                items = {
                    commitedChangesEntriesContextMenuItems(
                        diffEntry,
                        onBlame = { onBlame(diffEntry.filePath) },
                        onHistory = { onHistory(diffEntry.filePath) },
                    )
                }
            ) {
                CommitLogChangesItem(
                    diffEntry = diffEntry,
                    diffSelected = diffSelected,
                    onDiffSelected = { onDiffSelected(diffEntry) }
                )
            }
        }
    }
}

@Composable
fun CommitLogChangesItem(
    diffEntry: DiffEntry,
    diffSelected: DiffEntryType?,
    onDiffSelected: () -> Unit,
) {
    Column(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .handMouseClickable {
                onDiffSelected()
            }
            .backgroundIf(
                condition = diffSelected is DiffEntryType.CommitDiff && diffSelected.diffEntry == diffEntry,
                color = MaterialTheme.colors.backgroundSelected,
            ),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(2f))

        Row {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(16.dp),
                imageVector = diffEntry.icon,
                contentDescription = null,
                tint = diffEntry.iconColor,
            )

            if (diffEntry.parentDirectoryPath.isNotEmpty()) {
                Text(
                    text = diffEntry.parentDirectoryPath.removeSuffix("/"),
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.body2,
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
                text = diffEntry.fileName,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(end = 16.dp),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
            )
        }

        Spacer(modifier = Modifier.weight(2f))
    }
}