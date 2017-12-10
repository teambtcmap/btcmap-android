import android.content.Context
import com.bubelov.coins.dagger.AppModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Singleton
@Component(modules = [AppModule::class, DatabaseTestModule::class])
interface TestComponent {
    fun inject(target: ExchangeRatesRepositoryTest)
    fun inject(target: NotificationAreaRepositoryTest)
    fun inject(target: PlacesAssetsCacheTest)
    fun inject(target: PlacesRepositoryTest)
    fun inject(target: SyncLogsRepositoryTest)
    fun inject(target: DatabaseSyncTest)
    fun inject(target: PlacesDbTests)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder
        fun build(): TestComponent
    }
}