@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui.diff

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.animatedImages
import com.jetpackduba.gitnuro.git.diff.DiffResult
import com.jetpackduba.gitnuro.git.diff.Hunk
import com.jetpackduba.gitnuro.git.diff.Line
import com.jetpackduba.gitnuro.git.diff.LineType
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.SecondaryButton
import com.jetpackduba.gitnuro.viewmodels.DiffViewModel
import com.jetpackduba.gitnuro.viewmodels.TextDiffType
import com.jetpackduba.gitnuro.viewmodels.ViewDiffResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.diff.DiffEntry
import org.jetbrains.compose.animatedimage.Blank
import org.jetbrains.compose.animatedimage.animate
import org.jetbrains.compose.animatedimage.loadAnimatedImage
import org.jetbrains.compose.resources.loadOrNull
import java.io.FileInputStream
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.math.max

@Composable
fun Diff(
    diffViewModel: DiffViewModel,
    onCloseDiffView: () -> Unit,
) {
    val diffResultState = diffViewModel.diffResult.collectAsState()
    val diffType by diffViewModel.diffTypeFlow.collectAsState()
    val viewDiffResult = diffResultState.value ?: return
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.matchesBinding(KeybindingOption.EXIT)) {
                    onCloseDiffView()
                    true
                } else
                    false
            }
    ) {
        when (viewDiffResult) {
            ViewDiffResult.DiffNotFound -> {
                onCloseDiffView()
            }

            is ViewDiffResult.Loaded -> {
                val diffEntryType = viewDiffResult.diffEntryType
                val diffEntry = viewDiffResult.diffResult.diffEntry
                val diffResult = viewDiffResult.diffResult

                DiffHeader(
                    diffEntryType = diffEntryType,
                    diffEntry = diffEntry,
                    onCloseDiffView = onCloseDiffView,
                    diffType = diffType,
                    onStageFile = { diffViewModel.stageFile(it) },
                    onUnstageFile = { diffViewModel.unstageFile(it) },
                    onChangeDiffType = { diffViewModel.changeTextDiffType(it) }
                )

                val scrollState by diffViewModel.lazyListState.collectAsState()

                when (diffResult) {
                    is DiffResult.TextSplit -> HunkSplitTextDiff(
                        diffEntryType = diffEntryType,
                        scrollState = scrollState,
                        diffResult = diffResult,
                        onUnstageHunk = { entry, hunk ->
                            diffViewModel.unstageHunk(entry, hunk)
                        },
                        onStageHunk = { entry, hunk ->
                            diffViewModel.stageHunk(entry, hunk)
                        },
                        onResetHunk = { entry, hunk ->
                            diffViewModel.resetHunk(entry, hunk)
                        },
                        onActionTriggered = { entry, hunk, line ->
                            if (diffEntryType is DiffEntryType.UnstagedDiff)
                                diffViewModel.stageHunkLine(entry, hunk, line)
                            else if (diffEntryType is DiffEntryType.StagedDiff)
                                diffViewModel.unstageHunkLine(entry, hunk, line)
                        }
                    )

                    is DiffResult.Text -> HunkUnifiedTextDiff(
                        diffEntryType = diffEntryType,
                        scrollState = scrollState,
                        diffResult = diffResult,
                        onUnstageHunk = { entry, hunk ->
                            diffViewModel.unstageHunk(entry, hunk)
                        },
                        onStageHunk = { entry, hunk ->
                            diffViewModel.stageHunk(entry, hunk)
                        },
                        onResetHunk = { entry, hunk ->
                            diffViewModel.resetHunk(entry, hunk)
                        },
                        onActionTriggered = { entry, hunk, line ->
                            if (diffEntryType is DiffEntryType.UnstagedDiff)
                                diffViewModel.stageHunkLine(entry, hunk, line)
                            else if (diffEntryType is DiffEntryType.StagedDiff)
                                diffViewModel.unstageHunkLine(entry, hunk, line)
                        }
                    )

                    is DiffResult.NonText -> {
                        NonTextDiff(diffResult)
                    }
                }
            }

            is ViewDiffResult.Loading -> {
                Column {
                    PathOnlyDiffHeader(filePath = viewDiffResult.filePath, onCloseDiffView = onCloseDiffView)
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colors.primaryVariant
                    )
                }

            }

            ViewDiffResult.None -> throw NotImplementedError("None should be a possible state in the diff")
        }


    }
}

