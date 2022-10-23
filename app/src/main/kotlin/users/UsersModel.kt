package users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import db.SelectAllUsersAsListItems
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.btcmap.R
import org.koin.android.annotation.KoinViewModel
import java.util.regex.Pattern

@KoinViewModel
class UsersModel(
    private val app: Application,
    private val usersRepo: UsersRepo,
): AndroidViewModel(app) {

    private val _items: MutableStateFlow<List<UsersAdapter.Item>> = MutableStateFlow(emptyList())
    val items = _items.asStateFlow()

    init {
        viewModelScope.launch {
            val items = usersRepo.selectAllUsersAsListItems().map {
                val changes = if (it.user_name == "Bill on Bitcoin Island") {
                    it.changes + 120
                } else {
                    it.changes
                }

                UsersAdapter.Item(
                    id = it.user_id,
                    name = it.user_name ?: app.getString(R.string.unnamed_user),
                    changes = changes,
                    tipLnurl = it.lnurl(),
                    imgHref = it.user_img_href ?: "",
                )
            }.sortedByDescending { it.changes }

            _items.update { items }
        }
    }

    fun SelectAllUsersAsListItems.lnurl(): String {
        val description = user_description ?: ""
        val pattern = Pattern.compile("\\(lightning:[^)]*\\)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(description)
        val matchFound: Boolean = matcher.find()

        return if (matchFound) {
            matcher.group().trim('(', ')')
        } else {
            ""
        }
    }
}