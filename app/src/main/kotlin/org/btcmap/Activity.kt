package org.btcmap

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.btcmap.databinding.ActivityBinding
import org.btcmap.map.MapFragment
import org.btcmap.util.DeepLink
import org.btcmap.util.deepLink

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    private var pendingDeepLink: DeepLink? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only handle the launching link on a fresh start: on recreation the restored
        // back stack already contains the target screen, and re-delivering causes duplicates.
        pendingDeepLink = if (savedInstanceState == null) intent.deepLink() else null
        enableEdgeToEdge() // TODO remove once min api is 35
        window.isNavigationBarContrastEnforced = false // remove nav bar scrim for 3 button mode
        binding = ActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.updatePadding(bottom = ime)
            windowInsets
        }

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
        return dataString?.toHttpUrlOrNull()?.deepLink()
    }
}
