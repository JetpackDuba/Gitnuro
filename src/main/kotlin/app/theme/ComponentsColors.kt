package app.theme

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun textFieldColors() = TextFieldDefaults.textFieldColors(
    cursorColor = MaterialTheme.colors.primaryVariant,
    focusedIndicatorColor = MaterialTheme.colors.primaryVariant,
    focusedLabelColor = MaterialTheme.colors.primaryVariant,
    backgroundColor = MaterialTheme.colors.background,
    textColor = MaterialTheme.colors.primaryTextColor,
)

@Composable
fun outlinedTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    cursorColor = MaterialTheme.colors.primaryVariant,
    focusedBorderColor = MaterialTheme.colors.primaryVariant,
    focusedLabelColor = MaterialTheme.colors.primaryVariant,
    backgroundColor = MaterialTheme.colors.background,
    textColor = MaterialTheme.colors.primaryTextColor,
)

@Composable
fun textButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colors.primaryVariant
)