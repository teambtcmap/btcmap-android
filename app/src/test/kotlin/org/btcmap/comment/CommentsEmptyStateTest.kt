package org.btcmap.comment

import org.btcmap.R
import org.junit.Assert
import org.junit.Test

class CommentsEmptyStateTest {

    @Test
    fun beforeTheSyncFinishes_nothingIsShown() {
        Assert.assertNull(
            commentsEmptyStateMessageRes(
                syncFinished = false,
                syncFailed = false,
                hasComments = false,
            )
        )
        Assert.assertNull(
            commentsEmptyStateMessageRes(
                syncFinished = false,
                syncFailed = true,
                hasComments = false,
            )
        )
    }

    @Test
    fun withComments_nothingIsShown() {
        Assert.assertNull(
            commentsEmptyStateMessageRes(
                syncFinished = true,
                syncFailed = false,
                hasComments = true,
            )
        )
        Assert.assertNull(
            commentsEmptyStateMessageRes(
                syncFinished = true,
                syncFailed = true,
                hasComments = true,
            )
        )
    }

    @Test
    fun emptyAfterASuccessfulSync_saysThereAreNone() {
        Assert.assertEquals(
            R.string.no_comments_yet,
            commentsEmptyStateMessageRes(
                syncFinished = true,
                syncFailed = false,
                hasComments = false,
            ),
        )
    }

    @Test
    fun emptyAfterAFailedSync_saysTheLoadFailed() {
        Assert.assertEquals(
            R.string.failed_to_load,
            commentsEmptyStateMessageRes(
                syncFinished = true,
                syncFailed = true,
                hasComments = false,
            ),
        )
    }
}
