package org.btcmap.comment

import androidx.lifecycle.ViewModel

/**
 * Retains the comment text submitted for payment across a configuration
 * change, so the paid result can hand the exact text back to the comments list
 * for its post-payment retry.
 */
internal class AddCommentViewModel : ViewModel() {
    var postedComment: String? = null
}
