package app.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusOrder
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MergeDialog(
    currentBranchName: String,
    mergeBranchName: String,
    fastForward: Boolean = false,
    onReject: () -> Unit,
    onAccept: (fastForward: Boolean) -> Unit
) {
    var fastForwardCheck by remember { mutableStateOf(fastForward) }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row {
            Text(
                text = currentBranchName,
            )

            Text("  ----------->  ")

            Text(
                text = mergeBranchName,
            )
        }

        Row {
            Checkbox(
                checked = fastForwardCheck,
                onCheckedChange = { checked ->
                    fastForwardCheck = checked
                }
            )

            Text(
                "Fast forward",
                modifier = Modifier.padding(start = 8.dp)
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