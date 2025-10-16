package activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.btcmap.databinding.ActivityBinding

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // TODO remove once min api is 35
        window.isNavigationBarContrastEnforced = false // remove nav bar scrim for 3 button mode
        binding = ActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}