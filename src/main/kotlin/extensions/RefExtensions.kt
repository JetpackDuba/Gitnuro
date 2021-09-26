package extensions

import org.eclipse.jgit.lib.Ref

val Ref.simpleName: String
    get() {
        return this.name.split("/").last()
    }