package app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.git.CloneStatus
import app.git.TabViewModel
import app.theme.primaryTextColor
import java.io.File

@Composable
fun CloneDialog(
    gitManager: TabViewModel,
    onClose: () -> Unit
) {
    val cloneStatus = gitManager.cloneStatus.collectAsState()
    val cloneStatusValue = cloneStatus.value
    var directory by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    Column {
        if (cloneStatusValue is CloneStatus.Cloning || cloneStatusValue == CloneStatus.CheckingOut)
            LinearProgressIndicator(modifier = Modifier.width(500.dp))
        else if (cloneStatusValue == CloneStatus.Completed) {
            gitManager.openRepository(directory)
            onClose()
        }

        Text(
            "Clone a repository",
            color = MaterialTheme.colors.primaryTextColor,
        )

        OutlinedTextField(
            modifier = Modifier
                .width(400.dp)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            label = { Text("URL") },
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
            maxLines = 1,
            value = url,
            onValueChange = {
                url = it
            }
        )

        OutlinedTextField(
            modifier = Modifier
                .width(400.dp)
                .padding(vertical = 4.dp, horizontal = 8.dp),
            label = { Text("Directory") },
            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colors.primaryTextColor),
            maxLines = 1,
            value = directory,
            onValueChange = {
                directory = it
            }
        )

        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .align(Alignment.End)
        ) {
            TextButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = {
                    onClose()
                }
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    gitManager.clone(File(directory), url)
                }
            ) {
                Text("Clone")
            }
        }
    }
}