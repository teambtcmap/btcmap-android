package conf

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import org.koin.core.annotation.Single

@Single
class ConfRepo(
    private val queries: ConfQueries,
) {

    private val _conf: MutableStateFlow<Conf> = MutableStateFlow(
        runBlocking { queries.select() ?: DEFAULT_CONF }
    )

    val conf: StateFlow<Conf> = _conf.asStateFlow()

    init {
        conf
            .onEach { queries.insertOrReplace(it) }
            .launchIn(GlobalScope)
    }

    fun update(newConf: (Conf) -> Conf) {
        _conf.update { newConf(conf.value) }
    }

    companion object {
        val DEFAULT_CONF = Conf(
            lastSyncDate = null,
            themedPins = true,
            darkMap = false,
            viewportNorthLat = 11.994133785187255,
            viewportEastLon = 121.95219572432649,
            viewportSouthLat = 11.945223417353624,
            viewportWestLon = 121.90219745907318,
            showTags = false,
            osmLogin = "",
            osmPassword = "",
        )
    }
}