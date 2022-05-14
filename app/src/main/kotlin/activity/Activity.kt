package activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import db.database
import org.btcmap.BuildConfig
import org.btcmap.databinding.ActivityBinding
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.ksp.generated.defaultModule

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.DEBUG)
            androidContext(applicationContext)
            defaultModule()
            modules(module { single { database(applicationContext) } })
        }

        binding = ActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}