@Composable
fun NonTextDiff(diffResult: DiffResult.NonText) {
    val oldBinaryContent = diffResult.oldBinaryContent
    val newBinaryContent = diffResult.newBinaryContent

    val showOldAndNew = oldBinaryContent != EntryContent.Missing && newBinaryContent != EntryContent.Missing

    Row(
        modifier = Modifier
            .fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        if (showOldAndNew) {
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 24.dp, end = 8.dp, top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SideTitle("Old")
                SideDiff(oldBinaryContent)
            }
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SideTitle("New")
                SideDiff(newBinaryContent)
            }
        } else if (oldBinaryContent != EntryContent.Missing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 24.dp),
            ) {
                SideDiff(oldBinaryContent)
            }
        } else if (newBinaryContent != EntryContent.Missing) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                SideDiff(newBinaryContent)
            }
        }
    }
}

@Composable
fun SideTitle(text: String) {
    Text(
        text = text,
        fontSize = 20.sp,
        color = MaterialTheme.colors.onBackground,
    )
}

@Composable
fun SideDiff(entryContent: EntryContent) {
    when (entryContent) {
        EntryContent.Binary -> BinaryDiff()
        is EntryContent.ImageBinary -> ImageDiff(entryContent.tempFilePath, entryContent.contentType)
        else -> {
        }
//        is EntryContent.Text -> //TODO maybe have a text view if the file was a binary before?
// TODO Show some info about this       EntryContent.TooLargeEntry -> TODO()
    }
}

@Composable
private fun ImageDiff(tempImagePath: Path, contentType: String) {
    val imagePath = tempImagePath.absolutePathString()

    if (animatedImages.contains(contentType)) {
        AnimatedImage(imagePath)
    } else {
        StaticImage(imagePath)
    }
}

@Composable
private fun StaticImage(tempImagePath: String) {
    var image by remember(tempImagePath) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(tempImagePath) {
        withContext(Dispatchers.IO) {
            FileInputStream(tempImagePath).use { inputStream ->
                image = loadImageBitmap(inputStream = inputStream)
            }
        }
    }

    Image(
        bitmap = image ?: ImageBitmap.Blank,
        contentDescription = null,
        modifier = Modifier
            .run {
                val safeImage = image
                if (safeImage == null)
                    fillMaxSize()
                else {
                    width(safeImage.width.dp)
                        .height(safeImage.height.dp)

                }
            }
            .handMouseClickable {
                openFileWithExternalApp(tempImagePath)
            }
    )
}

@Composable
private fun AnimatedImage(tempImagePath: String) {
    Image(
        bitmap = loadOrNull(tempImagePath) { loadAnimatedImage(tempImagePath) }?.animate() ?: ImageBitmap.Blank,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
            .handMouseClickable {
                openFileWithExternalApp(tempImagePath)
            }
    )
}

@Composable
fun BinaryDiff() {
    Image(
        painter = painterResource("binary.svg"),
        contentDescription = null,
        modifier = Modifier.width(400.dp),
        colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
    )
}


