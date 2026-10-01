package org.btcmap.feed

import org.btcmap.api.ActivityFeedItem
import org.junit.Assert
import org.junit.Test

/**
 * The activity feed's row identity: rows are keyed by type, place and date, so a
 * re-fetch that only changes a comment keeps the same key (and the list reuses
 * the row) while a different type, place or date gets a new one.
 */
class ActivityFeedKeyTest {

    private fun item(
        type: String = ActivityFeedItem.TYPE_PLACE_ADDED,
        placeId: Long = 1L,
        date: String = "2026-04-20T12:00:00Z",
        comment: String? = null,
    ) = ActivityFeedItem(
        type = type,
        placeId = placeId,
        placeName = "Place",
        osmUserId = null,
        osmUserName = null,
        osmUserTip = null,
        image = null,
        date = date,
        durationDays = null,
        comment = comment,
    )

    @Test
    fun key_isStableWhenOnlyTheCommentChanges() {
        Assert.assertEquals(item().feedKey(), item(comment = "changed").feedKey())
    }

    @Test
    fun key_differsWhenTypeDiffers() {
        Assert.assertNotEquals(
            item().feedKey(),
            item(type = ActivityFeedItem.TYPE_PLACE_BOOSTED).feedKey(),
        )
    }

    @Test
    fun key_differsWhenPlaceDiffers() {
        Assert.assertNotEquals(item().feedKey(), item(placeId = 2L).feedKey())
    }

    @Test
    fun key_differsWhenDateDiffers() {
        Assert.assertNotEquals(item().feedKey(), item(date = "2026-04-20T12:00:01Z").feedKey())
    }
}
