@file:UseSerializers(ColorAsStringSerializer::class)

package com.jetpackduba.gitnuro.theme

import androidx.compose.material.Colors
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


@Serializable
data class ColorsScheme(
    val primary: Color,
    val primaryVariant: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onBackgroundSecondary: Color,
    val error: Color,
    val onError: Color,
    val background: Color,
    val backgroundSelected: Color,
    val surface: Color,
    val secondarySurface: Color,
    val tertiarySurface: Color,
    val addFile: Color,
    val deletedFile: Color,
    val modifiedFile: Color,
    val conflictingFile: Color,
    val dialogOverlay: Color,
    val normalScrollbar: Color,
    val hoverScrollbar: Color,
    val diffLineAdded: Color,
    val diffContentAdded: Color,
    val diffLineRemoved: Color,
    val diffContentRemoved: Color,
    val diffKeyword: Color,
    val diffAnnotation: Color,
    val diffComment: Color,
    val isLight: Boolean,
) {
    fun toComposeColors(): Colors {
        return Colors(
            primary = this.primary,
            primaryVariant = this.primaryVariant,
            secondary = this.secondary,
            onSecondary = this.onSecondary,
            secondaryVariant = this.secondary,
            background = this.background,
            surface = this.surface,
            error = this.error,
            onPrimary = this.onPrimary,
            onBackground = this.onBackground,
            onSurface = this.onBackground,
            onError = this.onError,
            isLight = isLight,
        )
    }
}

object ColorAsStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        encoder.encodeString("")
    }

    override fun deserialize(decoder: Decoder): Color {
        val value = decoder.decodeString()
        val longValue = value.toLong(radix = 16)

        return Color(longValue)
    }
}