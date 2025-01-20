package reports

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
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
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IFillFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import map.getOnSurfaceColor
import org.btcmap.databinding.FragmentReportsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ReportsFragment : Fragment() {

    private val args = lazy {
        ReportsModel.Args(arguments?.getLong("area_id", 662L) ?: 662L)
    }

    private val model: ReportsModel by viewModel()

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
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

        initChart(binding.chartVerifiedElements)
        initChart(binding.chartTotalElements)
        initVerifiedElementsFractionChart()
        initDaysSinceVerifiedChart()

        model.args.update { args.value }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                model.data.collect { data ->
                    if (data == null) {
                        return@collect
                    }

                    show(
                        chart = binding.chartVerifiedElements,
                        data = data.verifiedPlaces.map { Pair(it.first, it.second.toFloat()) },
                    )

                    show(
                        chart = binding.chartTotalElements,
                        data = data.totalPlaces.map { Pair(it.first, it.second.toFloat()) },
                    )

                    show(
                        chart = binding.chartVerifiedElementsFraction,
                        data = data.verifiedPlacesFraction,
                    )

                    showDaysSinceVerified(
                        chart = binding.chartDaysSinceVerified,
                        data = data.daysSinceVerified,
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initChart(chart: LineChart) {
        chart.apply {
            val topOffset =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    2f,
                    resources.displayMetrics,
                )

            val bottomOffset =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics,
                )

            setExtraOffsets(0f, topOffset, 0f, bottomOffset)
            minOffset = 0f

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
            xAxis.labelCount = 4

            axisLeft.setDrawGridLines(false)
            axisLeft.isEnabled = false
            val gridColor = Color.valueOf(requireContext().getOnSurfaceColor())
            axisRight.gridColor =
                Color.argb(0.12f, gridColor.red(), gridColor.green(), gridColor.blue())
            axisRight.textColor = requireContext().getOnSurfaceColor()
            axisRight.setDrawAxisLine(false)
            axisRight.granularity = 1f
        }
    }

    private fun show(chart: LineChart, data: List<Pair<String, Float>>) {
        val values = mutableListOf<Entry>()

        for (item in data) {
            values.add(
                Entry(
                    OffsetDateTime.parse(item.first + "T00:00:00Z").toInstant().toEpochMilli()
                        .toFloat(),
                    item.second,
                    null,
                )
            )
        }

        val dataSet: ILineDataSet

        dataSet = LineDataSet(values, null)
        dataSet.setDrawIcons(false)
        dataSet.valueFormatter = DefaultValueFormatter(0)

        dataSet.color = Color.parseColor("#f7931a")
        dataSet.valueTextColor = requireContext().getOnSurfaceColor()

        dataSet.setDrawValues(false)
        dataSet.lineWidth = 1f

        dataSet.valueTextSize = 9f

        dataSet.setDrawCircles(false)
        dataSet.setDrawFilled(true)
        dataSet.fillFormatter =
            IFillFormatter { _, _ ->
                chart.axisLeft.axisMinimum
            }

        dataSet.fillColor = Color.parseColor("#f7931a")

        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(dataSet)

        chart.data = LineData(dataSets)
        chart.invalidate()
        chart.isVisible = true
    }

    private fun initVerifiedElementsFractionChart() {
        binding.chartVerifiedElementsFraction.apply {
            val topOffset =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    2f,
                    resources.displayMetrics,
                )

            val bottomOffset =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics,
                )

            setExtraOffsets(0f, topOffset, 0f, bottomOffset)
            minOffset = 0f

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
            xAxis.labelCount = 4

            axisLeft.setDrawGridLines(false)
            axisLeft.isEnabled = false
            val gridColor = Color.valueOf(requireContext().getOnSurfaceColor())
            axisRight.gridColor =
                Color.argb(0.12f, gridColor.red(), gridColor.green(), gridColor.blue())
            axisRight.textColor = requireContext().getOnSurfaceColor()
            axisRight.setDrawAxisLine(false)
            axisRight.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toLong()}%"
                }
            }
            axisRight.granularity = 1f
        }
    }

    private fun initDaysSinceVerifiedChart() {
        binding.chartDaysSinceVerified.apply {
            val topOffset =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    2f,
                    resources.displayMetrics,
                )

            val bottomOffset =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    1f,
                    resources.displayMetrics,
                )

            setExtraOffsets(0f, topOffset, 0f, bottomOffset)
            minOffset = 0f

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
            xAxis.labelCount = 4

            axisLeft.setDrawGridLines(false)
            axisLeft.isEnabled = false
            val gridColor = Color.valueOf(requireContext().getOnSurfaceColor())
            axisRight.gridColor =
                Color.argb(0.12f, gridColor.red(), gridColor.green(), gridColor.blue())
            axisRight.textColor = requireContext().getOnSurfaceColor()
            axisRight.setDrawAxisLine(false)
            axisRight.granularity = 1f
        }
    }

    private fun showDaysSinceVerified(chart: LineChart, data: List<Pair<LocalDate, Long>>) {
        val values = mutableListOf<Entry>()

        for (item in data) {
            values.add(
                Entry(
                    item.first.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli().toFloat(),
                    item.second.toFloat(),
                    null,
                )
            )
        }

        val dataSet: ILineDataSet

        dataSet = LineDataSet(values, null)
        dataSet.color = Color.parseColor("#f7931a")
        dataSet.valueTextColor = requireContext().getOnSurfaceColor()
        dataSet.setDrawValues(false)
        dataSet.valueTextSize = 9f
        dataSet.setDrawCircles(false)
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#f7931a")

        val dataSets: ArrayList<ILineDataSet> = ArrayList()
        dataSets.add(dataSet)

        chart.data = LineData(dataSets)
        chart.invalidate()
        chart.isVisible = true
    }
}