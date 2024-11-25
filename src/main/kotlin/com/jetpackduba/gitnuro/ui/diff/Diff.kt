@file:OptIn(ExperimentalComposeUiApi::class)

package com.jetpackduba.gitnuro.ui.diff

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.LocalTextContextMenu
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetpackduba.gitnuro.AppIcons
import com.jetpackduba.gitnuro.extensions.*
import com.jetpackduba.gitnuro.git.CloseableView
import com.jetpackduba.gitnuro.git.DiffType
import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.animatedImages
import com.jetpackduba.gitnuro.git.diff.*
import com.jetpackduba.gitnuro.git.workspace.StatusEntry
import com.jetpackduba.gitnuro.git.workspace.StatusType
import com.jetpackduba.gitnuro.theme.*
import com.jetpackduba.gitnuro.ui.components.PrimaryButton
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.SecondaryButton
import com.jetpackduba.gitnuro.ui.components.tooltip.DelayedTooltip
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenu
import com.jetpackduba.gitnuro.ui.context_menu.ContextMenuElement
import com.jetpackduba.gitnuro.ui.context_menu.SelectionAwareTextContextMenu
import com.jetpackduba.gitnuro.ui.diff.syntax_highlighter.SyntaxHighlighter
import com.jetpackduba.gitnuro.ui.diff.syntax_highlighter.getSyntaxHighlighterFromExtension
import com.jetpackduba.gitnuro.viewmodels.DiffViewModel
import com.jetpackduba.gitnuro.viewmodels.TextDiffType
import com.jetpackduba.gitnuro.viewmodels.ViewDiffResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.submodule.SubmoduleStatusType
import org.jetbrains.compose.animatedimage.Blank
import org.jetbrains.compose.animatedimage.animate
import org.jetbrains.compose.animatedimage.loadAnimatedImage
import java.io.FileInputStream
import kotlin.math.max

private const val MAX_MOVES_COUNT = 5

@Composable
private fun <T> loadOrNull(key: Any, action: suspend () -> T?): T? {
    var result: T? by remember(key) { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        result = action()
    }
    return result
}

@Composable
fun Diff(
    diffViewModel: DiffViewModel,
    onCloseDiffView: () -> Unit,
) {
    val diffResultState = diffViewModel.diffResult.collectAsState()
    val textDiffType by diffViewModel.diffTypeFlow.collectAsState()
    val isDisplayFullFile by diffViewModel.isDisplayFullFile.collectAsState()
    val viewDiffResult = diffResultState.value ?: return
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        diffViewModel.closeViewFlow.collectLatest {
            if (it == CloseableView.DIFF) onCloseDiffView()
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
            .focusable(true)
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    diffViewModel.addToCloseables()
                }
            }
    ) {
        when (viewDiffResult) {
            ViewDiffResult.DiffNotFound -> {
                onCloseDiffView()
            }

            is ViewDiffResult.Loaded -> {
                val diffType = viewDiffResult.diffType
                val diffEntry = viewDiffResult.diffResult.diffEntry
                val diffResult = viewDiffResult.diffResult

                DiffHeader(
                    diffType = diffType,
                    diffEntry = diffEntry,
                    onCloseDiffView = onCloseDiffView,
                    textDiffType = textDiffType,
                    isTextDiff = diffResult is DiffResult.TextDiff,
                    isDisplayFullFile = isDisplayFullFile,
                    onStageFile = { diffViewModel.stageFile(it) },
                    onUnstageFile = { diffViewModel.unstageFile(it) },
                    onChangeDiffType = { diffViewModel.changeTextDiffType(it) },
                    onDisplayFullFile = { diffViewModel.changeDisplayFullFile(it) },
                )

                val scrollState by diffViewModel.lazyListState.collectAsState()

                when (diffResult) {
                    is DiffResult.TextSplit -> HunkSplitTextDiff(
                        diffType = diffType,
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
                        onUnStageLine = { entry, hunk, line ->
                            if (diffType is DiffType.UnstagedDiff)
                                diffViewModel.stageHunkLine(entry, hunk, line)
                            else if (diffType is DiffType.StagedDiff)
                                diffViewModel.unstageHunkLine(entry, hunk, line)
                        },
                        onDiscardLine = { entry, hunk, line ->
                            diffViewModel.discardHunkLine(entry, hunk, line)
                        }
                    )

                    is DiffResult.Text -> HunkUnifiedTextDiff(
                        diffType = diffType,
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
                        onUnStageLine = { entry, hunk, line ->
                            if (diffType is DiffType.UnstagedDiff)
                                diffViewModel.stageHunkLine(entry, hunk, line)
                            else if (diffType is DiffType.StagedDiff)
                                diffViewModel.unstageHunkLine(entry, hunk, line)
                        },
                        onDiscardLine = { entry, hunk, line ->
                            diffViewModel.discardHunkLine(entry, hunk, line)
                        }
                    )

                    is DiffResult.NonText -> {
                        NonTextDiff(
                            diffResult,
                            onOpenFileWithExternalApp = { path -> diffViewModel.openFileWithExternalApp(path) })
                    }

                    is DiffResult.Submodule -> {
                        SubmoduleDiff(
                            diffResult,
                            onOpenSubmodule = { diffViewModel.openSubmodule(diffResult.diffEntry.filePath) }
                        )
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
fun NonTextDiff(diffResult: DiffResult.NonText, onOpenFileWithExternalApp: (String) -> Unit) {
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
                SideDiff(oldBinaryContent, onOpenFileWithExternalApp)
            }
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .padding(start = 8.dp, end = 24.dp, top = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                SideTitle("New")
                SideDiff(newBinaryContent, onOpenFileWithExternalApp)
            }
        } else if (oldBinaryContent != EntryContent.Missing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 24.dp),
            ) {
                SideDiff(oldBinaryContent, onOpenFileWithExternalApp)
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
                SideDiff(newBinaryContent, onOpenFileWithExternalApp)
            }
        }
    }
}

