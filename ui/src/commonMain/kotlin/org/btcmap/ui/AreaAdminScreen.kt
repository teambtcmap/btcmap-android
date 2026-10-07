package org.btcmap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.btcmap.db.table.area.Area
import org.btcmap.util.toUrlOrNull

/** The placeholder shown for a field the cache has no value for. */
private const val NOT_SET = "—"

/** How much of `geo_json` the read-only view shows before it elides the rest. */
private const val GEO_JSON_PREVIEW = 400

/** The height of the preview map above the field list. */
private val AREA_ADMIN_MAP_HEIGHT = 180.dp

/** The field's role, so the host can offer the right trailing action. */
enum class AreaAdminFieldKind { Plain, Name, Description, VerifiedAt }

/** One field of the cached area. */
data class AreaAdminField(
    val label: String,
    val value: String,
    val kind: AreaAdminFieldKind = AreaAdminFieldKind.Plain,
)

/** The trailing action a field offers: its icon, label and enabled state. */
data class AreaFieldAction(
    val icon: String,
    val description: String,
    val enabled: Boolean = true,
)

/**
 * The area admin screen: every field the local cache holds for one area, so an
 * admin can inspect what the sync actually stored. Read-only; the host shows the
 * area's name in its own bar.
 *
 * [map] is an optional non-interactive preview of the area, shown as the first
 * item of the scrolling field list so it scrolls away with the fields; a host
 * without a map (or a test) leaves it out.
 *
 * The body is a single column capped at [CONTENT_MAX_WIDTH] and centred, so a
 * desktop or tablet window does not stretch the map and the values edge to edge.
 *
 * The field labels are the schema/API names and are deliberately not localized,
 * like the infrastructure dashboard's labels.
 */
@Composable
fun AreaAdminScreen(
    area: Area,
    modifier: Modifier = Modifier,
    map: (@Composable () -> Unit)? = null,
    onOpenUrl: (String) -> Unit = {},
    fieldAction: ((AreaAdminField) -> AreaFieldAction?)? = null,
    onFieldAction: ((AreaAdminField) -> Unit)? = null,
) {
    val fields = remember(area) { areaAdminFields(area) }

    ContentColumn(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            map?.let { preview ->
                item(key = "map") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(AREA_ADMIN_MAP_HEIGHT)
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        preview()
                    }
                }
            }
            items(fields, key = { it.label }) { field ->
                AreaAdminFieldRow(field, onOpenUrl, fieldAction, onFieldAction)
            }
        }
    }
}

/**
 * One field row. A plain row is announced as a single label/value stop for a
 * screen reader; a URL value is instead a link, and a field with an action keeps
 * its own focusable node rather than merging into the row.
 */
@Composable
private fun AreaAdminFieldRow(
    field: AreaAdminField,
    onOpenUrl: (String) -> Unit,
    fieldAction: ((AreaAdminField) -> AreaFieldAction?)?,
    onFieldAction: ((AreaAdminField) -> Unit)?,
) {
    val url = field.value.takeIf { it.toUrlOrNull() != null }
    val action = fieldAction?.invoke(field)
    val handler = onFieldAction

    ListItem(
        overlineContent = { Text(field.label) },
        headlineContent = {
            if (url != null) {
                Text(
                    text = url,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onOpenUrl(url) },
                )
            } else {
                Text(field.value)
            }
        },
        trailingContent = if (action != null && handler != null) {
            {
                IconButton(
                    onClick = { handler(field) },
                    enabled = action.enabled,
                    modifier = Modifier.testTag(areaAdminActionTag(field.label)),
                ) {
                    MaterialSymbol(glyph = action.icon, contentDescription = action.description)
                }
            }
        } else {
            null
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (url == null && action == null) {
            Modifier.semantics(mergeDescendants = true) {}
        } else {
            Modifier
        },
    )
}

/** Every cached field of [area], in the order the projection reads them. */
internal fun areaAdminFields(area: Area): List<AreaAdminField> = listOf(
    AreaAdminField("id", area.id.toString()),
    AreaAdminField("name", area.name, AreaAdminFieldKind.Name),
    AreaAdminField("type", area.type),
    AreaAdminField("url_alias", area.urlAlias),
    AreaAdminField("website_url", area.websiteUrl),
    AreaAdminField("description", area.description ?: NOT_SET, AreaAdminFieldKind.Description),
    AreaAdminField("icon", area.icon ?: NOT_SET),
    AreaAdminField("icon_wide", area.iconWide ?: NOT_SET),
    AreaAdminField("bbox", area.bboxText()),
    AreaAdminField("geo_json", area.geoJsonText()),
    AreaAdminField("verified_at", area.verifiedAt ?: NOT_SET, AreaAdminFieldKind.VerifiedAt),
    AreaAdminField("updated_at", area.updatedAt.toString()),
    AreaAdminField("deleted_at", area.deletedAt?.toString() ?: NOT_SET),
    AreaAdminField("localized_name", area.localizedName?.toString() ?: NOT_SET),
    AreaAdminField("localized_description", area.localizedDescription?.toString() ?: NOT_SET),
)

private fun Area.bboxText(): String {
    val west = bboxWest ?: return NOT_SET
    val south = bboxSouth ?: return NOT_SET
    val east = bboxEast ?: return NOT_SET
    val north = bboxNorth ?: return NOT_SET
    return "$west, $south, $east, $north"
}

private fun Area.geoJsonText(): String {
    val json = geoJson ?: return NOT_SET
    return if (json.length <= GEO_JSON_PREVIEW) {
        json
    } else {
        "${json.take(GEO_JSON_PREVIEW)}… (${json.length} chars)"
    }
}
