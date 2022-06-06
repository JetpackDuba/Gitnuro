package app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.*
import app.git.DiffEntryType
import app.theme.*
import app.ui.components.AvatarImage
import app.ui.components.ScrollableLazyColumn
import app.ui.components.TooltipText
import app.ui.context_menu.commitedChangesEntriesContextMenuItems
import app.viewmodels.CommitChangesStatus
import app.viewmodels.CommitChangesViewModel
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun CommitChanges(
    commitChangesViewModel: CommitChangesViewModel,
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
            .fillMaxSize(),
    ) {
        val scroll = rememberScrollState(0)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
        ) {
            SelectionContainer {
                Text(
                    text = commit.fullMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primaryTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background)
                        .height(120.dp)
                        .padding(8.dp)
                        .verticalScroll(scroll),
                )
            }

            Divider(modifier = Modifier.fillMaxWidth())

            Author(commit)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
                .background(MaterialTheme.colors.background)
        ) {
            Text(
                modifier = Modifier
                    .background(MaterialTheme.colors.headerBackground)
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                text = "Files changed",
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.headerText,
                maxLines = 1,
                fontSize = 13.sp,
            )


            CommitLogChanges(
                diffSelected = diffSelected,
                diffEntries = changes,
                onDiffSelected = onDiffSelected,
                onBlame = onBlame,
                onHistory = onHistory,
            )
        }
    }
}

@Composable
fun Author(commit: RevCommit) {
    val authorIdent = commit.authorIdent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(40.dp),
            personIdent = commit.authorIdent,
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            TooltipText(
                text = authorIdent.name,
                color = MaterialTheme.colors.primaryTextColor,
                maxLines = 1,
                fontSize = 14.sp,
                tooltipTitle = authorIdent.emailAddress,
            )

            Row {
                Text(
                    text = commit.id.abbreviate(7).name(),
                    color = MaterialTheme.colors.secondaryTextColor,
                    maxLines = 1,
                    fontSize = 12.sp,
                )


                Spacer(modifier = Modifier.weight(1f, fill = true))

                val date = remember(authorIdent) {
                    authorIdent.`when`.toSmartSystemString()
                }

                TooltipText(
                    text = date,
                    color = MaterialTheme.colors.secondaryTextColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 13.sp,
                    tooltipTitle = authorIdent.`when`.toSystemDateTimeString()
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
            ContextMenuArea(
                items = {
                    commitedChangesEntriesContextMenuItems(
                        diffEntry,
                        onBlame = { onBlame(diffEntry.filePath) },
                        onHistory = { onHistory(diffEntry.filePath) },
                    )
                }
            ) {
                Column(
                    modifier = Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                        .handMouseClickable {
                            onDiffSelected(diffEntry)
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
                                text = diffEntry.parentDirectoryPath,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 13.sp,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colors.secondaryTextColor,
                            )
                        }
                        Text(
                            text = diffEntry.fileName,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 13.sp,
                            color = MaterialTheme.colors.primaryTextColor,
                        )
                    }

                    Spacer(modifier = Modifier.weight(2f))

                    Divider()
                }
            }
        }
    }
}