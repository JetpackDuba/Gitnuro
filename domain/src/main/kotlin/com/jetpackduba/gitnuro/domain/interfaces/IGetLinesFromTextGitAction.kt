package com.jetpackduba.gitnuro.domain.interfaces

interface IGetLinesFromTextGitAction {
    operator fun invoke(content: String): List<String>
}