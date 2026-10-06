package org.btcmap.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The widest a single-column screen body grows before it stops and centres.
 * Keeping a body around this width stops lines and rows from running the whole
 * width of a desktop or tablet window while leaving a phone unaffected.
 */
val CONTENT_MAX_WIDTH = 600.dp

/** The sign-in / sign-up form's width, narrower than a list for readability. */
val AUTH_FORM_MAX_WIDTH = 420.dp

/** The window width at or above which a body uses the expanded horizontal margin. */
private val EXPANDED_WIDTH = 600.dp

/**
 * A single-column screen body: capped at [maxWidth] and centred, so a large
 * window centres the content instead of stretching it edge to edge.
 *
 * [centerVertically] centres a short body in the available height; it has no
 * effect once the body fills the height. [scroll] lets an over-tall body scroll,
 * so a form stays reachable on a short window. [margin] adds the screen's
 * horizontal margin, which grows from 16dp on a phone to 24dp once the window is
 * as wide as [EXPANDED_WIDTH].
 */
@Composable
fun ContentColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = CONTENT_MAX_WIDTH,
    centerVertically: Boolean = false,
    scroll: Boolean = false,
    margin: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = if (centerVertically) Alignment.Center else Alignment.TopCenter,
    ) {
        val horizontal = if (margin) {
            if (this.maxWidth >= EXPANDED_WIDTH) 24.dp else 16.dp
        } else {
            0.dp
        }
        Column(
            verticalArrangement = verticalArrangement,
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = horizontal),
            content = content,
        )
    }
}
