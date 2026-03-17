package com.jetpackduba.gitnuro.domain.models

import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.RemoteConfig

data class RemoteInfo(val remoteConfig: RemoteConfig, val branchesList: List<Branch>)