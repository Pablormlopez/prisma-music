package com.prisma.music
import org.junit.Assert.*
import org.junit.Test

class LyricsTest {
    @Test fun parsesMultipleTimestampsAndOffset() {
        val lines=Lyrics.parse("[ar:Prisma]\n[offset:-500]\n[00:01.5][00:02.250]Hola\n[00:00.20]Inicio")
        assertEquals(listOf(0L,1000L,1750L),lines.map {it.time})
        assertEquals(listOf("Inicio","Hola","Hola"),lines.map {it.text})
    }
    @Test fun preservesPlainLyricsAndExcludesMetadata() {
        assertEquals(listOf("Primera","Segunda"),Lyrics.parse("[ti:Título]\nPrimera\n\nSegunda").map {it.text})
    }
    @Test fun detectsWhatsAppWithoutExcludingUnrelatedNames() {
        assertTrue(Library.isWhatsApp("Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Audio"))
        assertTrue(Library.isWhatsApp("WhatsApp Business/Media/WhatsApp Voice Notes"))
        assertFalse(Library.isWhatsApp("Music/Mis canciones"))
        assertFalse(Library.isWhatsApp("Music/WhatsAppFavorites"))
    }
}
