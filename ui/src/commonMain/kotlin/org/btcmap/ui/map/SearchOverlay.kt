package org.btcmap.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.btcmap.search.SearchAdapterItem
import org.btcmap.ui.MaterialSymbol

/**
 * The map search field and its results, ported from the `SearchView` in
 * `map_fragment.xml`. The results are the local matches from
 * [rememberSearchResults].
 */
@Composable
fun SearchOverlay(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<SearchAdapterItem>,
    onResultClick: (SearchAdapterItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = { MaterialSymbol(glyph = "search", contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    MaterialSymbol(
                        glyph = "close",
                        contentDescription = null,
                        modifier = Modifier.clickable { onQueryChange("") },
                    )
                }
            },
            placeholder = { Text("Search") },
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        if (results.isNotEmpty()) {
            Spacer(modifier = Modifier.size(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(results) { result ->
                        SearchResultRow(result = result, onClick = onResultClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    result: SearchAdapterItem,
    onClick: (SearchAdapterItem) -> Unit,
) {
    val boosted = (result as? SearchAdapterItem.Place)?.boosted == true
    val areaIconUrl = (result as? SearchAdapterItem.Area)?.iconUrl

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(result) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (areaIconUrl != null) {
            AsyncImage(
                model = areaIconUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
            )
        } else {
            MaterialSymbol(glyph = result.icon, contentDescription = null)
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = result.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (boosted) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )

        result.distanceToUser?.let { distance ->
            Text(text = distance, style = MaterialTheme.typography.bodySmall)
        }
    }
}
