package app.git.remotes

import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RemoteConfig

data class RemoteInfo(val remoteConfig: RemoteConfig, val branchesList: List<Ref>)