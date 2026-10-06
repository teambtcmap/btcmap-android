package org.btcmap

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.btcmap.util.toUrlOrNull
import org.btcmap.databinding.ActivityBinding
import org.btcmap.nav.AppRootFragment
import org.btcmap.util.DeepLink
import org.btcmap.util.deepLink

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    private var pendingDeepLink: DeepLink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App
        if (app.databaseReady.isCompleted) {
            setUpContent(savedInstanceState)
        } else {
            // The database is being opened (and migrated, when needed) off the
            // main thread. Building the first screen before it is ready would
            // open it on the main thread, so wait for it without blocking.
            lifecycleScope.launch {
                app.databaseReady.await()
                setUpContent(savedInstanceState)
            }
        }
    }

    private fun setUpContent(savedInstanceState: Bundle?) {
        // Only handle the launching link on a fresh start: on recreation the restored
        // back stack already contains the target screen, and re-delivering causes duplicates.
        pendingDeepLink = if (savedInstanceState == null) intent.deepLink() else null
        enableEdgeToEdge() // TODO remove once min api is 35
        window.isNavigationBarContrastEnforced = false // remove nav bar scrim for 3 button mode
        binding = ActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // The keyboard inset is handled in Compose (AppRoot), so the full-bleed
        // map is never padded and its camera does not move when a field is
        // focused. The map draws its own insets; the screens clear the keyboard.

        deliverDeepLink()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink = intent.deepLink()
        deliverDeepLink()
    }

    internal fun consumeDeepLink(): DeepLink? {
        val deepLink = pendingDeepLink
        pendingDeepLink = null
        return deepLink
    }

    /**
     * Opens [placeId] on the map, popping any screens above it. Used by a screen
     * that is not the map (for example an area's boosted merchants), so tapping
     * a merchant selects it the same way a btcmap.org merchant deep link does.
     */
    internal fun openPlace(placeId: Long) {
        pendingDeepLink = DeepLink.Place(placeId)
        deliverDeepLink()
    }

    /**
     * Shows a transient confirmation over the current screen. It lives on the
     * activity so a message can outlive the fragment that raised it, for example
     * a payment screen that closes right after a successful payment.
     */
    internal fun showMessage(message: CharSequence) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun deliverDeepLink() {
        val deepLink = pendingDeepLink ?: return
        // Take ownership right away so a queued link is not delivered twice.
        pendingDeepLink = null

        val root = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as? AppRootFragment

        if (root == null || !root.isAdded) {
            pendingDeepLink = deepLink
            return
        }

        when (deepLink) {
            is DeepLink.Place -> root.openPlace(deepLink.id)
            is DeepLink.Event -> root.openEventById(deepLink.id)
        }
    }

    private fun Intent.deepLink(): DeepLink? {
        return dataString?.toUrlOrNull()?.deepLink()
    }
}