@Composable
fun HunkUnifiedTextDiff(
    diffEntryType: DiffEntryType,
    scrollState: LazyListState,
    diffResult: DiffResult.Text,
    onUnstageHunk: (DiffEntry, Hunk) -> Unit,
    onStageHunk: (DiffEntry, Hunk) -> Unit,
    onActionTriggered: (DiffEntry, Hunk, Line) -> Unit,
    onResetHunk: (DiffEntry, Hunk) -> Unit,
) {
    val hunks = diffResult.hunks

    SelectionContainer {
        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = scrollState
        ) {
            for (hunk in hunks) {
                item {
                    DisableSelection {
                        HunkHeader(
                            header = hunk.header,
                            diffEntryType = diffEntryType,
                            onUnstageHunk = { onUnstageHunk(diffResult.diffEntry, hunk) },
                            onStageHunk = { onStageHunk(diffResult.diffEntry, hunk) },
                            onResetHunk = { onResetHunk(diffResult.diffEntry, hunk) },
                        )
                    }
                }

                val oldHighestLineNumber = hunk.lines.maxOf { it.displayOldLineNumber }
                val newHighestLineNumber = hunk.lines.maxOf { it.displayNewLineNumber }
                val highestLineNumber = max(oldHighestLineNumber, newHighestLineNumber)
                val highestLineNumberLength = highestLineNumber.toString().count()

                items(hunk.lines) { line ->
                    DiffLine(
                        highestLineNumberLength,
                        line,
                        diffEntryType = diffEntryType,
                        onActionTriggered = {
                            onActionTriggered(
                                diffResult.diffEntry,
                                hunk,
                                line,
                            )
                        },
                    )
                }
            }
        }
    }

}

