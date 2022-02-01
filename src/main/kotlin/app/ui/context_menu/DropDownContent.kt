package app.ui.context_menu

import androidx.compose.foundation.layout.Row
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

@Composable
fun DropDownContent(dropDownContentData: DropDownContentData, onDismiss: () -> Unit) {
    DropdownMenuItem(
        onClick = {
            dropDownContentData.onClick()
            onDismiss()
        }
    ) {
        Row {
            if (dropDownContentData.icon != null) {
                Icon(imageVector = dropDownContentData.icon, contentDescription = null)
            }

            Text(dropDownContentData.label, fontSize = 14.sp)
        }
    }
}