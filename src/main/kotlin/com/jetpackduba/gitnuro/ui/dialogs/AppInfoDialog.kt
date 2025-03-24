package com.jetpackduba.gitnuro.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.AppConstants
import com.jetpackduba.gitnuro.AppConstants.openSourceProjects
import com.jetpackduba.gitnuro.Project
import com.jetpackduba.gitnuro.extensions.handOnHover
import com.jetpackduba.gitnuro.theme.textButtonColors
import com.jetpackduba.gitnuro.ui.components.ScrollableLazyColumn
import com.jetpackduba.gitnuro.ui.components.TextLink

@Composable
fun AppInfoDialog(
    onClose: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
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
                            style = MaterialTheme.typography.h2,
                        )

                        Text(
                            AppConstants.APP_DESCRIPTION,
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Text(
                            "Gitnuro has been possible thanks to the following open source projects:",
                            style = MaterialTheme.typography.body1,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }

                items(openSourceProjects) {
                    ProjectUsed(it, onOpenUrlInBrowser = onOpenUrlInBrowser)
                }
            }

            TextButton(
                modifier = Modifier
                    .padding(top = 16.dp, end = 8.dp)
                    .align(Alignment.End)
                    .handOnHover(),
                onClick = onClose,
                colors = textButtonColors(),
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
fun ProjectUsed(
    project: Project,
    onOpenUrlInBrowser: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp)

    ) {
        TextLink(
            text = project.name,
            url = project.url,
            modifier = Modifier
                .padding(vertical = 8.dp),
            onClick = { onOpenUrlInBrowser(project.url) }
        )

        Spacer(Modifier.weight(1f))

        TextLink(
            text = project.license.name,
            url = project.license.url,
            modifier = Modifier
                .padding(vertical = 8.dp),
            colorsInverted = true,
            onClick = { onOpenUrlInBrowser(project.license.url) }
        )
    }
}