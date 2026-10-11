package com.prisma.music

import org.junit.Assert.*
import org.junit.Test

class MenuNavigationTest {
    @Test fun swipesFollowMenuOrderAndRespectBoundaries() {
        assertEquals("Biblioteca", adjacentPage("Inicio", -100f, 72f))
        assertEquals("Inicio", adjacentPage("Biblioteca", 100f, 72f))
        assertEquals("Sonido", adjacentPage("Listas", -100f, 72f))
        assertEquals("Ajustes", adjacentPage("Sonido", -100f, 72f))
        assertNull(adjacentPage("Inicio", 100f, 72f))
        assertNull(adjacentPage("Ajustes", -100f, 72f))
        assertNull(adjacentPage("Biblioteca", -30f, 72f))
        assertNull(adjacentPage("Reproduciendo", -100f, 72f))
    }
}
