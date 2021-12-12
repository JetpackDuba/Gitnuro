package app.ui.dialogs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.theme.primaryTextColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MergeDialog(
    currentBranchName: String,
    mergeBranchName: String,
    fastForward: Boolean = false,
    onReject: () -> Unit,
    onAccept: (fastForward: Boolean) -> Unit
) {
    var fastForwardCheck by remember { mutableStateOf(fastForward) }

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
                    text = mergeBranchName,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primaryTextColor,
                )


                Text(
                    text = "will be merged into",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colors.primaryTextColor,
                )

                Text(
                    text = currentBranchName,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.primaryTextColor,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .mouseClickable {
                        if (this.buttons.isPrimaryPressed) {
                            fastForwardCheck = !fastForwardCheck
                        }
                    }
            ) {
                Checkbox(
                    checked = fastForwardCheck,
                    onCheckedChange = { checked ->
                        fastForwardCheck = checked
                    }
                )

                Text(
                    "Fast forward",
                    modifier = Modifier
                        .padding(start = 8.dp)
                )

            }
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
                Button(
                    onClick = {
                        onAccept(fastForwardCheck)
                    }
                ) {
                    Text("Merge")
                }
            }
        }
    }
}