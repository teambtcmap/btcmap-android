package org.btcmap.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.btcmap.map.MapArea

/**
 * The communities and countries containing the map centre, shown as chips above
 * the map, ported from `org.btcmap.map.AreasAdapter`. The chip image comes from
 * the API; its initials stay behind it until (and unless) the image loads.
 */
@Composable
fun AreaChips(
    areas: List<MapArea>,
    apiUrl: String,
    palette: AreaChipPalette,
    onAreaClick: (MapArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        areas.forEach { area ->
            AreaChip(
                area = area,
                apiUrl = apiUrl,
                palette = palette,
                onClick = onAreaClick,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun AreaChip(
    area: MapArea,
    apiUrl: String,
    palette: AreaChipPalette,
    onClick: (MapArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(AREA_CHIP_SIZE).clickable { onClick(area) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(AREA_CHIP_SIZE)
                .clip(CircleShape)
                .background(palette.buttonBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = areaInitials(area),
                color = palette.buttonIcon,
                fontSize = 20.sp,
            )
            AsyncImage(
                model = "$apiUrl/v4/areas/${area.id}/image?type=square&w=256&h=256",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AREA_CHIP_SIZE).clip(CircleShape),
            )
        }

        if (area.upcomingEventsCount > 0) {
            Text(
                text = area.upcomingEventsCount.toString(),
                color = palette.badgeText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(palette.badgeBackground)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

/** The chip's side, which the map's round action buttons below it also use. */
internal val AREA_CHIP_SIZE = 56.dp

/**
 * The letters shown on a chip until its image loads, or in its place when the
 * image cannot load. A country shows its two-letter alias; a community shows the
 * initials of up to two words of its name, or the first two letters of a
 * single-word name.
 */
internal fun areaInitials(area: MapArea): String {
    if (area.type == "country") return area.urlAlias.take(2).uppercase()

    val words = area.name.trim()
        .split(Regex("\\s+"))
        .filter { it.firstOrNull()?.isLetterOrDigit() == true }
    return when (words.size) {
        0 -> ""
        1 -> words[0].take(2).uppercase()
        else -> words.take(2).map { it.first() }.joinToString("").uppercase()
    }
}
