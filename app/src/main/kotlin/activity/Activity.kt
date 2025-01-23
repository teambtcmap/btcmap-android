package activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.btcmap.databinding.ActivityBinding
import org.maplibre.android.MapLibre

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        binding = ActivityBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        window.isNavigationBarContrastEnforced = false
    }
}