@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.handMouseClickable
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.extensions.shortName
import com.jetpackduba.gitnuro.extensions.toSmartSystemString
import com.jetpackduba.gitnuro.git.diff.DiffResult
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.onBackgroundSecondary
import com.jetpackduba.gitnuro.theme.tertiarySurface
import com.jetpackduba.gitnuro.ui.components.AvatarImage
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.TooltipText
import com.jetpackduba.gitnuro.ui.diff.HunkSplitTextDiff
import com.jetpackduba.gitnuro.ui.diff.HunkUnifiedTextDiff
import com.jetpackduba.gitnuro.viewmodels.HistoryState
import com.jetpackduba.gitnuro.viewmodels.HistoryViewModel
import com.jetpackduba.gitnuro.viewmodels.ViewDiffResult
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun FileHistory(
    historyViewModel: HistoryViewModel,
    onClose: () -> Unit
) {
    val historyState by historyViewModel.historyState.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { keyEvent ->
                if (keyEvent.matchesBinding(KeybindingOption.EXIT) && keyEvent.type == KeyEventType.KeyDown) {
                    onClose()
                    true
                } else
                    false
            },
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
            .height(40.dp)
            .background(MaterialTheme.colors.tertiarySurface)
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = filePath,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .handOnHover()
        ) {
            Image(
                painter = painterResource(AppIcons.CLOSE),
                contentDescription = "Close history",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
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
                when (val diffResult = viewDiffResult.diffResult) {
                    is DiffResult.Text -> {
                        HunkUnifiedTextDiff(
                            fileDiffType = viewDiffResult.fileDiffType,
                            scrollState = scrollState,
                            diffResult = diffResult,
                            onUnstageHunk = { _, _ -> },
                            onStageHunk = { _, _ -> },
                            onResetHunk = { _, _ -> },
                            onUnStageLine = { _, _, _ -> },
                            onDiscardLine = { _, _, _ -> },
                        )
                    }

                    is DiffResult.TextSplit -> {
                        HunkSplitTextDiff(
                            fileDiffType = viewDiffResult.fileDiffType,
                            scrollState = scrollState,
                            diffResult = diffResult,
                            onUnstageHunk = { _, _ -> },
                            onStageHunk = { _, _ -> },
                            onResetHunk = { _, _ -> },
                            onUnStageLine = { _, _, _ -> },
                            onDiscardLine = { _, _, _ -> },
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colors.background)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
                style = MaterialTheme.typography.body1,
            )

            Row {
                Text(
                    text = commit.shortName,
                    maxLines = 1,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                )
                Spacer(modifier = Modifier.weight(1f))

                val date = remember(commit.authorIdent) {
                    commit.authorIdent.`when`.toSmartSystemString()
                }

                TooltipText(
                    text = date,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.body2,
                    tooltipTitle = date
                )
            }
        }
    }
}
