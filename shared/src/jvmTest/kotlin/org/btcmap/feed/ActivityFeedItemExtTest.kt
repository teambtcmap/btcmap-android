package org.btcmap.feed

import org.btcmap.api.ActivityFeedItem
import org.junit.Assert
import org.junit.Test

class ActivityFeedItemExtTest {

    private fun item(type: String) = ActivityFeedItem(
        type = type,
        placeId = 7,
        placeName = "Cafe",
        osmUserId = null,
        osmUserName = null,
        osmUserTip = null,
        image = null,
        date = "2026-01-02T03:04:05Z",
        durationDays = null,
        comment = null,
    )

    @Test
    fun feedKey_identifiesTheRow() {
        Assert.assertEquals(
            "place_added:7:2026-01-02T03:04:05Z",
            item(ActivityFeedItem.TYPE_PLACE_ADDED).feedKey(),
        )
    }

    @Test
    fun iconGlyph_mapsEachKind() {
        Assert.assertEquals("add_location", item(ActivityFeedItem.TYPE_PLACE_ADDED).iconGlyph())
        Assert.assertEquals("edit", item(ActivityFeedItem.TYPE_PLACE_UPDATED).iconGlyph())
        Assert.assertEquals("delete", item(ActivityFeedItem.TYPE_PLACE_DELETED).iconGlyph())
        Assert.assertEquals("comment", item(ActivityFeedItem.TYPE_PLACE_COMMENTED).iconGlyph())
        Assert.assertEquals("rocket_launch", item(ActivityFeedItem.TYPE_PLACE_BOOSTED).iconGlyph())
        Assert.assertEquals("place", item("something_else").iconGlyph())
    }
}
