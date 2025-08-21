package com.jetpackduba.gitnuro.ui.dialogs.base

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jetpackduba.gitnuro.generated.resources.Res
import com.jetpackduba.gitnuro.generated.resources.generic_button_cancel
import com.jetpackduba.gitnuro.keybindings.KeybindingOption
import com.jetpackduba.gitnuro.keybindings.matchesBinding
import com.jetpackduba.gitnuro.ui.components.AdjustableOutlinedTextField
import org.jetbrains.compose.resources.stringResource

@Composable
fun SingleTextFieldDialog(
    icon: Painter,
    title: String,
    subtitle: String,
    value: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onValueChange: (String) -> Unit,
    primaryActionText: String,
    cancelActionText: String = stringResource(Res.string.generic_button_cancel),
    isPrimaryActionEnabled: Boolean,
    onDismiss: () -> Unit,
    onPrimaryActionClicked: () -> Unit,
    fieldTrailingIcon: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (ColumnScope.() -> Unit)? = null,
) {
    val fieldFocusRequester = remember { FocusRequester() }
    val actionsFocusRequester = remember { FocusRequester() }

    IconBasedDialog(
        icon = icon,
        title = title,
        subtitle = subtitle,
        primaryActionText = primaryActionText,
        cancelActionText = cancelActionText,
        isPrimaryActionEnabled = isPrimaryActionEnabled,
        contentFocusRequester = fieldFocusRequester,
        actionsFocusRequester = actionsFocusRequester,
        onDismiss = onDismiss,
        onPrimaryActionClicked = onPrimaryActionClicked,
    ) {
        AdjustableOutlinedTextField(
            modifier = Modifier
                .focusRequester(fieldFocusRequester)
                .focusProperties {
                    this.next = actionsFocusRequester
                }
                .width(300.dp)
                .onPreviewKeyEvent { keyEvent ->
                    if (keyEvent.matchesBinding(KeybindingOption.SIMPLE_ACCEPT) && isPrimaryActionEnabled) {
                        onPrimaryActionClicked()
                        true
                    } else {
                        false
                    }
                },
            value = value,
            maxLines = 1,
            singleLine = true,
            visualTransformation = visualTransformation,
            trailingIcon = fieldTrailingIcon,
            onValueChange = {
                onValueChange(it)
            },
        )

        trailingContent?.invoke(this)

        LaunchedEffect(Unit) {
            fieldFocusRequester.requestFocus()
        }
    }
}