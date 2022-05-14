package common

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.btcmap.databinding.ActivityAppBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class Activity : AppCompatActivity() {

    private val model: ActivityModel by viewModel()

    private lateinit var binding: ActivityAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        model.syncPlaces()
    }
}