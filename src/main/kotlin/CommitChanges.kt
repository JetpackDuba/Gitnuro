import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.useResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import extensions.*
import kotlinx.coroutines.*
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit
import org.jetbrains.skija.Image.makeFromEncoded
import theme.headerBackground
import theme.primaryTextColor
import theme.secondaryTextColor
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CommitChanges(commitDiff: Pair<RevCommit, List<DiffEntry>>, onDiffSelected: (DiffEntry) -> Unit) {
    val commit = commitDiff.first
    val diff = commitDiff.second

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
                    Text(
                        text = commit.fullMessage,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .padding(8.dp)
                            .verticalScroll(scroll),
                    )

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
                    color = MaterialTheme.colors.primary,
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
                makeFromEncoded(it.toByteArray()).asImageBitmap()
            }
        )
    }

    LaunchedEffect(url) {
        loadImage(url).let {
            image = makeFromEncoded(it).asImageBitmap()
        }
    }

    return image
}

@Composable
fun CommitLogChanges(diffEntries: List<DiffEntry>, onDiffSelected: (DiffEntry) -> Unit) {
    val selectedIndex = remember(diffEntries) { mutableStateOf(-1) }

    LazyColumn(
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
                        tint = MaterialTheme.colors.primary,
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