@Composable
fun SubmoduleDiff(diffResult: DiffResult.Submodule, onOpenSubmodule: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Submodule ${diffResult.diffEntry.filePath}",
            style = MaterialTheme.typography.h4,
            modifier = Modifier.padding(bottom = 8.dp),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colors.onBackground,
        )

        SelectionContainer {
            Column {
                Text(
                    AnnotatedString(
                        "Old ID: ",
                        SpanStyle(fontWeight = FontWeight.SemiBold)
                    ) + AnnotatedString(diffResult.diffEntry.oldId.name()),
                    color = MaterialTheme.colors.onBackground,
                )
                Text(
                    AnnotatedString(
                        "New ID: ",
                        SpanStyle(fontWeight = FontWeight.SemiBold)
                    ) + AnnotatedString(diffResult.diffEntry.newId.name()),
                    color = MaterialTheme.colors.onBackground,
                )
            }
        }

        val submoduleStatus = diffResult.submoduleStatus

        if (
            submoduleStatus != null &&
            listOf(
                SubmoduleStatusType.INITIALIZED,
                SubmoduleStatusType.REV_CHECKED_OUT
            ).contains(submoduleStatus.type) &&
            submoduleStatus.indexId == diffResult.diffEntry.newId?.toObjectId()
        ) {
            PrimaryButton(
                modifier = Modifier.padding(top = 8.dp),
                text = "Open submodule",
                onClick = onOpenSubmodule,
            )
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
fun SideDiff(entryContent: EntryContent, onOpenFileWithExternalApp: (String) -> Unit) {
    when (entryContent) {
        EntryContent.Binary -> BinaryDiff()
        is EntryContent.ImageBinary -> ImageDiff(
            entryContent.imagePath,
            entryContent.contentType,
            onOpenFileWithExternalApp = { onOpenFileWithExternalApp(entryContent.imagePath) }
        )

        else -> {
        }
//        is EntryContent.Text -> //TODO maybe have a text view if the file was a binary before?
// TODO Show some info about this       EntryContent.TooLargeEntry -> TODO()
    }
}

@Composable
private fun ImageDiff(
    imagePath: String,
    contentType: String,
    onOpenFileWithExternalApp: () -> Unit
) {
    if (animatedImages.contains(contentType)) {
        AnimatedImage(imagePath, onOpenFileWithExternalApp)
    } else {
        StaticImage(imagePath, onOpenFileWithExternalApp)
    }
}

@Composable
private fun StaticImage(
    tempImagePath: String,
    onOpenFileWithExternalApp: () -> Unit
) {
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
                onOpenFileWithExternalApp()
            }
    )
}

