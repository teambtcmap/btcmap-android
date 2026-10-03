package org.btcmap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import org.btcmap.util.rethrowIfCancellation

/** Test tags so a test can drive the uploaded-images screen. */
const val UPLOADED_IMAGES_DELETE_TAG_PREFIX = "uploaded-images-delete-"

/** One of the signed-in user's uploaded photos, with the place it belongs to. */
data class UploadedImageUi(
    val placeId: Long,
    val imageId: Long,
    val thumbnailUrl: String,
    val placeName: String,
)

/** The uploaded-images screen's strings, so the screen stays resource-free. */
data class UploadedImagesLabels(
    val empty: String,
    val delete: String,
    val failed: String,
    val retry: String,
    val unknownPlace: (Long) -> String,
)

/**
 * The signed-in user's uploaded place photos, newest first, each with the place
 * it belongs to and a delete action. The list is loaded through [load] and
 * deleted through [delete], so the host owns the API calls and the screen stays
 * testable; a successful delete drops the row without a refetch. The screen
 * carries no title of its own: the host shows "Uploaded images" in its own
 * title bar, and its back affordance leaves this screen.
 */
@Composable
fun UploadedImagesScreen(
    labels: UploadedImagesLabels,
    load: suspend () -> List<UploadedImageUi>,
    delete: suspend (UploadedImageUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val images = remember { mutableStateListOf<UploadedImageUi>() }
    val deleting = remember { mutableStateListOf<Long>() }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loadError = null
        try {
            val fetched = load()
            images.clear()
            images.addAll(fetched)
        } catch (t: Throwable) {
            t.rethrowIfCancellation()
            loadError = t.message ?: t.toString()
        } finally {
            loaded = true
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = modifier.fillMaxSize()) {
        val error = loadError
        when {
            !loaded -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            error != null -> Column(modifier = Modifier.padding(16.dp)) {
                Text(text = error, color = MaterialTheme.colorScheme.error)
                TextButton(
                    onClick = { scope.launch { reload() } },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(labels.retry)
                }
            }

            images.isEmpty() -> Text(
                text = labels.empty,
                modifier = Modifier.padding(16.dp),
            )

            else -> Column(modifier = Modifier.fillMaxSize()) {
                actionError?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 144.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(images, key = { it.imageId }) { image ->
                        UploadedImageCell(
                            image = image,
                            deleteDescription = labels.delete,
                            deleting = image.imageId in deleting,
                            onDelete = {
                                actionError = null
                                scope.launch {
                                    deleting.add(image.imageId)
                                    try {
                                        delete(image)
                                        images.remove(image)
                                    } catch (t: Throwable) {
                                        t.rethrowIfCancellation()
                                        actionError = labels.failed
                                    } finally {
                                        deleting.remove(image.imageId)
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadedImageCell(
    image: UploadedImageUi,
    deleteDescription: String,
    deleting: Boolean,
    onDelete: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = image.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                text = image.placeName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onDelete,
                enabled = !deleting,
                modifier = Modifier.testTag(UPLOADED_IMAGES_DELETE_TAG_PREFIX + image.imageId),
            ) {
                if (deleting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    MaterialSymbol(glyph = "delete", contentDescription = deleteDescription)
                }
            }
        }
    }
}
