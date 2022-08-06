package app.ui.context_menu

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun DropDownContent(
    dropDownContentData: DropDownContentData,
    enabled: Boolean = true,
    onDismiss: () -> Unit,
) {
    DropdownMenuItem(
        enabled = enabled,
        onClick = {
            dropDownContentData.onClick()
            onDismiss()
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dropDownContentData.icon != null) {
                Icon(
                    painter = painterResource(dropDownContentData.icon),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }

            Text(
                text = dropDownContentData.label,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(end = 8.dp),
                maxLines = 1,
            )
        }
    }
}