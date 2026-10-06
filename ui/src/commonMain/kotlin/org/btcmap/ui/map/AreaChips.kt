package org.btcmap.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import org.btcmap.map.MapArea
import org.btcmap.ui.MaterialSymbol

/**
 * The countries, communities and cities containing the map centre, shown as
 * chips above the map, ported from `org.btcmap.map.AreasAdapter`. The chip image
 * comes from the API; behind it a country or community shows its initials and a
 * city its [areaIcon] glyph, until (and unless) the image loads.
 */
@Composable
fun AreaChips(
    areas: List<MapArea>,
    apiUrl: String,
    palette: AreaChipPalette,
    onAreaClick: (MapArea) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MAP_CONTROLS_GAP),
    ) {
        areas.forEach { area ->
            AreaChip(
                area = area,
                apiUrl = apiUrl,
                palette = palette,
                onClick = onAreaClick,
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
    BadgedBox(
        badge = {
            if (area.upcomingEventsCount > 0) {
                // The same Material 3 Badge the map's review button uses, in the
                // chip's own badge colours: round for a short count, a pill for
                // a longer one.
                Badge(
                    containerColor = palette.badgeBackground,
                    contentColor = palette.badgeText,
                    modifier = Modifier.offset(MAP_BADGE_OFFSET.x, MAP_BADGE_OFFSET.y),
                ) {
                    Text(area.upcomingEventsCount.toString())
                }
            }
        },
        modifier = modifier
            .size(AREA_CHIP_SIZE)
            .clickable { onClick(area) },
    ) {
        Box(
            modifier = Modifier
                .size(AREA_CHIP_SIZE)
                .clip(CircleShape)
                .background(palette.buttonBackground),
            contentAlignment = Alignment.Center,
        ) {
            if (area.type == CITY_AREA_TYPE) {
                MaterialSymbol(
                    glyph = areaIcon(area.type),
                    contentDescription = null,
                    tint = palette.buttonIcon,
                )
            } else {
                Text(
                    text = areaInitials(area),
                    color = palette.buttonIcon,
                    fontSize = 20.sp,
                )
            }
            AsyncImage(
                model = "$apiUrl/v4/areas/${area.id}/image?type=square&w=256&h=256",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(AREA_CHIP_SIZE).clip(CircleShape),
            )
        }
    }
}

/** The chip's side, which the map's round action buttons below it also use. */
internal val AREA_CHIP_SIZE = 56.dp

/**
 * The vertical gap between the map's stacked controls: between chips, and
 * between the chips and the round action buttons below them.
 */
internal val MAP_CONTROLS_GAP = 16.dp

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
