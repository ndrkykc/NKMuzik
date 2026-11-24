package com.nkapps.nkmuzik.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun FastScrollbar(
    listState: LazyListState,
    items: List<Any>,
    itemToChar: (Any) -> Char,
    modifier: Modifier = Modifier
) {
    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ#".toList()
    val scope = rememberCoroutineScope()
    var barHeight by remember { mutableStateOf(0f) }

    fun normalizeChar(c: Char): Char {
        return when (c.uppercaseChar()) {
            'Ç' -> 'C'
            'Ğ' -> 'G'
            'İ', 'I', 'ı' -> 'I'
            'Ö' -> 'O'
            'Ş' -> 'S'
            'Ü' -> 'U'
            in 'A'..'Z' -> c.uppercaseChar()
            else -> '#'
        }
    }

    // Map each character to the first index in the list
    val charToIndex = remember(items) {
        val map = mutableMapOf<Char, Int>()
        items.forEachIndexed { index, item ->
            val char = itemToChar(item)
            val key = normalizeChar(char)
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }

    fun scrollToChar(y: Float) {
        if (barHeight == 0f || items.isEmpty()) return
        val fraction = (y / barHeight).coerceIn(0f, 1f)
        val charIndex = (fraction * (alphabet.size - 1)).toInt()
        val targetChar = alphabet[charIndex]

        charToIndex[targetChar]?.let { targetItemIndex ->
            scope.launch {
                listState.scrollToItem(targetItemIndex)
            }
        } ?: run {
            // If exact char is not found, find the closest previous character
            for (i in charIndex downTo 0) {
                val fallbackChar = alphabet[i]
                if (charToIndex.containsKey(fallbackChar)) {
                    scope.launch {
                        listState.scrollToItem(charToIndex[fallbackChar]!!)
                    }
                    break
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(32.dp)
            .padding(vertical = 16.dp)
            .onGloballyPositioned { coordinates ->
                barHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    scrollToChar(offset.y)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    scrollToChar(change.position.y)
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            alphabet.forEach { char ->
                Text(
                    text = char.toString(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