@Composable
private fun AnimatedImage(
    imagePath: String,
    onOpenFileWithExternalApp: () -> Unit
) {
    Image(
        bitmap = loadOrNull(imagePath) { loadAnimatedImage(imagePath) }?.animate() ?: ImageBitmap.Blank,
        contentDescription = null,
        modifier = Modifier.fillMaxSize()
            .handMouseClickable {
                onOpenFileWithExternalApp()
            }
    )
}

@Composable
fun BinaryDiff() {
    Image(
        painter = painterResource(AppIcons.BINARY),
        contentDescription = null,
        modifier = Modifier.width(400.dp),
        colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
    )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HunkUnifiedTextDiff(
    diffType: DiffType,
    scrollState: LazyListState,
    diffResult: DiffResult.Text,
    onUnstageHunk: (DiffEntry, Hunk) -> Unit,
    onStageHunk: (DiffEntry, Hunk) -> Unit,
    onUnStageLine: (DiffEntry, Hunk, Line) -> Unit,
    onResetHunk: (DiffEntry, Hunk) -> Unit,
    onDiscardLine: (DiffEntry, Hunk, Line) -> Unit,
) {
    val hunks = diffResult.hunks
    var selectedText by remember { mutableStateOf(AnnotatedString("")) }

    CompositionLocalProvider(
        LocalTextContextMenu provides SelectionAwareTextContextMenu {
            selectedText = it
        }
    ) {
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
                                diffType = diffType,
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
                        DiffContextMenu(
                            selectedText = selectedText,
                            diffType = diffType,
                            onDiscardLine = { onDiscardLine(diffResult.diffEntry, hunk, line) },
                            line = line,
                        ) {
                            DiffLine(
                                highestLineNumberLength,
                                line,
                                diffType = diffType,
                                onActionTriggered = {
                                    onUnStageLine(
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
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HunkSplitTextDiff(
    diffType: DiffType,
    scrollState: LazyListState,
    diffResult: DiffResult.TextSplit,
    onUnstageHunk: (DiffEntry, Hunk) -> Unit,
    onStageHunk: (DiffEntry, Hunk) -> Unit,
    onResetHunk: (DiffEntry, Hunk) -> Unit,
    onUnStageLine: (DiffEntry, Hunk, Line) -> Unit,
    onDiscardLine: (DiffEntry, Hunk, Line) -> Unit,
) {
    val hunks = diffResult.hunks

    /**
     * Disables selection in one side when the other is being selected
     */
    var selectableSide by remember { mutableStateOf(SelectableSide.BOTH) }

    var selectedText by remember { mutableStateOf(AnnotatedString("")) }

    CompositionLocalProvider(
        LocalTextContextMenu provides SelectionAwareTextContextMenu {
            selectedText = it
        }
    ) {
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
                                diffType = diffType,
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
                            diffType = diffType,
                            selectedText = selectedText,
                            onActionTriggered = { line ->
                                onUnStageLine(diffResult.diffEntry, splitHunk.sourceHunk, line)
                            },
                            onChangeSelectableSide = { newSelectableSide ->
                                if (newSelectableSide != selectableSide) {
                                    selectableSide = newSelectableSide
                                }
                            },
                            onDiscardLine = { line ->
                                onDiscardLine(diffResult.diffEntry, splitHunk.sourceHunk, line)
                            }
                        )
                    }
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
    diffType: DiffType,
    selectedText: AnnotatedString,
    onChangeSelectableSide: (SelectableSide) -> Unit,
    onActionTriggered: (Line) -> Unit,
    onDiscardLine: (Line) -> Unit,
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
            diffType = diffType,
            onActionTriggered = { if (oldLine != null) onActionTriggered(oldLine) },
            selectedText = selectedText,
            onDiscardLine = onDiscardLine,
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
            diffType = diffType,
            onActionTriggered = { if (newLine != null) onActionTriggered(newLine) },
            selectedText = selectedText,
            onDiscardLine = onDiscardLine,
        )

    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SplitDiffLineSide(
    modifier: Modifier,
    highestLineNumberLength: Int,
    line: Line?,
    displayLineNumber: Int,
    currentSelectableSide: SelectableSide,
    lineSelectableSide: SelectableSide,
    diffType: DiffType,
    selectedText: AnnotatedString,
    onChangeSelectableSide: (SelectableSide) -> Unit,
    onActionTriggered: () -> Unit,
    onDiscardLine: (Line) -> Unit,
) {
    var pressedAndMoved by remember(line) { mutableStateOf(Pair(false, false)) }
    var movesCount by remember(line) { mutableStateOf(0) }

    Box(
        modifier = modifier
            .onPointerEvent(PointerEventType.Press) {
                movesCount = 0
                pressedAndMoved = pressedAndMoved.copy(first = true, second = false)
                onChangeSelectableSide(SelectableSide.BOTH)

            }
            .onPointerEvent(PointerEventType.Release) {
                pressedAndMoved = pressedAndMoved.copy(first = false)

                // When using DynamicDisableSelection, there is a bug in compose where ctrl+C copies different stuff
                // than using the contextual menu Copy.
                // Ctrl+c copies everything that is not currently contained in the DisableSelection block,
                // even if it was during text selection. The context menu only copies what is currently selected.
                //
                // With this workaround, both sides are enabled if the mouse hasn't been moved (or not enough
                // to select something)
                if (movesCount < MAX_MOVES_COUNT)
                    onChangeSelectableSide(SelectableSide.BOTH)
            }
            .onPointerEvent(PointerEventType.Move) {
                movesCount++

                if (pressedAndMoved.first)
                    onChangeSelectableSide(lineSelectableSide)
            }
    ) {
        if (line != null) {
            // To avoid both sides being selected, disable one side when the use is interacting with the other
            DynamicSelectionDisable(
                currentSelectableSide != lineSelectableSide &&
                        currentSelectableSide != SelectableSide.BOTH
            ) {
                DiffContextMenu(
                    selectedText,
                    line,
                    diffType,
                    onDiscardLine = { onDiscardLine(line) },
                ) {
                    SplitDiffLine(
                        highestLineNumberLength = highestLineNumberLength,
                        line = line,
                        lineNumber = displayLineNumber,
                        diffType = diffType,
                        onActionTriggered = onActionTriggered,
                    )
                }
            }
        }
    }
}

@Composable
fun DiffContextMenu(
    selectedText: AnnotatedString,
    line: Line,
    diffType: DiffType,
    onDiscardLine: () -> Unit,
    content: @Composable () -> Unit,
) {
    ContextMenu(
        enabled = selectedText.isEmpty(),
        items = {
            if (
                line.lineType != LineType.CONTEXT &&
                diffType is DiffType.UnstagedDiff &&
                diffType.statusType == StatusType.MODIFIED
            ) {
                listOf(
                    ContextMenuElement.ContextTextEntry(
                        label = "Discard line",
                        icon = { painterResource(AppIcons.UNDO) },
                        onClick = {
                            onDiscardLine()
                        }
                    )
                )
            } else
                emptyList()
        },
    ) {
        content()
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
    diffType: DiffType,
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
            (diffType is DiffType.SafeStagedDiff || diffType is DiffType.SafeUnstagedDiff) &&
            diffType.statusType == StatusType.MODIFIED
        ) {
            val buttonText: String
            val color: Color
            if (diffType is DiffType.StagedDiff) {
                buttonText = "Unstage hunk"
                color = MaterialTheme.colors.error
            } else {
                buttonText = "Stage hunk"
                color = MaterialTheme.colors.primary
            }

            if (diffType is DiffType.UnstagedDiff) {
                SecondaryButton(
                    text = "Discard hunk",
                    backgroundButton = MaterialTheme.colors.error,
                    textColor = MaterialTheme.colors.onError,
                    onClick = onResetHunk,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            SecondaryButton(
                text = buttonText,
                backgroundButton = color,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    if (diffType is DiffType.StagedDiff) {
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
    diffType: DiffType,
    diffEntry: DiffEntry,
    textDiffType: TextDiffType,
    isDisplayFullFile: Boolean,
    isTextDiff: Boolean,
    onCloseDiffView: () -> Unit,
    onStageFile: (StatusEntry) -> Unit,
    onUnstageFile: (StatusEntry) -> Unit,
    onChangeDiffType: (TextDiffType) -> Unit,
    onDisplayFullFile: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .background(MaterialTheme.colors.tertiarySurface)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fileName = diffEntry.fileName
        val dirPath: String = diffEntry.parentDirectoryPath

        Box(
            modifier = Modifier
                .weight(1f, true)
        ) {
            SelectionContainer {
                Row {
                    if (dirPath.isNotEmpty()) {
                        Text(
                            text = dirPath.removeSuffix("/"),
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onBackgroundSecondary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .weight(1f, false),
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
                        text = fileName,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (diffType.statusType != StatusType.ADDED && diffType.statusType != StatusType.REMOVED && isTextDiff) {
                DiffTypeButtons(
                    diffType = textDiffType,
                    isDisplayFullFile = isDisplayFullFile,
                    onChangeDiffType = onChangeDiffType,
                    onDisplayFullFile = onDisplayFullFile,
                )
            }

            if (diffType is DiffType.UncommittedDiff) {
                UncommittedDiffFileHeaderButtons(
                    diffType,
                    onUnstageFile = onUnstageFile,
                    onStageFile = onStageFile
                )
            }

            IconButton(
                onClick = onCloseDiffView,
                modifier = Modifier
                    .handOnHover()
            ) {
                Icon(
                    painter = painterResource(AppIcons.CLOSE),
                    contentDescription = "Close diff",
                    tint = MaterialTheme.colors.onBackground,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
fun StateIcon(
    icon: String,
    tooltip: String,
    isToggled: Boolean,
    onClick: () -> Unit,
) {
    DelayedTooltip(tooltip) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .run {
                    if (isToggled)
                        this.background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                    else
                        this
                }
                .handMouseClickable { if (!isToggled) onClick() }
                .padding(4.dp)
        ) {
            Icon(
                painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colors.onSurface,
                modifier = Modifier
                    .size(24.dp),
            )
        }
    }
}

@Composable
fun DiffTypeButtons(
    diffType: TextDiffType,
    isDisplayFullFile: Boolean,
    onChangeDiffType: (TextDiffType) -> Unit,
    onDisplayFullFile: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            StateIcon(
                icon = AppIcons.HORIZONTAL_SPLIT,
                tooltip = "Divide by hunks",
                isToggled = !isDisplayFullFile,
                onClick = { onDisplayFullFile(false) },
            )

            StateIcon(
                icon = AppIcons.DESCRIPTION,
                tooltip = "View the complete file",
                isToggled = isDisplayFullFile,
                onClick = { onDisplayFullFile(true) },
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StateIcon(
                icon = AppIcons.UNIFIED,
                tooltip = "Unified diff",
                isToggled = diffType == TextDiffType.UNIFIED,
                onClick = { onChangeDiffType(TextDiffType.UNIFIED) },
            )

            StateIcon(
                icon = AppIcons.VERTICAL_SPLIT,
                tooltip = "Split diff",
                isToggled = diffType == TextDiffType.SPLIT,
                onClick = { onChangeDiffType(TextDiffType.SPLIT) },
            )
        }
    }
}

@Composable
fun UncommittedDiffFileHeaderButtons(
    diffType: DiffType.UncommittedDiff,
    onUnstageFile: (StatusEntry) -> Unit,
    onStageFile: (StatusEntry) -> Unit
) {
    val buttonText: String
    val color: Color

    if (diffType is DiffType.StagedDiff) {
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
            if (diffType is DiffType.StagedDiff) {
                onUnstageFile(diffType.statusEntry)
            } else {
                onStageFile(diffType.statusEntry)
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
                painter = painterResource(AppIcons.CLOSE),
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
    diffType: DiffType,
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

        DiffLineText(line, diffType, onActionTriggered = onActionTriggered)
    }
}

@Composable
fun SplitDiffLine(
    highestLineNumberLength: Int,
    line: Line,
    lineNumber: Int,
    diffType: DiffType,
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

        DiffLineText(line, diffType, onActionTriggered = onActionTriggered)
    }
}


@Composable
fun DiffLineText(line: Line, diffType: DiffType, onActionTriggered: () -> Unit) {
    val fileExtension = diffType.filePath.split(".").lastOrNull()
    val syntaxHighlighter = getSyntaxHighlighterFromExtension(fileExtension)

    val text = line.text
    val matchLine = line.textDiffed
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()

    Box(modifier = Modifier.hoverable(hoverInteraction)) {
        if (
            isHovered &&
            diffType is DiffType.UncommittedDiff &&
            line.lineType != LineType.CONTEXT &&
            diffType.statusType == StatusType.MODIFIED
        ) {
            val color: Color = if (diffType is DiffType.StagedDiff) {
                MaterialTheme.colors.error
            } else {
                MaterialTheme.colors.primary
            }

            val iconName = remember(diffType) {
                if (diffType is DiffType.StagedDiff) {
                    AppIcons.REMOVE
                } else {
                    AppIcons.ADD
                }
            }

            Icon(
                painterResource(iconName),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .clickable { onActionTriggered() }
                    .size(16.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }

        DiffText(text, matchLine, syntaxHighlighter)
    }
}

@Composable
fun DiffText(text: String, matchLine: MatchLine?, syntaxHighlighter: SyntaxHighlighter) {
    val line = matchLine ?: MatchLine(listOf(DiffMatchPatch.Diff(DiffMatchPatch.Operation.EQUAL, text)))

    Row {
        val diffContentRemoved = MaterialTheme.colors.diffContentRemoved
        val diffComment = MaterialTheme.colors.diffComment
        val diffKeyword = MaterialTheme.colors.diffKeyword
        val diffAnnotation = MaterialTheme.colors.diffAnnotation
        val diffContentAdded = MaterialTheme.colors.diffContentAdded

        val annotatedString = remember(line) {
            formatDiff(
                line = line,
                commentColor = diffComment,
                keywordColor = diffKeyword,
                annotationColor = diffAnnotation,
                contentAddedColor = diffContentAdded,
                contentRemovedColor = diffContentRemoved,
                syntaxHighlighter = syntaxHighlighter,
            )
        }

        Text(
            text = annotatedString,
            modifier = Modifier
                .padding(start = 16.dp)
                .fillMaxWidth(),
            fontFamily = notoSansMonoFontFamily,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground,
            overflow = TextOverflow.Visible,
            softWrap = true,
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

@Composable
fun LineNumber(text: String, remarked: Boolean) {
    Text(
        text = text,
        modifier = Modifier
            .padding(start = 8.dp, end = 4.dp),
        fontFamily = notoSansMonoFontFamily,
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

fun formatDiff(
    line: MatchLine,
    commentColor: Color,
    keywordColor: Color,
    annotationColor: Color,
    contentAddedColor: Color,
    contentRemovedColor: Color,
    syntaxHighlighter: SyntaxHighlighter,
): AnnotatedString {
    val isAllSameType = line.diffs
        .filter { it.text != "\n" }
        .map { it.operation }
        .count() == 1

    val diffBuilder = AnnotatedString.Builder()
    val diffs = line.diffs

    diffs
        .forEach { diff ->
            val color = if (isAllSameType) {
                Color.Transparent
            } else {
                when (diff.operation) {
                    DiffMatchPatch.Operation.DELETE -> contentRemovedColor
                    DiffMatchPatch.Operation.INSERT -> contentAddedColor
                    else -> Color.Transparent
                }
            }

            val newAnnotatedString = AnnotatedString(
                text = diff.text
                    .replaceTabs()
                    .removeLineDelimiters(),
                spanStyle = SpanStyle(background = color),
            )

            diffBuilder.append(newAnnotatedString)
        }

    val annotatedString = diffBuilder.toAnnotatedString()

    return syntaxHighlighter.syntaxHighlight(
        annotatedString = annotatedString,
        commentColor = commentColor,
        keywordColor = keywordColor,
        annotationColor = annotationColor,
    )
}