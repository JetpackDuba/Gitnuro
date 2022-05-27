package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.lineAt
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
    Column {
        Header(filePath, onClose = onClose)

        ScrollableLazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            val contents = blameResult.resultContents
            items(contents.size()) { index ->
                val line = contents.lineAt(index)
                val author = blameResult.getSourceAuthor(index)
                val commit = blameResult.getSourceCommit(index)

                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.background)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .width(200.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colors.surface)
                            .clickable { onSelectCommit(commit) },
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
                            text = commit.shortMessage,
                            color = MaterialTheme.colors.primaryTextColor,
                            maxLines = 1,
                            modifier = Modifier.padding(start = 16.dp),
                            fontSize = 10.sp,
                        )
                    }

                    Text(
                        text = line,
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

@Composable
fun MinimizedBlame(
    filePath: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
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
            onClick = onClose
        ) {
            Image(
                painter = painterResource("close.svg"),
                contentDescription = "Close blame",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primaryTextColor),
            )
        }
    }
}