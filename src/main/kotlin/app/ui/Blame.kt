@file:OptIn(ExperimentalComposeUiApi::class)

package app.ui

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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.handMouseClickable
import app.extensions.lineAt
import app.extensions.toStringWithSpaces
import app.theme.headerBackground
import app.theme.primaryTextColor
import app.ui.components.PrimaryButton
import app.ui.components.ScrollableLazyColumn
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
            .onPreviewKeyEvent {
                if (it.key == Key.Escape) {
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
                        modifier = Modifier.fillMaxWidth()
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
                                    .background(MaterialTheme.colors.surface)
                                    .handMouseClickable { if (commit != null) onSelectCommit(commit) },
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = author?.name.orEmpty(),
                                    color = MaterialTheme.colors.primaryTextColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 16.dp),
                                    fontSize = 12.sp,
                                )
                                Text(
                                    text = commit?.shortMessage ?: "Uncommited change",
                                    color = MaterialTheme.colors.primaryTextColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(start = 16.dp),
                                    fontSize = 10.sp,
                                )
                            }
                        }

                        Text(
                            text = line + blameResult.resultContents.lineDelimiter,
                            color = MaterialTheme.colors.primaryTextColor,
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            fontFamily = FontFamily.Monospace,
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
            .background(MaterialTheme.colors.surface)
            .padding(start = 4.dp, end = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = line.toStringWithSpaces(highestLineLength),
            color = MaterialTheme.colors.primaryTextColor,
            fontFamily = FontFamily.Monospace,
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
                color = MaterialTheme.colors.primaryTextColor,
                maxLines = 1,
                fontSize = 10.sp,
            )
            Text(
                text = filePath,
                color = MaterialTheme.colors.primaryTextColor,
                maxLines = 1,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            onClick = onExpand,
            text = "Show",
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier.padding(horizontal = 16.dp)
                .pointerHoverIcon(PointerIconDefaults.Hand)
        ) {
            Image(
                painter = painterResource("close.svg"),
                contentDescription = "Close blame",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primaryTextColor),
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
            .padding(start = 8.dp, end = 8.dp)
            .background(MaterialTheme.colors.headerBackground),
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
                contentDescription = "Close blame",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primaryTextColor),
            )
        }
    }
}