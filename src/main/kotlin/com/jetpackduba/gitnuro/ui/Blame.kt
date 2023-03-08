@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.notoSansMonoFontFamily
import com.jetpackduba.gitnuro.theme.secondarySurface
import com.jetpackduba.gitnuro.theme.tertiarySurface
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import org.eclipse.jgit.blame.BlameResult
import org.eclipse.jgit.revwalk.RevCommit

@Composable
fun Blame(
    filePath: String,
    blameResult: BlameResult,
    onSelectCommit: (RevCommit) -> Unit,
    onClose: () -> Unit,
) {

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.matchesBinding(KeybindingOption.EXIT)) {
                    onClose()
                    true
                } else
                    false
            },
    ) {
        Header(filePath, onClose = onClose)
        SelectionContainer {
            ScrollableLazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                val contents = blameResult.resultContents
                val contentsSize = contents.size()
                val contentSizeLengthInString = contentsSize.toString().count()

                items(contentsSize) { index ->
                    val line = contents.lineAt(index)
                    val author = blameResult.getSourceAuthor(index)
                    val commit = blameResult.getSourceCommit(index)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colors.background)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {

                        DisableSelection {
                            LineNumber(
                                line = index + 1,
                                highestLineLength = contentSizeLengthInString
                            )

                            Column(
                                modifier = Modifier
                                    .width(200.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colors.secondarySurface)
                                    .fastClickable { if (commit != null) onSelectCommit(commit) },
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = author?.name.orEmpty(),
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 16.dp),
                                    style = MaterialTheme.typography.body2,
                                )
                                Text(
                                    text = commit?.shortMessage ?: "Uncommited change",
                                    style = MaterialTheme.typography.caption,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                        }

                        Text(
                            text = line + blameResult.resultContents.lineDelimiter,
                            color = MaterialTheme.colors.onBackground,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            fontFamily = notoSansMonoFontFamily,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LineNumber(line: Int, highestLineLength: Int) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colors.secondarySurface)
            .padding(start = 4.dp, end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = line.toStringWithSpaces(highestLineLength),
            color = MaterialTheme.colors.onBackground,
            fontFamily = notoSansMonoFontFamily,
        )
    }
}

@Composable
fun MinimizedBlame(
    filePath: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(MaterialTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Minimized file blame",
                style = MaterialTheme.typography.caption,
                maxLines = 1,
            )
            Text(
                text = filePath,
                style = MaterialTheme.typography.body2,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            onClick = onExpand,
            text = "Show",
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .handOnHover()
        ) {
            Image(
                painter = painterResource("close.svg"),
                contentDescription = "Close blame",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
            )
        }
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
                painter = painterResource("close.svg"),
                contentDescription = "Close blame",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
            )
        }
    }
}