package com.jetpackduba.gitnuro.git.graph

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class GenerateLogWalkUseCaseTest {

    @Test
    fun getReservedLane() {
        val generateLogWalkUseCase = GenerateLogWalkUseCase()
        val reservedLanes = mapOf(
            0 to "*",
            1 to "A",
            2 to "B",
            3 to "C",
            4 to "D",
            5 to "E",
            6 to "F",
        )
        val reservedLane = generateLogWalkUseCase.getReservedLanes(reservedLanes, "A")
        assertEquals(listOf(1), reservedLane)
    }

    @Test
    fun getReservedLane_when_value_not_present() {
        val generateLogWalkUseCase = GenerateLogWalkUseCase()
        val reservedLanes = mapOf(
//            0 to "*",
            1 to "A",
            2 to "B",
            3 to "C",
            4 to "D",
            5 to "E",
            6 to "F",
        )
        val reservedLane = generateLogWalkUseCase.getReservedLanes(reservedLanes, "P")
        assertEquals(listOf(0), reservedLane)
    }

    @Test
    fun firstAvailableLane_without_first_item() {
        val generateLogWalkUseCase = GenerateLogWalkUseCase()
        val reservedLanes = mapOf(
//            0 to "*",
            1 to "A",
            2 to "B",
            3 to "C",
//            4 to "D",
            5 to "E",
            6 to "F",
        )

        val firstAvailableLane = generateLogWalkUseCase.firstAvailableLane(reservedLanes)
        assertEquals(0, firstAvailableLane)
    }

    @Test
    fun firstAvailableLane_without_middle_item() {
        val generateLogWalkUseCase = GenerateLogWalkUseCase()
        val reservedLanes = mapOf(
            0 to "*",
            1 to "A",
            2 to "B",
            3 to "C",
//            4 to "D",
            5 to "E",
            6 to "F",
        )

        val firstAvailableLane = generateLogWalkUseCase.firstAvailableLane(reservedLanes)
        assertEquals(4, firstAvailableLane)
    }

    @Test
    fun firstAvailableLane_with_empty_reserved_lanes() {
        val generateLogWalkUseCase = GenerateLogWalkUseCase()
        val reservedLanes = mapOf<Int, String>()
        val firstAvailableLane = generateLogWalkUseCase.firstAvailableLane(reservedLanes)
        assertEquals(0, firstAvailableLane)
    }

    @Test
    fun firstAvailableLane_without_single_non_zero() {
        val generateLogWalkUseCase = GenerateLogWalkUseCase()
        val reservedLanes = mapOf(
            1 to "A",
        )

        val firstAvailableLane = generateLogWalkUseCase.firstAvailableLane(reservedLanes)
        assertEquals(0, firstAvailableLane)
    }

    @Test
    fun firstAvailableLane_without_2_keys_non_zero() {
        val generateLogWalkUseCase = GenerateLogWalkUseCase()
        val reservedLanes = mapOf(
            1 to "A",
            2 to "B",
        )

        val firstAvailableLane = generateLogWalkUseCase.firstAvailableLane(reservedLanes)
        assertEquals(0, firstAvailableLane)
    }
}