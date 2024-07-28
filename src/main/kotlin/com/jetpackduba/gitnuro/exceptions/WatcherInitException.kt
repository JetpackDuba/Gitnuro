package com.jetpackduba.gitnuro.exceptions

class WatcherInitException(
    code: Int,
    message: String = codeToMessage(code),
) : GitnuroException(message)

private fun codeToMessage(code: Int): String {
    return when (code) {
        1 /*is WatcherInitException.Generic*/, 2 /*is WatcherInitException.Io*/ -> "Could not watch directory. Check if it exists and you have read permissions."
        3 /*is WatcherInitException.PathNotFound*/ -> "Path not found, check if your repository still exists"
        5 /*is WatcherInitException.InvalidConfig*/ -> "Invalid configuration"
        6 /*is WatcherInitException.MaxFilesWatch*/ -> "Reached the limit of files that can be watched. Please increase the system inotify limit to be able to detect the changes on this repository."
        else/*is WatcherInitException.WatchNotFound*/ -> "Watch not found! This should not happen, please report this issue to Gitnuro's issue tracker." // This should never trigger as we don't unwatch files
    }
}