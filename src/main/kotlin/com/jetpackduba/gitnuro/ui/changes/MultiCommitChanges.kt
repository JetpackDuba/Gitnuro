package com.jetpackduba.gitnuro.ui.changes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.theme.tertiarySurface
import com.jetpackduba.gitnuro.ui.SelectedItem
import com.jetpackduba.gitnuro.ui.components.ScrollableColumn
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.gitnuroViewModel
import com.jetpackduba.gitnuro.viewmodels.CommitChanges
import com.jetpackduba.gitnuro.viewmodels.MultiCommitChangesStatus
import com.jetpackduba.gitnuro.viewmodels.MultiCommitChangesViewModel
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit


@Composable
fun MultiCommitChanges(
    multiCommitChangesViewModel: MultiCommitChangesViewModel = gitnuroViewModel(),
    selectedItem: SelectedItem.MultiCommitBasedItem,
    onDiffSelected: (DiffEntry) -> Unit,
    diffSelected: DiffEntryType?,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
) {
    LaunchedEffect(selectedItem) {
        multiCommitChangesViewModel.loadChanges(selectedItem.itemList)
    }

    val commitChangesStatusState = multiCommitChangesViewModel.commitsChangesStatus.collectAsState()

    when (val commitChangesStatus = commitChangesStatusState.value) {
        MultiCommitChangesStatus.Loading -> {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colors.primaryVariant)
        }

        is MultiCommitChangesStatus.Loaded -> {
            MultiCommitChangesView(
                diffSelected = diffSelected,
                changes = commitChangesStatus.changesList,
                onDiffSelected = onDiffSelected,
                onBlame = onBlame,
                onHistory = onHistory,
            )
        }
    }
}

@Composable
fun MultiCommitChangesView(
    changes: List<CommitChanges>,
    onDiffSelected: (DiffEntry) -> Unit,
    diffSelected: DiffEntryType?,
    onBlame: (String) -> Unit,
    onHistory: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(end = 8.dp, bottom = 8.dp)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .fillMaxWidth()
                .weight(1f, fill = true)
                .background(MaterialTheme.colors.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(MaterialTheme.colors.tertiarySurface),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    modifier = Modifier
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    text = "Files changed",
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Left,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    style = MaterialTheme.typography.body2,
                )
            }

            ScrollableLazyColumn(
                modifier = Modifier
            ) {
                items(changes) {commitChanges ->
                    CommitLogChanges(
                        diffSelected = diffSelected,
                        diffEntries = commitChanges.changes,
                        onDiffSelected = onDiffSelected,
                        onBlame = onBlame,
                        onHistory = onHistory,
                    )

                    Text(
                        text = commitChanges.commit.fullMessage,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    )

                    Divider(
                        color = MaterialTheme.colors.onBackground,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}