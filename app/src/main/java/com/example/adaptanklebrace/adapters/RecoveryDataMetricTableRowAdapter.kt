package com.example.adaptanklebrace.adapters

import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat.getString
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.SettingsActivity
import com.example.adaptanklebrace.data.Metric
import com.example.adaptanklebrace.enums.ExerciseType
import com.example.adaptanklebrace.utils.GeneralUtil
import java.time.LocalTime

class RecoveryDataMetricTableRowAdapter(
    private val metrics: MutableList<Metric>,
    private val recoveryDataCallback: RecoveryDataCallback
) : RecyclerView.Adapter<RecoveryDataMetricTableRowAdapter.MetricViewHolder>(), RecoveryMetricAdapter {

    // Define the callback interface
    interface RecoveryDataCallback {
        fun saveCurrentDateMetricData()
        fun onClickViewROMMetricDetails(metric: Metric, view: View)
        fun onClickViewGaitMetricDetails(metric: Metric, view: View)
    }

    init {
        // Enable stable IDs useful for the RecyclerView to uniquely identify each row
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        // Header row has a fixed ID of 0, while metric rows use their unique IDs
        return if (position == 0) {
            Long.MIN_VALUE
        } else {
            metrics[position - 1].id.toLong() // Use the `id` property of a metric as the stable ID
        }
    }

    // ViewHolder for both header and item rows
    inner class MetricViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // These will be either EditText or TextView based on row type
        val metricName: View = view.findViewById(R.id.metricName)
        val time: View = view.findViewById(R.id.time)
        private val viewMetricsButton: View = view.findViewById(R.id.viewMetricsBtn)
        val difficulty: View = view.findViewById(R.id.difficulty)
        val comments: View = view.findViewById(R.id.comments)
        private val selectRowCheckBox: View = view.findViewById(R.id.selectRowCheckBox)

        // Bind method will update based on the view type
        fun bind(metric: Metric?, viewType: Int) {
            val context = itemView.context
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (metricName as? TextView)?.text = getString(context, R.string.metricName)
                (time as? TextView)?.text = getString(context, R.string.timeOfCompletion)
                (viewMetricsButton as? TextView)?.text = getString(context, R.string.viewMetrics)
                (difficulty as? TextView)?.text = getString(context, R.string.difficulty)
                (comments as? TextView)?.text = getString(context, R.string.comments)
            } else {
                // Bind editable fields for metric data rows
                (metricName as? TextView)?.text = metric?.name
                (time as? EditText)?.setText(
                    metric?.timeCompleted?.format(GeneralUtil.timeFormatter) ?: LocalTime.now().format(GeneralUtil.timeFormatter))
                (viewMetricsButton as? Button)?.text = getString(context, R.string.viewBtn)
                (difficulty as? EditText)?.setText(metric?.difficulty.toString())
                (comments as? EditText)?.setText(metric?.comments)
                (selectRowCheckBox as? CheckBox)?.isChecked = metric?.isSelected ?: false

                // Update color of viewMetricsButton
                if (metric != null) {
                    if (metric.isManuallyRecorded) {
                        (viewMetricsButton as? Button)?.apply {
                            setBackgroundColor(context.getColor(R.color.grey_1))
                        }
                    } else {
                        (viewMetricsButton as? Button)?.apply {
                            if (SettingsActivity.nightMode) {
                                setBackgroundColor(context.getColor(R.color.nightPrimary))
                            } else {
                                setBackgroundColor(context.getColor(R.color.lightPrimary))
                            }
                        }
                    }
                }

                // Set up listeners for editable fields
                (time as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    onFocusChangeListener = null

                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            metric?.let {
                                GeneralUtil.showTimePickerDialog(context, this) { selectedTime ->
                                    metric.timeCompleted = selectedTime
                                    markAsChanged()
                                }
                                time.clearFocus()
                            }
                        }
                    }
                }
                (viewMetricsButton as? Button)?.setOnClickListener {
                    metric?.let {
                        if (!it.isManuallyRecorded) {
                            if (it.name == ExerciseType.RANGE_OF_MOTION.exerciseName) {
                                recoveryDataCallback.onClickViewROMMetricDetails(
                                    it,
                                    viewMetricsButton
                                )
                            } else if (it.name == ExerciseType.GAIT_TEST.exerciseName) {
                                recoveryDataCallback.onClickViewGaitMetricDetails(
                                    it,
                                    viewMetricsButton
                                )
                            }
                        } else {
                            GeneralUtil.showToast(context, LayoutInflater.from(context), context.getString(R.string.noMetricsAvailableToast))
                        }
                    }
                }
                (difficulty as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    onFocusChangeListener = null
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    var initialDifficulty: Int? = null  // Variable to store the original difficulty value

                    // Add TextChangedListener to handle real-time changes to the text
                    val newWatcher = addTextChangedListener {
                        val currentDifficulty = text.toString().toIntOrNull()

                        // Restrict difficulty level between 0-10
                        if (currentDifficulty == null || currentDifficulty !in 0..10) {
                            GeneralUtil.showToast(context, LayoutInflater.from(context), context.getString(R.string.enterDifficultyLevelToast))
                        } else {
                            metric?.difficulty = currentDifficulty
                        }
                        markAsChanged()
                    }
                    tag = newWatcher

                    // Add OnFocusChangeListener to handle focus loss and reset value if invalid
                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            // Store the original difficulty value when the field gains focus
                            initialDifficulty = metric?.difficulty
                        } else {
                            val currentDifficulty = text.toString().toIntOrNull()

                            // If the input is invalid, reset to the original difficulty value
                            if (currentDifficulty !in 0..10) {
                                // Use the initial value when focus was first gained
                                initialDifficulty?.let {
                                    metric?.difficulty = it
                                    setText(it.toString())  // Reset the text to the original difficulty
                                }
                            }
                            markAsChanged()
                        }
                    }
                }
                (comments as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    val currentWatcher = tag as? TextWatcher
                    if (currentWatcher != null) {
                        removeTextChangedListener(currentWatcher)
                    }

                    val newWatcher = addTextChangedListener {
                        metric?.comments = it.toString()
                        markAsChanged()
                    }
                    tag = newWatcher
                }
                (selectRowCheckBox as? CheckBox)?.setOnCheckedChangeListener { _, isChecked ->
                    metric?.isSelected = isChecked
                    markAsChanged()
                }
            }
        }

        private fun markAsChanged() {
            // This can be used to flag that a change occurred and data needs saving.
            recoveryDataCallback.saveCurrentDateMetricData()
        }
    }

    // Constants for view types
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetricViewHolder {
        // Inflate either header or item row based on the view type
        val view: View = if (viewType == VIEW_TYPE_HEADER) {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_data_metric_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_data_metric_row_item, parent, false)
        }
        return MetricViewHolder(view)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) { // First position is the header
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_ITEM
        }
    }

    override fun onBindViewHolder(holder: MetricViewHolder, position: Int) {
        if (position == 0) {
            holder.bind(null, VIEW_TYPE_HEADER) // No metric data for header
        } else {
            holder.bind(metrics[position - 1], VIEW_TYPE_ITEM) // Bind data for metric rows
        }
    }

    // Ensure listeners are properly cleared when view is detached from the window
    override fun onViewRecycled(holder: MetricViewHolder) {
        super.onViewRecycled(holder)
        (holder.difficulty as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
        (holder.comments as? EditText)?.apply {
            val currentWatcher = tag as? TextWatcher
            if (currentWatcher != null) {
                removeTextChangedListener(currentWatcher)
                tag = null
            }
        }
    }

    override fun getItemCount(): Int = metrics.size + 1 // +1 for the header row

    // Add metric row to the list
    override fun addMetricRow(metric: Metric) {
        metrics.add(metric)
        notifyItemInserted(metrics.size) // Notify adapter
    }

    // Delete metric row from the list
    fun deleteMetricRow() {
        for (i in metrics.size - 1 downTo 0) {
            if (metrics[i].isSelected) {
                metrics.removeAt(i) // Remove the metric
                notifyItemRemoved(i+1) // Notify adapter
            }
        }
    }

    // Get the current list of metrics
    override fun getMetrics(): List<Metric> {
        return metrics.toList() // Return a copy of the list to avoid external modifications
    }

    // Set a new list of metrics
    override fun setMetrics(newMetrics: List<Metric>) {
        // Clear the existing data
        val previousSize = getItemCount()
        metrics.clear()
        notifyItemRangeRemoved(1, previousSize)

        // Add new metrics
        metrics.addAll(newMetrics)
        notifyItemRangeInserted(1, getItemCount())
    }

    override fun notifyItemChangedAndRefresh(position: Int) {
        super.notifyItemChanged(position)
        recoveryDataCallback.saveCurrentDateMetricData()
    }
}
