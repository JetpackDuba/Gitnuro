package app.extensions

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ArrayExtensionsTest {
    @Test
    fun matchingIndexes() {
        val array = arrayOf<Int?>(0, 1, 2, 3, null, 5, 6, null)
        val result = array.matchingIndexes { it == null }
        assertEquals(result.count(), 2)
        assertEquals(result[0], 4)
        assertEquals(result[1], 7)
    }

    @Test
    fun matchingIndexes_empty_array() {
        val array = arrayOf<Int>()
        val result = array.matchingIndexes { it > 0 }
        assertEquals(result.count(), 0)
    }
}