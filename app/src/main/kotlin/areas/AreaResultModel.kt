package areas

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow

class AreaResultModel : ViewModel() {

    val area = MutableStateFlow<Area?>(null)
}