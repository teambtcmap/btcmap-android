import android.arch.persistence.room.Room
import android.content.Context
import com.bubelov.coins.database.Database
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
    fun provideDatabase(context: Context) = Room.inMemoryDatabaseBuilder(context, Database::class.java)
            .allowMainThreadQueries()
            .build()
}