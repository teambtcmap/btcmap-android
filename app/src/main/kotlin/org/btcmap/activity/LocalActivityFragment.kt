package org.btcmap.activity

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

class LocalActivityFragment : BaseActivityFeedTab() {

    override fun emptyMessage(): String = "No local activity"

    companion object {
        fun create(areas: List<Area>): Fragment {
            return LocalActivityFragment().apply {
                arguments = bundleOf(
                    ARG_SHOW_AREA_CHIPS to true,
                    ARG_INITIAL_AREA_IDS to ArrayList(areas.map { it.id }),
                    ARG_INITIAL_AREA_NAMES to ArrayList(areas.map { it.name }),
                    ARG_INITIAL_AREA_TYPES to ArrayList(areas.map { it.type }),
                )
            }
        }

        fun create(ids: List<String>, names: List<String>, types: List<String>): Fragment {
            return create(
                ids.indices.map { i ->
                    Area(
                        id = ids[i],
                        name = names.getOrNull(i) ?: ids[i],
                        type = types.getOrNull(i) ?: "",
                    )
                }
            )
        }
    }
}
