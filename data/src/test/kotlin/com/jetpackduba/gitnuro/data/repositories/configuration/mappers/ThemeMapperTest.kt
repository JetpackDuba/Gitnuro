package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.domain.models.ui.Theme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ThemeMapperTest {
    private val mapper = ThemeMapper()

    @ParameterizedTest
    @EnumSource(Theme::class)
    fun `every theme round-trips through its persisted value`(theme: Theme) {
        assertEquals(theme, mapper.toDomain(mapper.toData(theme)))
    }

    @Test
    fun `system theme is persisted as system`() {
        assertEquals("system", mapper.toData(Theme.System))
        assertEquals(Theme.System, mapper.toDomain("system"))
    }

    @Test
    fun `null maps to null both ways`() {
        assertNull(mapper.toData(null))
        assertNull(mapper.toDomain(null))
    }

    @Test
    fun `unknown persisted value throws`() {
        assertThrows<IllegalStateException> { mapper.toDomain("sepia") }
    }
}
