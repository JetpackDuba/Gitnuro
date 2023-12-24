package com.jetpackduba.gitnuro.git.config

object LocalConfigConstants {
    const val CONFIG_FILE_NAME = "gitnuro"

    object SignOff {
        const val SECTION = "signoff"
        const val FIELD_ENABLED = "enabled"
        const val FIELD_FORMAT = "format"

        const val DEFAULT_SIGN_OFF_FORMAT_USER = "%user"
        const val DEFAULT_SIGN_OFF_FORMAT_EMAIL = "%email"
        const val DEFAULT_SIGN_OFF_FORMAT =
            "Signed-off-by: $DEFAULT_SIGN_OFF_FORMAT_USER <$DEFAULT_SIGN_OFF_FORMAT_EMAIL>"
        const val DEFAULT_SIGN_OFF_ENABLED = false
    }
}