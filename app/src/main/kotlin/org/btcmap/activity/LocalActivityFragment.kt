package org.btcmap.activity

import android.os.Bundle
import androidx.fragment.app.Fragment

class LocalActivityFragment : BaseActivityFeedTab() {

    override fun emptyMessage(): String = "No local activity"

    companion object {
        fun create(areas: List<Area>): Fragment {
            return LocalActivityFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SHOW_AREA_CHIPS, true)
                    putStringArrayList(ARG_INITIAL_AREA_IDS, ArrayList(areas.map { it.id }))
                    putStringArrayList(ARG_INITIAL_AREA_NAMES, ArrayList(areas.map { it.name }))
                    putStringArrayList(ARG_INITIAL_AREA_TYPES, ArrayList(areas.map { it.type }))
                }
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