@Composable
fun HunkSplitTextDiff(
    diffEntryType: DiffEntryType,
    scrollState: LazyListState,
    diffResult: DiffResult.TextSplit,
    onUnstageHunk: (DiffEntry, Hunk) -> Unit,
    onStageHunk: (DiffEntry, Hunk) -> Unit,
    onResetHunk: (DiffEntry, Hunk) -> Unit,
    onActionTriggered: (DiffEntry, Hunk, Line) -> Unit,
) {
    val hunks = diffResult.hunks

    /**
     * Disables selection in one side when the other is being selected
     */
    var selectableSide by remember { mutableStateOf(SelectableSide.BOTH) }

    SelectionContainer {
        ScrollableLazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            state = scrollState
        ) {
            for (splitHunk in hunks) {
                item {
                    DisableSelection {
                        HunkHeader(
                            header = splitHunk.sourceHunk.header,
                            diffEntryType = diffEntryType,
                            onUnstageHunk = { onUnstageHunk(diffResult.diffEntry, splitHunk.sourceHunk) },
                            onStageHunk = { onStageHunk(diffResult.diffEntry, splitHunk.sourceHunk) },
                            onResetHunk = { onResetHunk(diffResult.diffEntry, splitHunk.sourceHunk) },
                        )
                    }
                }

                val oldHighestLineNumber = splitHunk.sourceHunk.lines.maxOf { it.displayOldLineNumber }
                val newHighestLineNumber = splitHunk.sourceHunk.lines.maxOf { it.displayNewLineNumber }
                val highestLineNumber = max(oldHighestLineNumber, newHighestLineNumber)
                val highestLineNumberLength = highestLineNumber.toString().count()

                items(splitHunk.lines) { linesPair ->
                    SplitDiffLine(
                        highestLineNumberLength = highestLineNumberLength,
                        oldLine = linesPair.first,
                        newLine = linesPair.second,
                        selectableSide = selectableSide,
                        diffEntryType = diffEntryType,
                        onActionTriggered = { line ->
                            onActionTriggered(diffResult.diffEntry, splitHunk.sourceHunk, line)
                        },
                        onChangeSelectableSide = { newSelectableSide ->
                            if (newSelectableSide != selectableSide) {
                                selectableSide = newSelectableSide
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DynamicSelectionDisable(isDisabled: Boolean, content: @Composable () -> Unit) {
    if (isDisabled) {
        DisableSelection(content)
    } else
        content()
}

@Composable
fun SplitDiffLine(
    highestLineNumberLength: Int,
    oldLine: Line?,
    newLine: Line?,
    selectableSide: SelectableSide,
    diffEntryType: DiffEntryType,
    onChangeSelectableSide: (SelectableSide) -> Unit,
    onActionTriggered: (Line) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.secondarySurface)
            .height(IntrinsicSize.Min)
    ) {
        SplitDiffLineSide(
            modifier = Modifier
                .weight(1f),
            highestLineNumberLength = highestLineNumberLength,
            line = oldLine,
            displayLineNumber = oldLine?.displayOldLineNumber ?: 0,
            currentSelectableSide = selectableSide,
            lineSelectableSide = SelectableSide.OLD,
            onChangeSelectableSide = onChangeSelectableSide,
            diffEntryType = diffEntryType,
            onActionTriggered = { if (oldLine != null) onActionTriggered(oldLine) }
        )

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .background(MaterialTheme.colors.secondarySurface)
        )

        SplitDiffLineSide(
            modifier = Modifier
                .weight(1f),
            highestLineNumberLength = highestLineNumberLength,
            line = newLine,
            displayLineNumber = newLine?.displayNewLineNumber ?: 0,
            currentSelectableSide = selectableSide,
            lineSelectableSide = SelectableSide.NEW,
            onChangeSelectableSide = onChangeSelectableSide,
            diffEntryType = diffEntryType,
            onActionTriggered = { if (newLine != null) onActionTriggered(newLine) }
        )

    }
}

@Composable
fun SplitDiffLineSide(
    modifier: Modifier,
    highestLineNumberLength: Int,
    line: Line?,
    displayLineNumber: Int,
    currentSelectableSide: SelectableSide,
    lineSelectableSide: SelectableSide,
    diffEntryType: DiffEntryType,
    onChangeSelectableSide: (SelectableSide) -> Unit,
    onActionTriggered: () -> Unit,
) {
    Box(
        modifier = modifier
            .onPointerEvent(PointerEventType.Press) {
                onChangeSelectableSide(lineSelectableSide)
            }
            .onPointerEvent(PointerEventType.Release) {
                onChangeSelectableSide(SelectableSide.BOTH)
            }
    ) {
        if (line != null) {
            // To avoid both sides being selected, disable one side when the use is interacting with the other
            DynamicSelectionDisable(
                currentSelectableSide != lineSelectableSide &&
                        currentSelectableSide != SelectableSide.BOTH
            ) {
                SplitDiffLine(
                    highestLineNumberLength = highestLineNumberLength,
                    line = line,
                    lineNumber = displayLineNumber,
                    diffEntryType = diffEntryType,
                    onActionTriggered = onActionTriggered,
                )
            }
        }
    }
}

enum class SelectableSide {
    BOTH,
    OLD,
    NEW;
}

@Composable
fun HunkHeader(
    header: String,
    diffEntryType: DiffEntryType,
    onUnstageHunk: () -> Unit,
    onStageHunk: () -> Unit,
    onResetHunk: () -> Unit,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.secondarySurface)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = header,
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.body1,
        )

        Spacer(modifier = Modifier.weight(1f))

        // Hunks options are only visible when repository is a normal state (not during merge/rebase)
        if (
            (diffEntryType is DiffEntryType.SafeStagedDiff || diffEntryType is DiffEntryType.SafeUnstagedDiff) &&
            (diffEntryType is DiffEntryType.UncommitedDiff && // Added just to make smartcast work
                    diffEntryType.statusEntry.statusType == StatusType.MODIFIED)
        ) {
            val buttonText: String
            val color: Color
            if (diffEntryType is DiffEntryType.StagedDiff) {
                buttonText = "Unstage hunk"
                color = MaterialTheme.colors.error
            } else {
                buttonText = "Stage hunk"
                color = MaterialTheme.colors.primary
            }

            if (diffEntryType is DiffEntryType.UnstagedDiff) {
                SecondaryButton(
                    text = "Discard hunk",
                    backgroundButton = MaterialTheme.colors.error,
                    textColor = MaterialTheme.colors.onError,
                    onClick = onResetHunk
                )
            }

            SecondaryButton(
                text = buttonText,
                backgroundButton = color,
                onClick = {
                    if (diffEntryType is DiffEntryType.StagedDiff) {
                        onUnstageHunk()
                    } else {
                        onStageHunk()
                    }
                }
            )
        }
    }
}

@Composable
private fun DiffHeader(
    diffEntryType: DiffEntryType,
    diffEntry: DiffEntry,
    diffType: TextDiffType,
    onCloseDiffView: () -> Unit,
    onStageFile: (StatusEntry) -> Unit,
    onUnstageFile: (StatusEntry) -> Unit,
    onChangeDiffType: (TextDiffType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colors.tertiarySurface)
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fileName = diffEntry.fileName
        val dirPath: String = diffEntry.parentDirectoryPath

        Row(Modifier.weight(1f, true)) {
            if (dirPath.isNotEmpty()) {
                Text(
                    text = dirPath,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackgroundSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f, false)
                        .padding(start = 16.dp),
                )
            }
            Text(
                text = fileName,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(end = 16.dp),
            )
        }


        Row(verticalAlignment = Alignment.CenterVertically) {
            if (diffEntryType.statusType != StatusType.ADDED && diffEntryType.statusType != StatusType.REMOVED) {
                DiffTypeButtons(diffType = diffType, onChangeDiffType = onChangeDiffType)
            }

            if (diffEntryType is DiffEntryType.UncommitedDiff) {
                UncommitedDiffFileHeaderButtons(
                    diffEntryType,
                    onUnstageFile = onUnstageFile,
                    onStageFile = onStageFile
                )
            }

            IconButton(
                onClick = onCloseDiffView,
                modifier = Modifier
                    .handOnHover()
            ) {
                Image(
                    painter = painterResource("close.svg"),
                    contentDescription = "Close diff",
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
                )
            }
        }
    }
}

