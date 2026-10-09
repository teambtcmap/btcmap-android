package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import org.btcmap.ui.map.rubyPinyin

/** How far a pinyin syllable is nudged down toward its character. */
private val RUBY_GAP = 4.dp

/**
 * [text] as plain text, or — when it is mostly Han script and the platform can
 * supply pinyin (see [rubyPinyin]) — with each character's pinyin drawn small
 * above it.
 *
 * Compose has no ruby API, so every character is its own column with the
 * syllable above it. Centring them reads as ruby because Han glyphs are
 * full-width and of even size; Latin runs, spaces and punctuation get an empty
 * syllable so the baselines still line up.
 *
 * The two lines are set to their font size rather than the Material line height,
 * which otherwise leaves a wide gap above the characters. [leading] — an icon,
 * say — is centred on the character line, not on the two-line block.
 */
@Composable
fun RubyText(
    text: String,
    style: TextStyle,
    color: Color,
    rubyColor: Color,
    rubySize: TextUnit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val baseStyle = style.fontSize
        .takeIf { it.isSpecified }
        ?.let { style.copy(lineHeight = it) }
        ?: style
    val syllables = rubyPinyin(text)
    if (syllables == null) {
        Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            Text(text = text, style = style, color = color)
        }
        return
    }
    val rubyLine = with(LocalDensity.current) { rubySize.toDp() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let { icon ->
            // Centred on the block, so drop it by half the ruby line to sit on
            // the characters instead. A Row (not a Box), so a spacer in [icon]
            // actually follows the glyph rather than stacking over it.
            Row(
                modifier = Modifier.offset(y = rubyLine / 2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icon()
            }
        }
        text.forEachIndexed { index, char ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // A space rather than nothing, so every column keeps the ruby
                // line's height and the base characters share one baseline.
                Text(
                    text = syllables.getOrNull(index) ?: " ",
                    style = TextStyle(fontSize = rubySize, lineHeight = rubySize),
                    color = rubyColor,
                    maxLines = 1,
                    // The character line keeps some leading above its glyph; drop
                    // the syllable into it so the two read as one unit.
                    modifier = Modifier.offset(y = RUBY_GAP),
                )
                Text(
                    text = char.toString(),
                    style = baseStyle,
                    color = color,
                    maxLines = 1,
                )
            }
        }
    }
}
