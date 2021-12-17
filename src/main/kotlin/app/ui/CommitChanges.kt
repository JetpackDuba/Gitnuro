package app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.extensions.*
import app.git.GitManager
import app.theme.headerBackground
import app.theme.headerText
import app.theme.primaryTextColor
import app.theme.secondaryTextColor
import app.ui.components.AvatarImage
import app.ui.components.ScrollableLazyColumn
import app.ui.components.TooltipText
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit

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
            .fillMaxSize(),
    ) {
        val scroll = rememberScrollState(0)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            SelectionContainer {
                Text(
                    text = commit.fullMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colors.primaryTextColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.background)
                        .height(120.dp)
                        .padding(8.dp)
                        .verticalScroll(scroll),
                )
            }

            Divider(modifier = Modifier.fillMaxWidth())

            Author(commit)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .background(MaterialTheme.colors.background)
        ) {
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

@Composable
fun Author(commit: RevCommit) {
    val authorIdent = commit.authorIdent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colors.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(40.dp),
            personIdent = commit.authorIdent,
        )

        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                TooltipText(
                    text = authorIdent.name,
                    color = MaterialTheme.colors.primaryTextColor,
                    maxLines = 1,
                    fontSize = 14.sp,
                    tooltipTitle = authorIdent.emailAddress,
                )

                Spacer(modifier = Modifier.weight(1f, fill = true))

                val date = remember(authorIdent) {
                    authorIdent.`when`.toSmartSystemString()
                }

                TooltipText(
                    text = date,
                    color = MaterialTheme.colors.secondaryTextColor,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 12.sp,
                    tooltipTitle = authorIdent.`when`.toSystemDateTimeString()
                )

            }

            Text(
                text = commit.id.abbreviate(7).name(),
                color = MaterialTheme.colors.secondaryTextColor,
                maxLines = 1,
                fontSize = 12.sp,
            )
        }
    }
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