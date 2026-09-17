package org.btcmap.comment

import org.junit.Assert
import org.junit.Test

class CommentsAdapterDiffTest {

    private val callback = CommentsAdapter.DiffCallback()

    private fun item(
        id: Long,
        comment: String = "gm",
        date: String = "Jun 1, 2024",
    ) = CommentsAdapterItem(id = id, comment = comment, localizedDate = date)

    @Test
    fun areItemsTheSame_whenIdsMatch() {
        Assert.assertTrue(callback.areItemsTheSame(item(1L), item(1L, comment = "changed")))
    }

    @Test
    fun areItemsTheSame_whenIdsDiffer() {
        Assert.assertFalse(callback.areItemsTheSame(item(1L), item(2L)))
    }

    @Test
    fun areItemsTheSame_whenTextIsIdenticalButIdsDiffer() {
        Assert.assertFalse(callback.areItemsTheSame(item(1L), item(2L, comment = "gm")))
    }

    @Test
    fun areContentsTheSame_whenAllFieldsMatch() {
        Assert.assertTrue(callback.areContentsTheSame(item(1L), item(1L)))
    }

    @Test
    fun areContentsTheSame_whenCommentChanged() {
        Assert.assertFalse(callback.areContentsTheSame(item(1L), item(1L, comment = "updated")))
    }

    @Test
    fun areContentsTheSame_whenDateChanged() {
        Assert.assertFalse(callback.areContentsTheSame(item(1L), item(1L, date = "Jun 2, 2024")))
    }
}
