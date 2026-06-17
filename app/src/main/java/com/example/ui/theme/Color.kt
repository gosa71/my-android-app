package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Clean, modern messenger palette (no "hearth" theming).
// Inspired by neutral, calm chat apps. Blue accent + grays.
// ============================================================

val BrandBlueLight = Color(0xFF2F81F7)
val BrandBlueDark = Color(0xFF3390EC)

// Light theme
val LightBackground = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEFF1F3)
val LightOnSurface = Color(0xFF0B141A)
val LightOnSurfaceVariant = Color(0xFF54656F)
val LightOutline = Color(0xFFE3E6EA)

// Dark theme
val DarkBackground = Color(0xFF0E1621)
val DarkSurface = Color(0xFF17212B)
val DarkSurfaceVariant = Color(0xFF1B2735)
val DarkOnSurface = Color(0xFFE6ECF2)
val DarkOnSurfaceVariant = Color(0xFF8B98A5)
val DarkOutline = Color(0xFF22303C)

// Chat bubbles
val IncomingBubbleLight = Color(0xFFF0F2F5)
val IncomingTextLight = Color(0xFF0B141A)
val OutgoingBubbleLight = Color(0xFF2F81F7)
val OutgoingTextLight = Color(0xFFFFFFFF)

val IncomingBubbleDark = Color(0xFF182533)
val IncomingTextDark = Color(0xFFE6ECF2)
val OutgoingBubbleDark = Color(0xFF2B5278)
val OutgoingTextDark = Color(0xFFFFFFFF)

// Deterministic avatar accent colors (per family member)
val AvatarColors = listOf(
    Color(0xFF2F81F7), // blue
    Color(0xFF9B59B6), // purple
    Color(0xFFE67E22), // orange
    Color(0xFFE91E63), // pink
    Color(0xFF16A085), // teal
    Color(0xFF607D8B), // slate
)
