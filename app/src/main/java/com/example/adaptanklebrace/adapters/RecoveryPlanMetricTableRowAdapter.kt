package com.example.adaptanklebrace.adapters

import android.annotation.SuppressLint
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

class RecoveryPlanMetricTableRowAdapter(
    private val metrics: MutableList<Metric>,
    private val recoveryPlanCallback: RecoveryPlanCallback
) : RecyclerView.Adapter<RecoveryPlanMetricTableRowAdapter.MetricViewHolder>(), RecoveryMetricAdapter {

    // Define the callback interface
    interface RecoveryPlanCallback {
        fun saveCurrentDateMetricData()
        fun onClickViewAllMetricDetails(metric: Metric)
        fun onFocusFrequencyText(metric: Metric, position: Int)
        fun onClickStartMetricWithWarning(metric: Metric)
        fun onClickStartMetricWithoutWarning(metric: Metric)
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
            metrics[position - 1].id.toLong() // Use the `id` property of an metric as the stable ID
        }
    }

    // ViewHolder for both header and item rows
    inner class MetricViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // These will be either EditText or TextView based on row type
        val metricName: View = view.findViewById(R.id.metricName)
        val frequency: View = view.findViewById(R.id.frequency)
        private val percentageCompleted: View = view.findViewById(R.id.percentageCompleted)
        private val viewMetricsButton: View = view.findViewById(R.id.viewMetricsBtn)
        private val startMetricButton: View = view.findViewById(R.id.startMetricBtn)
        val comments: View = view.findViewById(R.id.comments)
        private val selectRowCheckBox: View = view.findViewById(R.id.selectRowCheckBox)

        // Bind method will update based on the view type
        @SuppressLint("DefaultLocale")
        fun bind(metric: Metric?, position: Int, viewType: Int) {
            val context = itemView.context
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (metricName as? TextView)?.text = getString(context, R.string.metricName)
                (frequency as? TextView)?.text = getString(context, R.string.freq)
                (percentageCompleted as? TextView)?.text =
                    getString(context, R.string.percentCompleted)
                (viewMetricsButton as? TextView)?.text = getString(context, R.string.viewMetrics)
                (startMetricButton as? TextView)?.text =
                    getString(context, R.string.startMetricBtn)
                (comments as? TextView)?.text = getString(context, R.string.comments)
            } else {
                // Bind editable fields for metric data rows
                (metricName as? TextView)?.text = metric?.name
                (frequency as? EditText)?.setText(metric?.frequency)
                (percentageCompleted as? TextView)?.text =
                    String.format("%.2f%%", metric?.percentageCompleted)
                (viewMetricsButton as? Button)?.text = getString(context, R.string.viewBtn)
                (startMetricButton as? Button)?.text = getString(context, R.string.startBtn)
                (comments as? EditText)?.setText(metric?.comments)
                (selectRowCheckBox as? CheckBox)?.isChecked = metric?.isSelected ?: false

                // Update color of startMetricButton
                if (metric != null) {
                    if (metric.percentageCompleted >= 100) {
                        (startMetricButton as? Button)?.apply {
                            setBackgroundColor(context.getColor(R.color.grey_1))
                        }
                    } else {
                        (startMetricButton as? Button)?.apply {
                            if (SettingsActivity.nightMode) {
                                setBackgroundColor(context.getColor(R.color.nightPrimary))
                            } else {
                                setBackgroundColor(context.getColor(R.color.lightPrimary))
                            }
                        }
                    }
                }

                (frequency as? EditText)?.apply {
                    // Remove previous listener to avoid duplicate events
                    onFocusChangeListener = null

                    setOnFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            metric?.let {
                                recoveryPlanCallback.onFocusFrequencyText(it, position)
                                frequency.clearFocus()
                            }
                        }
                    }
                }
                (viewMetricsButton as? Button)?.setOnClickListener {
                    metric?.let {
                        recoveryPlanCallback.onClickViewAllMetricDetails(it)
                    }
                }
                (startMetricButton as? Button)?.setOnClickListener {
                    metric?.let {
                        if (it.percentageCompleted >= 100) {
                            recoveryPlanCallback.onClickStartMetricWithWarning(it)
                        } else {
                            recoveryPlanCallback.onClickStartMetricWithoutWarning(it)
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
            recoveryPlanCallback.saveCurrentDateMetricData()
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
                .inflate(R.layout.recovery_plan_metric_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_plan_metric_row_item, parent, false)
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
            holder.bind(null, 0, VIEW_TYPE_HEADER) // No metric data for header
        } else if (position != RecyclerView.NO_POSITION) {
            holder.bind(metrics[position - 1], position, VIEW_TYPE_ITEM) // Bind data for metric rows
        }
    }

    // Ensure listeners are properly cleared when view is detached from the window
    override fun onViewRecycled(holder: MetricViewHolder) {
        super.onViewRecycled(holder)
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
        recoveryPlanCallback.saveCurrentDateMetricData()
    }
}
