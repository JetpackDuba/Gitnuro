package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.git.DiffEntryType
import app.git.diff.DiffResult
import app.git.diff.Hunk
import app.git.diff.Line
import app.git.diff.LineType
import app.theme.primaryTextColor
import app.ui.components.ScrollableLazyColumn
import app.ui.components.SecondaryButton
import app.viewmodels.DiffViewModel
import org.eclipse.jgit.diff.DiffEntry
import java.io.FileInputStream
import kotlin.io.path.absolutePathString
import kotlin.math.max

@Composable
fun Diff(
    diffViewModel: DiffViewModel,
    onCloseDiffView: () -> Unit,
) {
    val diffResultState = diffViewModel.diffResult.collectAsState()
    val viewDiffResult = diffResultState.value ?: return

    val diffEntryType = viewDiffResult.diffEntryType
    val diffEntry = diffEntryType.diffEntry
    val diffResult = viewDiffResult.diffResult

    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        DiffHeader(diffEntry, onCloseDiffView)
        if (diffResult is DiffResult.Text) {
            TextDiff(diffEntryType, diffViewModel, diffResult)
        } else if (diffResult is DiffResult.Images) {
            ImagesDiff(diffResult)
        }
    }
}

@Composable
fun ImagesDiff(diffResult: DiffResult.Images) {
    val oldImagePath = diffResult.oldTempFile
    val newImagePath = diffResult.newTempsFile
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Red),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = loadImageBitmap(inputStream = FileInputStream(oldImagePath.absolutePathString())),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.5f)
                .background(Color.Yellow),
        )
        Spacer(
            modifier = Modifier.fillMaxWidth(0.1f)
                .background(Color.Green),
        )
        Image(
            bitmap = loadImageBitmap(inputStream = FileInputStream(newImagePath.absolutePathString())),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth()
                .background(Color.Blue),
        )
    }
}

@Composable
fun TextDiff(diffEntryType: DiffEntryType, diffViewModel: DiffViewModel, diffResult: DiffResult.Text) {
    val hunks = diffResult.hunks

    val scrollState by diffViewModel.lazyListState.collectAsState()
    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        state = scrollState
    ) {
        items(hunks) { hunk ->
            HunkHeader(
                hunk = hunk,
                diffEntryType = diffEntryType,
                diffViewModel = diffViewModel,
            )

            SelectionContainer {
                Column {
                    val oldHighestLineNumber = hunk.lines.maxOf { it.displayOldLineNumber }
                    val newHighestLineNumber = hunk.lines.maxOf { it.displayNewLineNumber }
                    val highestLineNumber = max(oldHighestLineNumber, newHighestLineNumber)
                    val highestLineNumberLength = highestLineNumber.toString().count()

                    hunk.lines.forEach { line ->
                        DiffLine(highestLineNumberLength, line)
                    }
                }
            }
        }
    }

}

@Composable
fun HunkHeader(
    hunk: Hunk,
    diffEntryType: DiffEntryType,
    diffViewModel: DiffViewModel,
) {
    Row(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .padding(vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = hunk.header,
            color = MaterialTheme.colors.primaryTextColor,
            fontSize = 13.sp,
        )

        Spacer(modifier = Modifier.weight(1f))
        if (
            (diffEntryType is DiffEntryType.StagedDiff || diffEntryType is DiffEntryType.UnstagedDiff) &&
            diffEntryType.diffEntry.changeType == DiffEntry.ChangeType.MODIFY
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

            SecondaryButton(
                text = buttonText,
                backgroundButton = color,
                onClick = {
                    if (diffEntryType is DiffEntryType.StagedDiff) {
                        diffViewModel.unstageHunk(diffEntryType.diffEntry, hunk)
                    } else {
                        diffViewModel.stageHunk(diffEntryType.diffEntry, hunk)
                    }
                }
            )
        }
    }
}

@Composable
fun DiffHeader(diffEntry: DiffEntry, onCloseDiffView: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val filePath = if (diffEntry.newPath != "/dev/null")
            diffEntry.newPath
        else
            diffEntry.oldPath

        Text(
            text = filePath,
            color = MaterialTheme.colors.primaryTextColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onCloseDiffView
        ) {
            Image(
                painter = painterResource("close.svg"),
                contentDescription = "Close diff",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primaryTextColor),
            )
        }
    }
}

@Composable
fun DiffLine(highestLineNumberLength: Int, line: Line) {
    val backgroundColor = when (line.lineType) {
        LineType.ADDED -> {
            Color(0x77a9d49b)
        }
        LineType.REMOVED -> {
            Color(0x77dea2a2)
        }
        LineType.CONTEXT -> {
            MaterialTheme.colors.background
        }
    }
    Row(
        modifier = Modifier
            .background(backgroundColor)
    ) {
        val oldLineText = if (line.lineType == LineType.REMOVED || line.lineType == LineType.CONTEXT) {
            formattedLineNumber(line.displayOldLineNumber, highestLineNumberLength)
        } else
            emptyLineNumber(highestLineNumberLength)

        val newLineText = if (line.lineType == LineType.ADDED || line.lineType == LineType.CONTEXT) {
            formattedLineNumber(line.displayNewLineNumber, highestLineNumberLength)
        } else
            emptyLineNumber(highestLineNumberLength)

        DisableSelection {
            LineNumber(
                text = oldLineText,
            )

            LineNumber(
                text = newLineText
            )
        }

        Text(
            text = line.text,
            modifier = Modifier
                .padding(start = 8.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colors.primaryTextColor,
            maxLines = 1,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun LineNumber(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colors.primaryTextColor,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 4.dp),
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
    )
}


fun formattedLineNumber(number: Int, charactersCount: Int): String {
    val numberStr = number.toString()
    return if (numberStr.count() == charactersCount)
        numberStr
    else {
        val lengthDiff = charactersCount - numberStr.count()
        val numberBuilder = StringBuilder()
        // Add whitespaces before the numbers
        repeat(lengthDiff) {
            numberBuilder.append(" ")
        }
        numberBuilder.append(numberStr)

        numberBuilder.toString()
    }
}

fun emptyLineNumber(charactersCount: Int): String {
    val numberBuilder = StringBuilder()
    // Add whitespaces before the numbers
    repeat(charactersCount) {
        numberBuilder.append(" ")
    }

    return numberBuilder.toString()
}

