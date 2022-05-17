package activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.btcmap.databinding.ActivityBinding

class Activity : AppCompatActivity() {

    private lateinit var binding: ActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}