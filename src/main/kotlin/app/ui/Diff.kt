package app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ui.components.ScrollableLazyColumn
import app.git.DiffEntryType
import app.git.GitManager
import app.theme.primaryTextColor

@Composable
fun Diff(gitManager: GitManager, diffEntryType: DiffEntryType, onCloseDiffView: () -> Unit) {
    var text by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(diffEntryType.diffEntry) {
        text = gitManager.diffFormat(diffEntryType)


        if(text.isEmpty()) onCloseDiffView()
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .background(MaterialTheme.colors.background)
            .fillMaxSize()
    ) {
        OutlinedButton(
            modifier = Modifier
                .padding(vertical = 16.dp, horizontal = 16.dp)
                .align(Alignment.End),
            onClick = onCloseDiffView,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = MaterialTheme.colors.background,
                contentColor = MaterialTheme.colors.primary,
            )
        ) {
            Text("Close diff")
        }
        SelectionContainer {
            ScrollableLazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                items(text) { line ->
                    val isHunkLine = line.startsWith("@@")

                    val backgroundColor = when {
                        line.startsWith("+") -> {
                            Color(0x77a9d49b)
                        }
                        line.startsWith("-") -> {
                            Color(0x77dea2a2)
                        }
                        isHunkLine -> {
                            MaterialTheme.colors.surface
                        }
                        else -> {
                            MaterialTheme.colors.background
                        }
                    }

                    val paddingTop = if (isHunkLine)
                        32.dp
                    else
                        0.dp

                    Text(
                        text = line,
                        modifier = Modifier
                            .padding(top = paddingTop)
                            .background(backgroundColor)
                            .fillMaxWidth(),
                        color = MaterialTheme.colors.primaryTextColor,
                        maxLines = 1,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                    )
                }
            }
        }

    }
}

