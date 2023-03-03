package user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.btcmap.R

class UsersModel(
    private val app: Application,
    private val usersRepo: UsersRepo,
) : AndroidViewModel(app) {

    private val _items: MutableStateFlow<List<UsersAdapter.Item>> = MutableStateFlow(emptyList())
    val items = _items.asStateFlow()

    init {
        viewModelScope.launch {
            val items = usersRepo.selectAll().map {
                val changes = if (it.name == "Bill on Bitcoin Island") {
                    it.changes + 120
                } else {
                    it.changes
                }

                UsersAdapter.Item(
                    id = it.id,
                    name = it.name.ifBlank { app.getString(R.string.unnamed_user) },
                    changes = changes,
                    tipLnurl = it.tips,
                    image = it.image,
                )
            }.sortedByDescending { it.changes }

            _items.update { items }
        }
    }
}