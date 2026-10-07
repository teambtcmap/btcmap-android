package org.btcmap.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import org.btcmap.Activity
import org.btcmap.i18n.Strings

/**
 * Opens a bolt11 invoice in the user's lightning wallet, telling them when no
 * wallet is installed. Shared by the boost and comment payment screens.
 */
fun Fragment.openLightningWallet(bolt11: String) {
    val intent = Intent(Intent.ACTION_VIEW, "lightning:$bolt11".toUri())
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        (activity as? Activity)?.showMessage(Strings.current()["you_dont_have_a_compatible_wallet"])
    }
}

/** Copies a bolt11 invoice to the clipboard under [label]. */
fun Fragment.copyBolt11(label: String, bolt11: String) {
    val clipboard =
        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, bolt11))
    (activity as? Activity)?.showMessage(Strings.current()["copied_to_clipboard"])
}
