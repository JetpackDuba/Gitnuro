@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalSplitPaneApi::class)

package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.handMouseClickable
import app.extensions.toSmartSystemString
import app.extensions.toSystemDateTimeString
import app.git.diff.DiffResult
import app.theme.primaryTextColor
import app.theme.secondaryTextColor
import app.ui.components.AvatarImage
import app.ui.components.ScrollableLazyColumn
import app.ui.components.TooltipText
import app.viewmodels.HistoryState
import app.viewmodels.HistoryViewModel
import app.viewmodels.ViewDiffResult
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi

@Composable
fun FileHistory(
    historyViewModel: HistoryViewModel,
    onClose: () -> Unit
) {
    val historyState by historyViewModel.historyState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Header(filePath = historyState.filePath, onClose = onClose)

        HistoryContent(
            historyViewModel,
            historyState,
            onCommitSelected = { historyViewModel.selectCommit(it) }
        )
    }
}

@Composable
private fun Header(
    filePath: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .background(MaterialTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = filePath,
            color = MaterialTheme.colors.primaryTextColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .pointerHoverIcon(PointerIconDefaults.Hand)
        ) {
            Image(
                painter = painterResource("close.svg"),
                contentDescription = "Close history",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primaryTextColor),
            )
        }
    }
}


@Composable
private fun HistoryContent(
    historyViewModel: HistoryViewModel,
    historyState: HistoryState,
    onCommitSelected: (RevCommit) -> Unit,
) {
    val textScrollState by historyViewModel.lazyListState.collectAsState()
    val viewDiffResult by historyViewModel.viewDiffResult.collectAsState()

    when (historyState) {
        is HistoryState.Loaded -> HistoryContentLoaded(
            historyState = historyState,
            viewDiffResult = viewDiffResult,
            scrollState = textScrollState,
            onCommitSelected = onCommitSelected,
        )
        is HistoryState.Loading -> Box { }
    }
}

@Composable
fun HistoryContentLoaded(
    historyState: HistoryState.Loaded,
    viewDiffResult: ViewDiffResult?,
    scrollState: LazyListState,
    onCommitSelected: (RevCommit) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
    ) {
        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .width(300.dp)
                .background(MaterialTheme.colors.surface)
        ) {
            items(historyState.commits) { commit ->
                HistoryCommit(commit, onCommitSelected = { onCommitSelected(commit) })
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (
                viewDiffResult != null &&
                viewDiffResult is ViewDiffResult.Loaded
            ) {
                val diffResult = viewDiffResult.diffResult
                if (diffResult is DiffResult.Text) {
                    TextDiff(
                        diffEntryType = viewDiffResult.diffEntryType,
                        scrollState = scrollState,
                        diffResult = diffResult,
                        onUnstageHunk = { _, _ -> },
                        onStageHunk = { _, _ -> }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(MaterialTheme.colors.background)
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colors.background)
                )
            }
        }
    }
}

@Composable
fun HistoryCommit(commit: RevCommit, onCommitSelected: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .handMouseClickable { onCommitSelected() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(40.dp),
            personIdent = commit.authorIdent,
        )

        Column {
            Text(
                text = commit.shortMessage,
                maxLines = 1,
                fontSize = 14.sp,
                color = MaterialTheme.colors.primaryTextColor,
            )

            Row {
                Text(
                    text = commit.name.take(7),
                    maxLines = 1,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.secondaryTextColor,
                )
                Spacer(modifier = Modifier.weight(1f))

                val date = remember(commit.authorIdent) {
                    commit.authorIdent.`when`.toSmartSystemString()
                }

                TooltipText(
                    text = date,
                    color = MaterialTheme.colors.secondaryTextColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 12.sp,
                    tooltipTitle = date
                )
            }
        }
    }
}
