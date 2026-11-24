package com.nkapps.nkmuzik.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay

@Composable
fun rememberTypewriterText(
    phrases: List<String>,
    isSearching: Boolean
): String {
    if (isSearching || phrases.isEmpty()) return ""

    var displayText by remember(phrases) { mutableStateOf("") }

    LaunchedEffect(phrases, isSearching) {
        if (isSearching || phrases.isEmpty()) {
            displayText = ""
            return@LaunchedEffect
        }

        var phraseIndex = 0
        while (true) {
            val currentPhrase = phrases[phraseIndex % phrases.size]

            // 1. Typing Phase (character by character - 90ms for a calmer pace)
            for (i in 1..currentPhrase.length) {
                displayText = currentPhrase.substring(0, i)
                delay(90)
            }

            // 2. Full Text Blinking Phase (The ENTIRE text blinks on and off 2 times)
            repeat(2) {
                displayText = currentPhrase
                delay(320)
                displayText = ""
                delay(200)
            }
            displayText = currentPhrase
            delay(250)

            // 3. Erasing Phase (character by character - 50ms)
            for (i in currentPhrase.length - 1 downTo 0) {
                displayText = currentPhrase.substring(0, i)
                delay(50)
            }

            displayText = ""
            delay(200)

            phraseIndex++
        }
    }

    return displayText
}

@Composable
fun TypewriterPlaceholder(
    phrases: List<String>,
    isSearching: Boolean,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val text = rememberTypewriterText(phrases = phrases, isSearching = isSearching)
    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = style,
            color = color,
            modifier = modifier
        )
    }
}
