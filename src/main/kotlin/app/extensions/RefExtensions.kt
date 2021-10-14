package app.extensions

import org.eclipse.jgit.lib.Ref

val Ref.simpleName: String
    get() {
        return if (this.name.startsWith("refs/remotes/"))
            name.replace("refs/remotes/", "")
        else
            this.name.split("/").last()
    }