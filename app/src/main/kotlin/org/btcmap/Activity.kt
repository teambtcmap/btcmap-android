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
import org.btcmap.util.deepLinkPlaceId

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    private var pendingDeepLinkPlaceId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLinkPlaceId = intent.deepLinkPlaceId()
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
        pendingDeepLinkPlaceId = intent.deepLinkPlaceId()
        deliverDeepLink()
    }

    internal fun consumeDeepLinkPlaceId(): Long? {
        val placeId = pendingDeepLinkPlaceId
        pendingDeepLinkPlaceId = null
        return placeId
    }

    private fun deliverDeepLink() {
        val placeId = pendingDeepLinkPlaceId ?: return

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

        if (fragment == null || !fragment.isAdded || fragment.view == null) return

        pendingDeepLinkPlaceId = null
        fragment.openPlaceById(placeId)
    }

    private fun Intent.deepLinkPlaceId(): Long? {
        return dataString?.toHttpUrlOrNull()?.deepLinkPlaceId()
    }
}
