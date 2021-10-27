package app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.*
import kotlinx.coroutines.*
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import app.theme.headerBackground
import app.theme.primaryTextColor
import app.theme.secondaryTextColor
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toComposeImageBitmap
import app.ui.components.ScrollableLazyColumn
import app.git.GitManager
import app.theme.headerText
import org.jetbrains.skia.Image.Companion.makeFromEncoded

@Composable
fun CommitChanges(
    gitManager: GitManager,
    commit: RevCommit,
    onDiffSelected: (DiffEntry) -> Unit
) {
    var diff by remember { mutableStateOf(emptyList<DiffEntry>()) }
    LaunchedEffect(commit) {
        diff = gitManager.diffListFromCommit(commit)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            val scroll = rememberScrollState(0)
            Card(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    SelectionContainer {
                        Text(
                            text = commit.fullMessage,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .padding(8.dp)
                                .verticalScroll(scroll),
                        )
                    }

                    Divider(modifier = Modifier.fillMaxWidth())

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val url = "https://www.gravatar.com/avatar/${commit.authorIdent.emailAddress.md5}"
                        Image(
                            bitmap = rememberNetworkImage(url),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .height(40.dp)
                                .clip(CircleShape),
                            contentDescription = null,
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row {
                                Text(
                                    text = commit.authorIdent.name,
                                    color = MaterialTheme.colors.primaryTextColor,
                                    maxLines = 1,
                                    fontSize = 14.sp,
                                )

                                Spacer(modifier = Modifier.weight(1f, fill = true))
                                val date = remember(commit) {
                                    commit.authorIdent.`when`.toSmartSystemString()
                                }

                                Text(
                                    text = date,
                                    color = MaterialTheme.colors.secondaryTextColor,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    fontSize = 12.sp,
                                )

                            }

                            Text(
                                text = commit.authorIdent.emailAddress,
                                color = MaterialTheme.colors.secondaryTextColor,
                                maxLines = 1,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    modifier = Modifier
                        .background(MaterialTheme.colors.headerBackground)
                        .padding(vertical = 8.dp)
                        .fillMaxWidth(),
                    text = "Files changed",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.headerText,
                    maxLines = 1,
                    fontSize = 14.sp,
                )


                CommitLogChanges(diff, onDiffSelected = onDiffSelected)
            }
        }

    }
}

suspend fun loadImage(link: String): ByteArray = withContext(Dispatchers.IO) {
    val url = URL(link)
    val connection = url.openConnection() as HttpURLConnection
    connection.connect()

    connection.inputStream.toByteArray()
}

@Composable
fun rememberNetworkImage(url: String): ImageBitmap {
    var image by remember(url) {
        mutableStateOf<ImageBitmap>(
            useResource("image.jpg") {
                makeFromEncoded(it.toByteArray()).toComposeImageBitmap()
            }
        )
    }


    LaunchedEffect(url) {
        try {
            loadImage(url).let {
                image = makeFromEncoded(it).toComposeImageBitmap()
            }
        } catch (ex: Exception) {
            println("Avatar loading failed: ${ex.message}")
        }
    }


    return image
}

@Composable
fun CommitLogChanges(diffEntries: List<DiffEntry>, onDiffSelected: (DiffEntry) -> Unit) {
    val selectedIndex = remember(diffEntries) { mutableStateOf(-1) }

    ScrollableLazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        itemsIndexed(items = diffEntries) { index, diffEntry ->
            val textColor = if (selectedIndex.value == index) {
                MaterialTheme.colors.primary
            } else
                MaterialTheme.colors.primaryTextColor

            Column(
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
                    .clickable {
                        selectedIndex.value = index
                        onDiffSelected(diffEntry)
                    },
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.weight(2f))


                Row {
                    Icon(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(16.dp),
                        imageVector = diffEntry.icon,
                        contentDescription = null,
                        tint = diffEntry.iconColor,
                    )

                    Text(
                        text = diffEntry.filePath,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = textColor,
                        maxLines = 1,
                        fontSize = 14.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.weight(2f))

                Divider()
            }
        }
    }
}