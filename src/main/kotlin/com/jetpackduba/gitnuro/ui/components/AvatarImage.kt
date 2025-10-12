package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.jetpackduba.gitnuro.LocalAvatarProvider
import com.jetpackduba.gitnuro.extensions.sha256
import org.eclipse.jgit.lib.PersonIdent

@Composable
fun AvatarImage(
    modifier: Modifier = Modifier,
    personIdent: PersonIdent,
    color: Color = MaterialTheme.colors.primary,
) {
    val current = LocalAvatarProvider.current

    Box(
        modifier = modifier
            .clip(CircleShape)
    ) {
        val avatarProviderUrl = current.getAvatarUrl(personIdent.emailAddress.sha256)
        var isSuccessfulLoad by remember { mutableStateOf(false) }

        AsyncImage(
            model = avatarProviderUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            onState = {
                isSuccessfulLoad = it is AsyncImagePainter.State.Success
            }
        )

        if (avatarProviderUrl == null || !isSuccessfulLoad) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = personIdent.name.firstOrNull()?.uppercase() ?: "#",
                    color = Color.White,
                )
            }
        }
    }
}