@Composable
fun DiffTypeButtons(diffType: TextDiffType, onChangeDiffType: (TextDiffType) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            "Unified",
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.caption,
        )

        Switch(
            checked = diffType == TextDiffType.SPLIT,
            onCheckedChange = { checked ->
                val newType = if (checked)
                    TextDiffType.SPLIT
                else
                    TextDiffType.UNIFIED

                onChangeDiffType(newType)
            },
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colors.secondaryVariant,
                uncheckedTrackColor = MaterialTheme.colors.secondaryVariant,
                uncheckedTrackAlpha = 0.54f
            )
        )

        Text(
            "Split",
            color = MaterialTheme.colors.onBackground,
//            modifier = Modifier.padding(horizontal = 4.dp),
            style = MaterialTheme.typography.caption,
        )
    }
}

@Composable
fun UncommitedDiffFileHeaderButtons(
    diffEntryType: DiffEntryType.UncommitedDiff,
    onUnstageFile: (StatusEntry) -> Unit,
    onStageFile: (StatusEntry) -> Unit
) {
    val buttonText: String
    val color: Color

    if (diffEntryType is DiffEntryType.StagedDiff) {
        buttonText = "Unstage file"
        color = MaterialTheme.colors.error
    } else {
        buttonText = "Stage file"
        color = MaterialTheme.colors.primary
    }

    SecondaryButton(
        text = buttonText,
        backgroundButton = color,
        onClick = {
            if (diffEntryType is DiffEntryType.StagedDiff) {
                onUnstageFile(diffEntryType.statusEntry)
            } else {
                onStageFile(diffEntryType.statusEntry)
            }
        }
    )
}

@Composable
private fun PathOnlyDiffHeader(
    filePath: String,
    onCloseDiffView: () -> Unit,
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
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onCloseDiffView,
            modifier = Modifier
                .handOnHover()
        ) {
            Image(
                painter = painterResource("close.svg"),
                contentDescription = "Close diff",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onBackground),
            )
        }
    }
}

