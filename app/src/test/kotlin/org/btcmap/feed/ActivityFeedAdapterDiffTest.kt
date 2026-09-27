package org.btcmap.feed

import org.btcmap.api.ActivityFeedItem
import org.junit.Assert
import org.junit.Test

class ActivityFeedAdapterDiffTest {

    private val callback = ActivityFeedAdapter.DiffCallback()

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
    fun areItemsTheSame_whenTypePlaceAndDateMatch() {
        Assert.assertTrue(callback.areItemsTheSame(item(), item(comment = "changed")))
    }

    @Test
    fun areItemsTheSame_whenTypeDiffers() {
        Assert.assertFalse(
            callback.areItemsTheSame(item(), item(type = ActivityFeedItem.TYPE_PLACE_BOOSTED)),
        )
    }

    @Test
    fun areItemsTheSame_whenPlaceDiffers() {
        Assert.assertFalse(callback.areItemsTheSame(item(), item(placeId = 2L)))
    }

    @Test
    fun areItemsTheSame_whenDateDiffers() {
        Assert.assertFalse(callback.areItemsTheSame(item(), item(date = "2026-04-20T12:00:01Z")))
    }

    @Test
    fun areContentsTheSame_whenAllFieldsMatch() {
        Assert.assertTrue(callback.areContentsTheSame(item(), item()))
    }

    @Test
    fun areContentsTheSame_whenCommentChanged() {
        Assert.assertFalse(callback.areContentsTheSame(item(), item(comment = "hi")))
    }
}
