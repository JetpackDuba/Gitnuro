package com.jetpackduba.gitnuro.data.extensions

import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.ObjectIdRef
import org.eclipse.jgit.lib.Ref

val Ref.isBranch: Boolean
    get() {
        return this is ObjectIdRef.PeeledNonTag
    }

val Ref.isTag: Boolean
    get() = this.name.startsWith(Constants.R_TAGS)

val Ref.isLocal: Boolean
    get() = !this.isRemote

val Ref.isRemote: Boolean
    get() = this.name.startsWith(Constants.R_REMOTES)
