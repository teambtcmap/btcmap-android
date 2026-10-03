package org.btcmap.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.AbstractComposeView
import org.btcmap.place.ReportDraft

/**
 * Hosts [ReportPlaceScreen] inside the Android Views hierarchy. The app sets the
 * initial reason, the labels, the (ViewModel-backed) photo list and the submit
 * and photo-picker callbacks; this view holds no state of its own.
 */
class ReportPlaceComposeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var initialType: String? by mutableStateOf(null)

    var labels: ReportPlaceLabels? by mutableStateOf(null)

    var photos: SnapshotStateList<ByteArray>? by mutableStateOf(null)

    var submit: suspend (ReportDraft) -> Unit = {}

    var pickPhotos: suspend () -> List<ByteArray> = { emptyList() }

    var onBack: () -> Unit = {}

    @Composable
    override fun Content() {
        val labels = labels ?: return
        val photos = photos ?: return
        AppTheme {
            ReportPlaceScreen(
                initialType = initialType,
                labels = labels,
                submit = submit,
                onBack = onBack,
                pickPhotos = pickPhotos,
                photos = photos,
            )
        }
    }
}
