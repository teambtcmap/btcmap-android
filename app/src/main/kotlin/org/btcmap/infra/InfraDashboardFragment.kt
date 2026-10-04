package org.btcmap.infra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.btcmap.R
import org.btcmap.api
import org.btcmap.api.getDashboard
import org.btcmap.databinding.InfraDashboardFragmentBinding
import org.btcmap.util.iconTypeface

/**
 * The admin-only infrastructure dashboard: a Views toolbar over the shared
 * [org.btcmap.ui.InfraDashboardScreen], which calls the `dashboard` RPC method
 * and renders the snapshot as cards. Reachable from the map's dashboard button,
 * which is only shown to admins and roots.
 */
class InfraDashboardFragment : Fragment() {

    private var _binding: InfraDashboardFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = InfraDashboardFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.topAppBar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // The refresh action is a custom action view (a refresh icon that swaps
        // to a spinner), so its click is wired on the view itself.
        binding.topAppBar.menu.findItem(R.id.refresh)?.actionView
            ?.findViewById<View>(R.id.refreshButton)
            ?.setOnClickListener { binding.dashboardContent.refreshKey++ }

        // Read before apply(): inside it, the name would resolve to the view's
        // own property instead of the icon font built from the assets.
        val typeface = iconTypeface
        binding.dashboardContent.apply {
            load = { api().getDashboard() }
            iconTypeface = typeface
            onLoadingChange = { loading -> setRefreshLoading(loading) }
        }
    }

    /** Swaps the toolbar's refresh icon for a spinner while a load is running. */
    private fun setRefreshLoading(loading: Boolean) {
        val actionView = _binding?.topAppBar?.menu?.findItem(R.id.refresh)?.actionView ?: return
        actionView.findViewById<View>(R.id.refreshButton)?.visibility =
            if (loading) View.GONE else View.VISIBLE
        actionView.findViewById<View>(R.id.refreshProgress)?.visibility =
            if (loading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