@Composable
fun DiffLine(
    highestLineNumberLength: Int,
    line: Line,
    diffEntryType: DiffEntryType,
    onActionTriggered: () -> Unit,
) {
    val backgroundColor = when (line.lineType) {
        LineType.ADDED -> MaterialTheme.colors.diffLineAdded
        LineType.REMOVED -> MaterialTheme.colors.diffLineRemoved
        LineType.CONTEXT -> MaterialTheme.colors.background
    }

    Row(
        modifier = Modifier
            .background(backgroundColor)
            .height(IntrinsicSize.Min),
    ) {
        val oldLineText = if (line.lineType == LineType.REMOVED || line.lineType == LineType.CONTEXT) {
            line.displayOldLineNumber.toStringWithSpaces(highestLineNumberLength)
        } else
            emptyLineNumber(highestLineNumberLength)

        val newLineText = if (line.lineType == LineType.ADDED || line.lineType == LineType.CONTEXT) {
            line.displayNewLineNumber.toStringWithSpaces(highestLineNumberLength)
        } else
            emptyLineNumber(highestLineNumberLength)

        DisableSelection {
            LineNumber(
                text = oldLineText,
                remarked = line.lineType != LineType.CONTEXT,
            )

            LineNumber(
                text = newLineText,
                remarked = line.lineType != LineType.CONTEXT,
            )
        }

        DiffLineText(line, diffEntryType, onActionTriggered = onActionTriggered)
    }
}

@Composable
fun SplitDiffLine(
    highestLineNumberLength: Int,
    line: Line,
    lineNumber: Int,
    diffEntryType: DiffEntryType,
    onActionTriggered: () -> Unit,
) {
    val backgroundColor = when (line.lineType) {
        LineType.ADDED -> MaterialTheme.colors.diffLineAdded
        LineType.REMOVED -> MaterialTheme.colors.diffLineRemoved
        LineType.CONTEXT -> MaterialTheme.colors.background
    }
    Row(
        modifier = Modifier
            .background(backgroundColor)
            .fillMaxHeight(),
    ) {
        DisableSelection {
            LineNumber(
                text = lineNumber.toStringWithSpaces(highestLineNumberLength),
                remarked = line.lineType != LineType.CONTEXT,
            )
        }

        DiffLineText(line, diffEntryType, onActionTriggered = onActionTriggered)
    }
}


@Composable
fun DiffLineText(line: Line, diffEntryType: DiffEntryType, onActionTriggered: () -> Unit) {
    val text = line.text
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Box(modifier = Modifier.hoverable(hoverInteraction)) {
        if (isHovered && diffEntryType is DiffEntryType.UncommitedDiff && line.lineType != LineType.CONTEXT) {
            val color: Color = if (diffEntryType is DiffEntryType.StagedDiff) {
                MaterialTheme.colors.error
            } else {
                MaterialTheme.colors.primary
            }

            val iconName = remember(diffEntryType) {
                if (diffEntryType is DiffEntryType.StagedDiff) {
                    "remove.svg"
                } else {
                    "add.svg"
                }
            }

            Icon(
                painterResource(iconName),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .fastClickable(line) { onActionTriggered() }
                    .size(14.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }

        Row {
            Text(
                text = text.replace(
                    "\t",
                    "    "
                ).removeLineDelimiters(),
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxSize(),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
                overflow = TextOverflow.Visible,
            )

            val lineDelimiter = text.lineDelimiter

            // Display line delimiter in its own text with a maxLines = 1. This will fix the issue
            // where copying a line didn't contain the line ending & also fix the issue where the text line would
            // display multiple lines even if there is only a single line with a line delimiter at the end
            if (lineDelimiter != null) {
                Text(
                    text = lineDelimiter,
                    maxLines = 1,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }
    }
}

@Composable
fun LineNumber(text: String, remarked: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = 8.dp, end = 4.dp),
        fontFamily = FontFamily.Monospace,
        style = MaterialTheme.typography.body2,
        color = if (remarked) MaterialTheme.colors.onBackground else MaterialTheme.colors.onBackgroundSecondary,
    )
}

fun emptyLineNumber(charactersCount: Int): String {
    val numberBuilder = StringBuilder()
    // Add whitespaces before the numbers
    repeat(charactersCount) {
        numberBuilder.append(" ")
    }

    return numberBuilder.toString()
}