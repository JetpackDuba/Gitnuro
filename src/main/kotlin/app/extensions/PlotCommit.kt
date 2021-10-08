@file:Suppress("UNCHECKED_CAST")

package app.extensions

import org.eclipse.jgit.revplot.PlotCommit
import org.eclipse.jgit.revplot.PlotLane
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

val PlotCommit<PlotLane>.reflectForkingOffLanes: Array<PlotLane>
    get() {
        val f = PlotCommit::class.memberProperties.find { it.name == "forkingOffLanes" }
        f?.let {
            it.isAccessible = true
            return it.get(this) as Array<PlotLane>
        }

        return emptyArray()
    }

val PlotCommit<PlotLane>.reflectMergingLanes: Array<PlotLane>
    get() {
        val f = PlotCommit::class.memberProperties.find { it.name == "mergingLanes" }
        f?.let {
            it.isAccessible = true
            return it.get(this) as Array<PlotLane>
        }

        return emptyArray()
    }