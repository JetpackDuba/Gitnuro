package com.jetpackduba.gitnuro.extensions

import org.eclipse.jgit.submodule.SubmoduleStatusType

fun SubmoduleStatusType.isValid(): Boolean {
    return this == SubmoduleStatusType.INITIALIZED ||
            this == SubmoduleStatusType.REV_CHECKED_OUT
}