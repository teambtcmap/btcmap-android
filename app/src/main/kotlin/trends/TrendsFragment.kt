package trends

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import http.await
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import map.getOnSurfaceColor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.btcmap.databinding.FragmentTrendsBinding
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class TrendsFragment : Fragment() {

    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { toolbar, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = insets.top
            }
            WindowInsetsCompat.CONSUMED
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        WindowCompat.getInsetsController(
            requireActivity().window,
            requireActivity().window.decorView,
        ).isAppearanceLightStatusBars =
            when (requireContext().resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> true
                else -> false
            }

        binding.chartUpToDateElements.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val date = Instant.ofEpochMilli(value.toLong())
                    return OffsetDateTime.ofInstant(date, ZoneOffset.UTC).toLocalDate().toString()
                }
            }
            xAxis.textColor = requireContext().getOnSurfaceColor()

            axisLeft.setDrawGridLines(false)
            axisLeft.isEnabled = false
            axisRight.setDrawGridLines(false)
            axisRight.isEnabled = false
        }

        binding.chartTotalElements.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            legend.isEnabled = false
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val date = Instant.ofEpochMilli(value.toLong())
                    return OffsetDateTime.ofInstant(date, ZoneOffset.UTC).toLocalDate().toString()
                }
            }
            xAxis.textColor = requireContext().getOnSurfaceColor()

            axisLeft.setDrawGridLines(false)
            axisLeft.isEnabled = false
            axisRight.setDrawGridLines(false)
            axisRight.isEnabled = false
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val reports = loadDailyReports()
                loadUpToDateElements(reports)
                loadTotalElements(reports)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadUpToDateElements(reports: List<JsonElement>) {
        val values = mutableListOf<Entry>()

        for (report in reports) {
            val date = report.jsonObject["date"]!!.jsonPrimitive.content

            values.add(
                Entry(
                    OffsetDateTime.parse(date + "T00:00:00Z").toInstant().toEpochMilli().toFloat(),
                    report.jsonObject["up_to_date_elements"]!!.jsonPrimitive.float,
                    null,
                )
            )
        }

        val dataSet: ILineDataSet

        dataSet = LineDataSet(values, null)
        dataSet.setDrawIcons(false)

        dataSet.color = Color.parseColor("#f7931a")
        dataSet.setCircleColor(Color.parseColor("#f7931a"))
        dataSet.valueTextColor = requireContext().getOnSurfaceColor()

        dataSet.lineWidth = 1f
        dataSet.circleRadius = 3f

        dataSet.setDrawCircleHole(false)

        dataSet.valueTextSize = 9f

        dataSet.setDrawFilled(true)
        dataSet.fillFormatter =
            IFillFormatter { _, _ ->
                binding.chartUpToDateElements.axisLeft.axisMinimum
            }

        dataSet.fillColor = Color.parseColor("#f7931a")

        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(dataSet)

        val data = LineData(dataSets)

        binding.chartUpToDateElements.data = data
        binding.chartUpToDateElements.invalidate()
        binding.chartUpToDateElements.isVisible = true
    }

    private fun loadTotalElements(reports: List<JsonElement>) {
        val values = mutableListOf<Entry>()

        for (report in reports) {
            val date = report.jsonObject["date"]!!.jsonPrimitive.content

            values.add(
                Entry(
                    OffsetDateTime.parse(date + "T00:00:00Z").toInstant().toEpochMilli().toFloat(),
                    report.jsonObject["total_elements"]!!.jsonPrimitive.float,
                    null,
                )
            )
        }

        val dataSet: ILineDataSet

        dataSet = LineDataSet(values, null)
        dataSet.setDrawIcons(false)

        dataSet.color = Color.parseColor("#f7931a")
        dataSet.setCircleColor(Color.parseColor("#f7931a"))
        dataSet.valueTextColor = requireContext().getOnSurfaceColor()

        dataSet.lineWidth = 1f
        dataSet.circleRadius = 3f

        dataSet.setDrawCircleHole(false)

        dataSet.valueTextSize = 9f

        dataSet.setDrawFilled(true)
        dataSet.fillFormatter =
            IFillFormatter { _, _ ->
                binding.chartTotalElements.axisLeft.axisMinimum
            }

        dataSet.fillColor = Color.parseColor("#f7931a")

        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(dataSet)

        val data = LineData(dataSets)

        binding.chartTotalElements.data = data
        binding.chartTotalElements.invalidate()
        binding.chartTotalElements.isVisible = true
    }

    private suspend fun loadDailyReports(): List<JsonElement> {
        val url = "https://api.btcmap.org/daily_reports"
        val request = OkHttpClient().newCall(Request.Builder().url(url).build())
        val response = runCatching { request.await() }.getOrNull()
            ?: return JsonArray(emptyList())
        val reports: JsonArray = Json.decodeFromString(response.body!!.string())
        return reports.subList(1, reports.size - 1)
            .sortedBy { it.jsonObject["date"]!!.jsonPrimitive.content }
    }
}