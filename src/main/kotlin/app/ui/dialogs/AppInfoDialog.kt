package app.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.AppConstants
import app.AppConstants.openSourceProjects
import app.Project
import app.theme.primaryTextColor
import app.theme.textButtonColors
import app.ui.components.ScrollableLazyColumn
import app.ui.components.TextLink

@Composable
fun AppInfoDialog(
    onClose: () -> Unit,
) {
    MaterialDialog(onCloseRequested = onClose) {
        Column(
            modifier = Modifier
                .width(600.dp)
                .height(600.dp)
        ) {
            ScrollableLazyColumn(
                modifier = Modifier
                    .weight(1f)
            ) {
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            AppConstants.APP_NAME,
                            fontSize = 24.sp,
                            color = MaterialTheme.colors.primaryTextColor,
                        )

                        Text(
                            AppConstants.APP_DESCRIPTION,
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.primaryTextColor,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Text(
                            "Gitnuro has been possible thanks to the following open source projects:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.primaryTextColor,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }

                items(openSourceProjects) {
                    ProjectUsed(it)
                }
            }

            TextButton(
                modifier = Modifier
                    .padding(top = 16.dp, end = 8.dp)
                    .align(Alignment.End),
                onClick = onClose,
                colors = textButtonColors(),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun ProjectUsed(project: Project) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)

    ) {
        TextLink(
            text = project.name,
            url = project.url,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )

        Spacer(Modifier.weight(1f))

        TextLink(
            text = project.license.name,
            url = project.license.url,
            modifier = Modifier
                .padding(vertical = 8.dp),
            colorsInverted = true
        )
    }
}