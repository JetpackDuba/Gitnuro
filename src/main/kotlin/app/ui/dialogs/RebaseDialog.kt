package app.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.theme.primaryTextColor
import app.ui.components.PrimaryButton

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RebaseDialog(
    currentBranchName: String,
    rebaseBranchName: String,
    onReject: () -> Unit,
    onAccept: () -> Unit
) {
    MaterialDialog {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currentBranchName,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primaryTextColor,
                )


                Text(
                    text = "will rebase ",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colors.primaryTextColor,
                )

                Text(
                    text = rebaseBranchName,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primaryTextColor,
                )
            }

            Text(
                text = "After completing the operation, $currentBranchName will contain $rebaseBranchName changes",
                color = MaterialTheme.colors.primaryTextColor,
            )

            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .align(Alignment.End)
            ) {
                TextButton(
                    modifier = Modifier.padding(end = 8.dp),
                    onClick = {
                        onReject()
                    }
                ) {
                    Text("Cancel")
                }
                PrimaryButton(
                    onClick = {
                        onAccept()
                    },
                    text = "Rebase"
                )
            }
        }
    }
}