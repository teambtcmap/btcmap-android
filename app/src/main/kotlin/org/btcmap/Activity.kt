package org.btcmap

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.btcmap.util.toUrlOrNull
import org.btcmap.databinding.ActivityBinding
import org.btcmap.map.MapFragment
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

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            // The map is full-bleed and draws its own controls, so padding it
            // for the keyboard would shrink the map and shift its camera when
            // the search field is focused. Only the other screens need to clear
            // the keyboard.
            val onMap = supportFragmentManager
                .findFragmentById(R.id.fragmentContainerView) is MapFragment
            view.updatePadding(bottom = if (onMap) 0 else ime)
            windowInsets
        }

        // Whether the keyboard inset is applied depends on the current screen,
        // and switching screens does not change the insets themselves, so
        // re-dispatch them whenever one is shown.
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    ViewCompat.requestApplyInsets(binding.root)
                }
            },
            false,
        )

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
        // Take ownership right away so restoring MapFragment below doesn't deliver it a second time.
        pendingDeepLink = null

        var fragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as? MapFragment

        if (fragment?.view == null) {
            supportFragmentManager.popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE,
            )
            fragment = supportFragmentManager
                .findFragmentById(R.id.fragmentContainerView) as? MapFragment
        }

        if (fragment == null || !fragment.isAdded || fragment.view == null) {
            pendingDeepLink = deepLink
            return
        }

        when (deepLink) {
            is DeepLink.Place -> fragment.openPlaceById(deepLink.id)
            is DeepLink.Event -> fragment.openEventById(deepLink.id)
        }
    }

    private fun Intent.deepLink(): DeepLink? {
        return dataString?.toUrlOrNull()?.deepLink()
    }
}
