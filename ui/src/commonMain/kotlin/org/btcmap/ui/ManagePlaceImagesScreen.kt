package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.btcmap.util.rethrowIfCancellation

/** The test tag prefix of a row's delete action on the manage-place-images screen. */
const val MANAGE_PLACE_IMAGES_DELETE_TAG_PREFIX = "manage-place-images-delete-"

/** One recent place image, with the place it belongs to and who uploaded it. */
data class ManagePlaceImageUi(
    val placeId: Long,
    val imageId: Long,
    val thumbnailUrl: String,
    /** The full-screen URL the viewer requests when the row is tapped. */
    val fullUrl: String,
    val placeName: String,
    /** The uploader's display name, or null when the API did not resolve one. */
    val uploaderName: String?,
    /** The RFC 3339 timestamp the image was stored at, formatted by the host. */
    val createdAt: String,
)

/** The manage-place-images screen's strings, so the screen stays resource-free. */
data class ManagePlaceImagesLabels(
    val empty: String,
    val failed: String,
    val retry: String,
    /** Names a place the API image points at but the cache does not hold. */
    val unknownPlace: (Long) -> String,
    /** Shown for an image whose uploader the API did not resolve. */
    val unknownUploader: String,
    /** The uploader line's format, e.g. "by %1$s". */
    val by: (String) -> String,
    /** Formats an image's stored-at timestamp for display. */
    val date: (String) -> String,
    /** The accessibility label of the full-screen image. */
    val fullscreen: String,
    /** The per-row delete action and its confirmation. */
    val delete: DeleteActionLabels,
)

/**
 * The "manage place images" screen: the latest place uploads across every place,
 * newest first, each with a thumbnail, the place it belongs to, its uploader and
 * a delete action, so a moderator can remove spam. Only admins and roots reach
 * it: the settings row is hidden for everyone else, and the server enforces the
 * role on the list and delete calls.
 *
 * The list is loaded through [load] and deleted through [delete], so the host
 * owns the API calls and the screen stays testable; a successful delete drops
 * the row without a refetch. Tapping a row opens the image full screen (swiping
 * through the list); the viewer is read-only, so a delete always goes through
 * the row's confirmation. The screen carries no title of its own: the host
 * shows "Manage place images" in its own bar, and its back affordance leaves
 * this screen.
 */
@Composable
fun ManagePlaceImagesScreen(
    labels: ManagePlaceImagesLabels,
    load: suspend () -> List<ManagePlaceImageUi>,
    delete: suspend (ManagePlaceImageUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var loaded by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    val images = remember { mutableStateListOf<ManagePlaceImageUi>() }

    LaunchedEffect(reloadKey) {
        failed = false
        loaded = false
        try {
            val fetched = load()
            images.clear()
            images.addAll(fetched)
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            failed = true
        } finally {
            loaded = true
        }
    }

    ContentColumn(modifier = modifier) {
        when {
            !loaded -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                CircularProgressIndicator()
            }

            failed -> ManagePlaceImagesStateMessage(
                icon = "error",
                text = labels.failed,
                modifier = Modifier.weight(1f),
                action = {
                    TextButton(onClick = { reloadKey++ }) { Text(labels.retry) }
                },
            )

            images.isEmpty() -> ManagePlaceImagesStateMessage(
                icon = "photo_library",
                text = labels.empty,
                modifier = Modifier.weight(1f),
            )

            else -> LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                itemsIndexed(images, key = { _, image -> image.imageId }) { index, image ->
                    ManagePlaceImageRow(
                        image = image,
                        labels = labels,
                        onOpen = { viewerIndex = index },
                        onDelete = { delete(image) },
                        onDeleted = { images.remove(image) },
                    )
                }
            }
        }
    }

    viewerIndex?.let { index ->
        val image = images.getOrNull(index) ?: return@let
        PlacePhotoViewer(
            photos = images.map { it.toPlacePhoto() },
            initialIndex = index,
            uploadedBy = labels.by,
            deleteDescription = labels.delete.delete,
            // The list owns deletion (with its confirmation), so the viewer only
            // shows the photo.
            onDelete = null,
            onDismiss = { viewerIndex = null },
            imageContentDescription = labels.fullscreen,
        )
    }
}

/** The viewer's rendition of an image row: the two URLs it draws and its uploader. */
private fun ManagePlaceImageUi.toPlacePhoto(): PlacePhoto = PlacePhoto(
    imageId = imageId,
    placeId = placeId,
    thumbnailUrl = thumbnailUrl,
    fullUrl = fullUrl,
    authorName = uploaderName,
)

/**
 * One image: its thumbnail, the place, the uploader and date, and the delete
 * action. A plain row rather than a [androidx.compose.material3.ListItem]
 * because a ListItem top-aligns its leading content on a multi-line row, which
 * left the thumbnail visually high and the row's padding bottom-heavy; centring
 * the three children keeps the vertical padding even.
 */
@Composable
private fun ManagePlaceImageRow(
    image: ManagePlaceImageUi,
    labels: ManagePlaceImagesLabels,
    onOpen: () -> Unit,
    onDelete: suspend () -> Unit,
    onDeleted: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        AsyncImage(
            model = image.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(MANAGE_PLACE_IMAGE_THUMBNAIL_SIZE)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = image.placeName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = image.uploaderName?.let(labels.by) ?: labels.unknownUploader,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = labels.date(image.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DeleteAction(
            labels = labels.delete,
            onDelete = onDelete,
            onDeleted = onDeleted,
            deleteTag = MANAGE_PLACE_IMAGES_DELETE_TAG_PREFIX + image.imageId,
        )
    }
}

private val MANAGE_PLACE_IMAGE_THUMBNAIL_SIZE = 56.dp

/**
 * One centred state: a symbol over a message and, for a failure, a retry action.
 * Used for loading-adjacent states so the screen never shows bare top-left text.
 */
@Composable
private fun ManagePlaceImagesStateMessage(
    icon: String,
    text: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth().padding(24.dp),
    ) {
        MaterialSymbol(
            glyph = icon,
            contentDescription = null,
            size = 48.sp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        action?.let {
            Spacer(modifier = Modifier.height(8.dp))
            it()
        }
    }
}
