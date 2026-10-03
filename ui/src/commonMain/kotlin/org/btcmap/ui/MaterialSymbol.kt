package org.btcmap.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * A Material Symbols icon, drawn from the typeface [AppTheme] provides.
 *
 * [contentDescription] is null for decorative icons; the ligature name is never
 * announced.
 */
@Composable
fun MaterialSymbol(
    glyph: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: TextUnit = 24.sp,
) {
    val semantics = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier.clearAndSetSemantics {}
    }

    Text(
        text = glyph,
        fontFamily = LocalIconFont.current,
        fontSize = size,
        color = tint,
        modifier = modifier.then(semantics),
    )
}
