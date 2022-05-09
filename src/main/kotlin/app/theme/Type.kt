package app.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val fontFamily = FontFamily(
    Font("fonts/OpenSans-Regular.ttf", FontWeight.Normal, FontStyle.Normal),
    Font("fonts/OpenSans-Italic.ttf", FontWeight.Normal, FontStyle.Italic),
    Font("fonts/OpenSans-Medium.ttf", FontWeight.Medium, FontStyle.Normal),
    Font("fonts/OpenSans-MediumItalic.ttf", FontWeight.Medium, FontStyle.Italic),
    Font("fonts/OpenSans-SemiBold.ttf", FontWeight.SemiBold, FontStyle.Normal),
    Font("fonts/OpenSans-SemiBoldItalic.ttf", FontWeight.SemiBold, FontStyle.Italic),
    Font("fonts/OpenSans-Bold.ttf", FontWeight.Bold, FontStyle.Normal),
    Font("fonts/OpenSans-BoldItalic.ttf", FontWeight.Bold, FontStyle.Italic),
)
val typography = Typography(
    defaultFontFamily = fontFamily
)