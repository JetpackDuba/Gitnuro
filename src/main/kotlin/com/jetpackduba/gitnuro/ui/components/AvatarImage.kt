package com.jetpackduba.gitnuro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import com.jetpackduba.gitnuro.extensions.md5
import com.jetpackduba.gitnuro.images.rememberNetworkImageOrNull
import org.eclipse.jgit.lib.PersonIdent

@Composable
fun AvatarImage(
    modifier: Modifier = Modifier,
    personIdent: PersonIdent,
    color: Color = MaterialTheme.colors.primary,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
    ) {
        val avatar = rememberAvatar(personIdent.emailAddress)
        if (avatar == null) {
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
        } else {
            Image(
                bitmap = avatar,
                modifier = Modifier
                    .fillMaxSize(),
                contentDescription = null,
            )
        }
    }
}


@Composable
fun rememberAvatar(email: String): ImageBitmap? {
    val url = "https://www.gravatar.com/avatar/${email.md5}?s=60&d=404"
    return rememberNetworkImageOrNull(
        url = url,
        placeHolderImageRes = null,
    )
}