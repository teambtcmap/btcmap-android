package tags

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import conf.ConfRepo
import elements.ElementsRepo
import http.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.btcmap.BuildConfig
import org.btcmap.R
import org.btcmap.databinding.FragmentTagsBinding
import org.koin.android.ext.android.inject

class TagsFragment : Fragment() {

    private var _binding: FragmentTagsBinding? = null
    private val binding get() = _binding!!

    private val elementsRepo: ElementsRepo by inject()

    private val confRepo: ConfRepo by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            val navBarsInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.root.setPadding(0, 0, 0, navBarsInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        binding.toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_add_tag) {
                val tagView = TagView(requireContext())
                tagView.onDeleteListener = TagView.OnDeleteListener { binding.tagsContainer -= it }
                binding.tagsContainer += tagView
            }

            true
        }

        val elementId = TagsFragmentArgs.fromBundle(requireArguments()).elementId

        if (elementId.split(":").first() != "node") {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.ways_and_relations_are_not_supported)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener { findNavController().popBackStack() }
                .show()
            return
        }

        val element = runBlocking { elementsRepo.selectById(elementId)!! }

        val tags = element.osm_json["tags"]?.jsonObject!!

        binding.toolbar.title =
            tags["name"]?.jsonPrimitive?.content ?: getString(R.string.unnamed_place)

        for (entry in tags) {
            val tagView = TagView(requireContext())
            tagView.setValue(entry.key, entry.value.jsonPrimitive.content)
            tagView.onDeleteListener = TagView.OnDeleteListener { binding.tagsContainer -= it }
            binding.tagsContainer += tagView
        }

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }

        binding.upload.setOnClickListener {
            if (binding.comment.text.toString().isBlank()) {
                MaterialAlertDialogBuilder(requireContext())
                    .setMessage(R.string.please_describe_your_changes)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                return@setOnClickListener
            }

            binding.tagsScrollView.isVisible = false
            binding.commentLayout.isVisible = false
            binding.upload.isVisible = false

            binding.progress.isVisible = true

            viewLifecycleOwner.lifecycleScope.launch {
                runCatching {
                    val conf = confRepo.conf.value

                    val osmCredentials = Credentials.basic(
                        conf.osmLogin,
                        conf.osmPassword,
                    )

                    val putChangesetBody = """
                        <osm>
                        	<changeset>
                        		<tag k="created_by" v="BTC Map Android ${BuildConfig.VERSION_NAME}"/>
                        		<tag k="comment" v="${binding.comment.text}"/>
                        	</changeset>
                        </osm>
                    """.trimIndent()

                    val putChangesetResponse = OkHttpClient().newCall(
                        Request.Builder()
                            .url("https://api.openstreetmap.org/api/0.6/changeset/create")
                            .header("Authorization", osmCredentials)
                            .put(putChangesetBody.toRequestBody())
                            .build()
                    ).await()

                    if (putChangesetResponse.isSuccessful) {
                        val changesetId = putChangesetResponse.body!!.string()
                        val nodeId = elementId.split(":").last()
                        val nodeLat = element.lat
                        val nodeLon = element.lon
                        val nodeVer = element.osm_json["version"]!!.jsonPrimitive.long

                        val tagsString =
                            binding.tagsContainer.children.filterIsInstance<TagView>().mapNotNull {
                                val value = it.getValue()

                                if (value.first.isBlank() || value.second.isBlank()) {
                                    return@mapNotNull null
                                }

                                """<tag k="${value.first}" v="${value.second}" />"""
                            }.toList().joinToString("\n")

                        val putNodeBody = """
                            <osm>
                            	<node changeset="$changesetId" id="$nodeId" lat="$nodeLat" lon="$nodeLon" version="$nodeVer" visible="true">
                            		$tagsString
                            	</node>
                            </osm>
                        """.trimIndent()

                        val putNodeResponse = OkHttpClient().newCall(
                            Request.Builder()
                                .url("https://api.openstreetmap.org/api/0.6/node/$nodeId")
                                .header("Authorization", osmCredentials)
                                .put(putNodeBody.toRequestBody())
                                .build()
                        ).await()

                        if (putNodeResponse.isSuccessful) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setMessage(R.string.your_changes_have_been_uploaded)
                                .setPositiveButton(android.R.string.ok, null)
                                .setOnDismissListener { findNavController().popBackStack() }
                                .show()

                            binding.progress.isVisible = false
                        } else {
                            MaterialAlertDialogBuilder(requireContext())
                                .setMessage(R.string.failed_to_update_tags)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()

                            binding.tagsScrollView.isVisible = true
                            binding.commentLayout.isVisible = true
                            binding.upload.isVisible = true

                            binding.progress.isVisible = false
                        }
                    } else {
                        MaterialAlertDialogBuilder(requireContext())
                            .setMessage(R.string.failed_to_create_a_changeset)
                            .setPositiveButton(android.R.string.ok, null)
                            .show()

                        binding.tagsScrollView.isVisible = true
                        binding.commentLayout.isVisible = true
                        binding.upload.isVisible = true

                        binding.progress.isVisible = false
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}