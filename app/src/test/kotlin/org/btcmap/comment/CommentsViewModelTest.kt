package org.btcmap.comment

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

/**
 * Drives the post-payment retry state machine directly, without the fragment or
 * a device. The `sync` and `render` calls are fakes, so the retry window and
 * the baseline logic can be exercised deterministically under virtual time.
 */
class CommentsViewModelTest {

    private val viewModel = CommentsViewModel()

    @Test
    fun syncOnResume_withoutPendingPayment_runsASingleSync() = runTest {
        var syncs = 0

        val result = viewModel.syncOnResume(
            renderedIds = emptySet(),
            sync = { syncs++; true },
            render = { error("the retry must not run without a pending payment") },
        )

        Assert.assertTrue(result)
        Assert.assertEquals(1, syncs)
    }

    @Test
    fun syncOnResume_withoutPendingPayment_reportsAFailedSync() = runTest {
        val result = viewModel.syncOnResume(
            renderedIds = emptySet(),
            sync = { false },
            render = { error("the retry must not run without a pending payment") },
        )

        Assert.assertFalse(result)
    }

    @Test
    fun syncOnResume_afterPayment_doesNotSyncWhenTheCommentIsAlreadyShown() = runTest {
        viewModel.onCommentsRendered(setOf(1L))
        viewModel.onAddCommentOpened()
        viewModel.onCommentPosted()
        var syncs = 0

        val result = viewModel.syncOnResume(
            renderedIds = setOf(1L, 9L),
            sync = { syncs++; true },
            render = { setOf(1L, 9L) },
        )

        Assert.assertTrue(result)
        Assert.assertEquals(0, syncs)
    }

    @Test
    fun syncOnResume_afterPayment_retriesUntilTheCommentIsPublished() = runTest {
        viewModel.onCommentsRendered(setOf(1L))
        viewModel.onAddCommentOpened()
        viewModel.onCommentPosted()
        var syncs = 0

        val result = viewModel.syncOnResume(
            renderedIds = setOf(1L),
            sync = { syncs++; true },
            // The server publishes the paid comment on the third sync.
            render = { if (syncs >= 3) setOf(1L, 9L) else setOf(1L) },
        )

        Assert.assertTrue(result)
        Assert.assertEquals(3, syncs)
    }

    @Test
    fun syncOnResume_afterPayment_reportsFailureWhenTheServerIsUnreachable() = runTest {
        viewModel.onCommentsRendered(setOf(1L))
        viewModel.onAddCommentOpened()
        viewModel.onCommentPosted()
        var syncs = 0

        val result = viewModel.syncOnResume(
            renderedIds = setOf(1L),
            sync = { syncs++; false },
            render = { setOf(1L) },
        )

        Assert.assertFalse(result)
        Assert.assertTrue("the retry must keep trying until the window ends", syncs > 1)
    }

    @Test
    fun syncOnResume_withoutAPriorRender_fallsBackToTheRenderedIds() = runTest {
        // The add screen was opened before the list rendered, so no baseline was
        // snapshotted. The paid comment cannot then be told apart from the ones
        // already shown, so the retry falls back to them and runs its window.
        viewModel.onAddCommentOpened()
        viewModel.onCommentPosted()
        var syncs = 0

        viewModel.syncOnResume(
            renderedIds = setOf(1L),
            sync = { syncs++; true },
            render = { setOf(1L) },
        )

        Assert.assertTrue(syncs > 1)
    }

    @Test
    fun onViewDestroyed_dropsTheStaleBaseline() = runTest {
        // A baseline snapshotted before the view was destroyed must not survive
        // into the recreated view, or a render that happens to include the paid
        // comment would short-circuit the retry.
        viewModel.onCommentsRendered(setOf(1L))
        viewModel.onViewDestroyed()
        viewModel.onAddCommentOpened()
        viewModel.onCommentPosted()
        var syncs = 0

        viewModel.syncOnResume(
            renderedIds = setOf(1L, 9L),
            sync = { syncs++; true },
            render = { setOf(1L, 9L) },
        )

        Assert.assertTrue(syncs >= 1)
    }

    @Test
    fun syncOnResume_clearsThePendingPaymentAfterTheRetry() = runTest {
        viewModel.onCommentsRendered(setOf(1L))
        viewModel.onAddCommentOpened()
        viewModel.onCommentPosted()
        viewModel.syncOnResume(
            renderedIds = setOf(1L),
            sync = { true },
            render = { setOf(1L, 9L) },
        )

        var syncs = 0
        val result = viewModel.syncOnResume(
            renderedIds = setOf(1L, 9L),
            sync = { syncs++; true },
            render = { error("the retry must not run twice") },
        )

        Assert.assertTrue(result)
        Assert.assertEquals(1, syncs)
    }
}
