@file:Suppress("UNUSED_PARAMETER")

package app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import app.ui.components.ScrollableLazyColumn
import org.eclipse.jgit.blame.BlameResult

@Composable
fun Blame(
    filePath: String,
    blameResult: BlameResult,
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
                        modifier = Modifier.width(200.dp).fillMaxHeight().background(MaterialTheme.colors.surface),
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
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    filePath: String,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colors.surface),
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
                contentDescription = "Close diff",
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primaryTextColor),
            )
        }
    }
}