package com.prisma.music

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsHubTest {
    @get:Rule val compose=createComposeRule()
    @Test fun searchFindsAudioCategoryAndOpensIt() {
        var selected=""
        compose.setContent {MaterialTheme {SettingsHub {selected=it}}}
        compose.onNodeWithText("Buscar ajustes").performTextInput("normalización")
        compose.onNodeWithText("Apariencia").assertDoesNotExist()
        compose.onNodeWithText("Reproductor y audio").performClick()
        assertEquals("Reproductor y audio",selected)
    }
}
