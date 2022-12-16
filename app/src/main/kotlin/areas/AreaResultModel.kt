package areas

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class AreaResultModel : ViewModel() {

    val area = MutableStateFlow<Area?>(null)
}