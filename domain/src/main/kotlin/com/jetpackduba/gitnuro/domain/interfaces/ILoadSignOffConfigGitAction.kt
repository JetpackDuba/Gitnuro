package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.SignOffConfig
import org.eclipse.jgit.lib.Repository

interface ILoadSignOffConfigGitAction {
    operator fun invoke(repository: Repository): SignOffConfig
}