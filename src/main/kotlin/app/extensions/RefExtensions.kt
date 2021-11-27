package app.extensions

import org.eclipse.jgit.lib.ObjectIdRef
import org.eclipse.jgit.lib.Ref

val Ref.simpleName: String
    get() {
        return if (this.isRemote)
            name.replace("refs/remotes/", "")
        else
            this.name.split("/").last() // TODO Do not take the last one, just remove the prefixes
    }

val Ref.isBranch: Boolean
    get() {
        return this is ObjectIdRef.PeeledNonTag
    }

val Ref.isTag: Boolean
    get() = this is ObjectIdRef.PeeledTag

val Ref.isLocal: Boolean
    get() = !this.isRemote

val Ref.isRemote: Boolean
    get() = this.name.startsWith("refs/remotes/")