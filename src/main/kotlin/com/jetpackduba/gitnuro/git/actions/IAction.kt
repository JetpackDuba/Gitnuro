package com.jetpackduba.gitnuro.git.actions

import com.jetpackduba.gitnuro.git.Action

interface IAction<T: Action> {
    operator fun invoke(action: T)
}