package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage

/** Test tag for the viewer's delete button. */
const val PLACE_PHOTO_DELETE_TAG = "place-photo-delete"

/**
 * A place photo shown full screen, paged between the place's photos, ported from
 * `PlacePhotoViewerDialogFragment`. The photo is requested at [PLACE_PHOTO_FULL_SIZE]
 * rather than the thumbnail's size, so swiping to a photo shows it in detail,
 * and the uploader's username is captioned over it when the API knows one.
 * A photo the signed-in user may delete carries a delete button. Tapping outside
 * or the close button dismisses it.
 */
@Composable
fun PlacePhotoViewer(
    photos: List<PlacePhoto>,
    initialIndex: Int,
    uploadedBy: (String) -> String,
    deleteDescription: String,
    onDelete: ((PlacePhoto) -> Unit)? = null,
    onDismiss: () -> Unit,
    /** The accessibility label for the paged image; null when it is decorative. */
    imageContentDescription: String? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = photos[page].fullUrl,
                    contentDescription = imageContentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            val current = photos.getOrNull(pagerState.currentPage)
            val author = current?.authorName
            if (author != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    MaterialSymbol(
                        glyph = "person",
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uploadedBy(author),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                if (current != null && current.canDelete && onDelete != null) {
                    IconButton(
                        onClick = { onDelete(current) },
                        modifier = Modifier.testTag(PLACE_PHOTO_DELETE_TAG),
                    ) {
                        MaterialSymbol(
                            glyph = "delete",
                            contentDescription = deleteDescription,
                            tint = Color.White,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    MaterialSymbol(
                        glyph = "close",
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
