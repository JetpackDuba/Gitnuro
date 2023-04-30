package com.jetpackduba.gitnuro.models

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.ProgressMonitor

sealed interface ProgressMonitorInfo {
    object Loading : ProgressMonitorInfo

    data class Processing(
        val totalTasks: Int,
        val currentTaskCount: Int,
        val currentTask: CurrentTask,
    ) : ProgressMonitorInfo

    data class Failed(val ex: Exception) : ProgressMonitorInfo

    object Completed : ProgressMonitorInfo
}

data class CurrentTask(
    val title: String,
    val totalWork: Int,
    val completedWork: Int,
)
