package com.jetpackduba.gitnuro.theme

import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun textFieldColors(
    cursorColor: Color = MaterialTheme.colors.primaryVariant,
    focusedIndicatorColor: Color = MaterialTheme.colors.primaryVariant,
    focusedLabelColor: Color = MaterialTheme.colors.primaryVariant,
    backgroundColor: Color = MaterialTheme.colors.background,
    textColor: Color = MaterialTheme.colors.onBackground,
    disabledTextColor: Color = MaterialTheme.colors.secondaryTextColor,
) = TextFieldDefaults.textFieldColors(
    cursorColor = cursorColor,
    focusedIndicatorColor = focusedIndicatorColor,
    focusedLabelColor = focusedLabelColor,
    backgroundColor = backgroundColor,
    textColor = textColor,
    disabledTextColor = disabledTextColor,
)

@Composable
fun outlinedTextFieldColors() = TextFieldDefaults.outlinedTextFieldColors(
    cursorColor = MaterialTheme.colors.primaryVariant,
    focusedBorderColor = MaterialTheme.colors.primaryVariant,
    focusedLabelColor = MaterialTheme.colors.primaryVariant,
    backgroundColor = MaterialTheme.colors.background,
    textColor = MaterialTheme.colors.onBackground,
)

@Composable
fun textButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colors.primaryVariant
)