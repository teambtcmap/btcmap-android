package org.btcmap.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow

/**
 * A screen with the app's standard top bar: a back affordance, a title and
 * optional [actions], with [content] filling the space below.
 *
 * Shared by every host that shows a screen which does not draw its own top bar,
 * so the Android fragments and the desktop window present the same chrome. A
 * screen that owns its top bar instead (the add-place, add-event and
 * event-review forms) builds its own `Scaffold`; this is for the plain pages
 * wrapped around a shared body.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenPage(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String = "Back",
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        MaterialSymbol(glyph = "arrow_back", contentDescription = backContentDescription)
                    }
                },
                actions = actions,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}
