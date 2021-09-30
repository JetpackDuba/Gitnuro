import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import extensions.simpleName
import org.eclipse.jgit.lib.Ref
import theme.headerBackground

@Composable
fun Branches(gitManager: GitManager) {
    val branches by gitManager.branches.collectAsState()
    val currentBranch by gitManager.currentBranch.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.5f)
            .padding(8.dp)
    ) {
        Column {
            Text(
                modifier = Modifier
                    .background(MaterialTheme.colors.headerBackground)
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                text = "Local branches",
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 14.sp,
                maxLines = 1,
            )

            LazyColumn(modifier = Modifier.weight(5f)) {
                itemsIndexed(branches) { _, branch ->
                    BranchRow(
                        branch = branch,
                        isCurrentBranch = currentBranch == branch.name
                    )

                }
            }
        }
    }
}

@Composable
private fun BranchRow(
    branch: Ref,
    isCurrentBranch: Boolean
) {
    val fontWeight = if(isCurrentBranch)
        FontWeight.Bold
    else
        FontWeight.Normal

    Row(
        modifier = Modifier
            .height(40.dp)
            .fillMaxWidth()
            .clickable(onClick = {}),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(
            painter = painterResource("branch.svg"),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(16.dp),
            tint = MaterialTheme.colors.primary,
        )

        Text(
            text = branch.simpleName,
            fontWeight = fontWeight,
            modifier = Modifier.weight(1f, fill = true),
            maxLines = 1,
            fontSize = 14.sp,
            overflow = TextOverflow.Ellipsis,
        )

        IconButton(
            onClick = {},
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}