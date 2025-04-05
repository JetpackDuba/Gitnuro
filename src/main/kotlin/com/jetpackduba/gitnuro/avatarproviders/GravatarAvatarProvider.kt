package com.jetpackduba.gitnuro.avatarproviders

class GravatarAvatarProvider : AvatarProvider {
    override fun getAvatarUrl(hashedEmail: String): String {
        return "https://www.gravatar.com/avatar/${hashedEmail}?s=60&d=404"
    }
}