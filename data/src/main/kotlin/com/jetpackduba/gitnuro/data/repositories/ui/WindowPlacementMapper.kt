package com.jetpackduba.gitnuro.data.repositories.ui

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.ui.WindowPlacement
import javax.inject.Inject

private const val APP_PLACEMENT_FLOATING = 1
private const val APP_PLACEMENT_MAXIMIZED = 2
private const val APP_PLACEMENT_FULLSCREEN = 3

class WindowPlacementMapper @Inject constructor() : DataMapper<WindowPlacement, Int> {
    override fun toData(value: WindowPlacement): Int {
        return when (value) {
            WindowPlacement.FLOATING -> APP_PLACEMENT_FLOATING
            WindowPlacement.MAXIMIZED -> APP_PLACEMENT_MAXIMIZED
            WindowPlacement.FULLSCREEN -> APP_PLACEMENT_FULLSCREEN
        }
    }

    override fun toDomain(value: Int): WindowPlacement {
        return when (value) {
            APP_PLACEMENT_FLOATING -> WindowPlacement.FLOATING
            APP_PLACEMENT_MAXIMIZED -> WindowPlacement.MAXIMIZED
            APP_PLACEMENT_FULLSCREEN -> WindowPlacement.FULLSCREEN
            else -> throw IllegalStateException("Unhandled AppWindowPlacement $value")
        }
    }

}