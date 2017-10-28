import android.arch.persistence.room.Room
import android.content.Context
import com.bubelov.coins.db.Database
import com.bubelov.coins.db.DatabaseConfig
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * @author Igor Bubelov
 */

@Module
class DatabaseTestModule {
    @Provides
    @Singleton
    fun provideDatabase(context: Context, databaseConfig: DatabaseConfig) = Room.inMemoryDatabaseBuilder(context, Database::class.java).apply {
        if (databaseConfig.canUseMainThread) {
            allowMainThreadQueries()
        }
    }.build()

    @Provides
    @Singleton
    fun provideDatabaseConfig() = DatabaseConfig(canUseMainThread = true)
}