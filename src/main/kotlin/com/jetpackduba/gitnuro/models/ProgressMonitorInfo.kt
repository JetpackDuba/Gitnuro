package com.jetpackduba.gitnuro.models

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
