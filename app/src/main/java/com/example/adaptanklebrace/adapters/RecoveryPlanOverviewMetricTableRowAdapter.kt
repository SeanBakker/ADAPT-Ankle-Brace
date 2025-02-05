package com.example.adaptanklebrace.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.adaptanklebrace.R
import com.example.adaptanklebrace.SettingsActivity
import androidx.core.content.ContextCompat.getString
import com.example.adaptanklebrace.data.Metric

class RecoveryPlanOverviewMetricTableRowAdapter(
    private val metrics: MutableList<Metric>,
    private val mainActivityCallback: MainActivityCallback
) : RecyclerView.Adapter<RecoveryPlanOverviewMetricTableRowAdapter.MetricViewHolder>(),
    RecoveryOverviewAdapter, RecoveryMetricAdapter {

    // Define the callback interface
    interface MainActivityCallback {
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
        private val startMetricButton: View = view.findViewById(R.id.startMetricBtn)

        // Bind method will update based on the view type
        @SuppressLint("DefaultLocale")
        fun bind(metric: Metric?, viewType: Int) {
            val context = itemView.context
            if (viewType == VIEW_TYPE_HEADER) {
                // Cast to TextView for header
                (metricName as? TextView)?.text = getString(context, R.string.metricName)
                (frequency as? TextView)?.text = getString(context, R.string.freq)
                (percentageCompleted as? TextView)?.text = getString(context, R.string.percentCompleted)
                (startMetricButton as? TextView)?.text = getString(context, R.string.startMetricBtn)
            } else {
                // Bind editable fields for metric data rows
                (metricName as? TextView)?.text = metric?.name
                (frequency as? TextView)?.text = metric?.frequency
                (percentageCompleted as? TextView)?.text = String.format("%.2f%%", metric?.percentageCompleted)
                (startMetricButton as? Button)?.text = getString(context, R.string.startBtn)

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

                // Set up listeners
                (startMetricButton as? Button)?.setOnClickListener {
                    metric?.let {
                        mainActivityCallback.onClickStartMetricWithoutWarning(it)
                    }
                }
            }
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
                .inflate(R.layout.recovery_plan_overview_metric_table_header, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.recovery_plan_overview_metric_row_item, parent, false)
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
            val metric = metrics[position - 1]

            // Hide the metric row if the percentage completed is >=100
            metric.isVisible = metric.percentageCompleted < 100

            if (metric.isVisible) {
                // Show the row
                holder.itemView.visibility = View.VISIBLE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT,
                    RecyclerView.LayoutParams.WRAP_CONTENT
                )
                holder.bind(metric, VIEW_TYPE_ITEM) // Bind data
            } else {
                // Hide the row
                holder.itemView.visibility = View.GONE
                holder.itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            }
        }
    }

    override fun getItemCount(): Int = metrics.size + 1 // +1 for the header row

    /**
     * Retrieves the item count of only visible rows in the table.
     *
     * @return integer visible item count
     */
    override fun getVisibleItemCount(): Int {
        metrics.forEach { it.isVisible = it.percentageCompleted < 100 }
        return metrics.count { it.isVisible } + 1 // Count visible metrics + header row
    }

    /**
     * Returns if the row is visible at the specified position
     *
     * @param position Int position of the row
     * @return Boolean whether the row is visible
     */
    override fun isRowVisibleByPosition(position: Int): Boolean {
        return metrics[position - 1].isVisible
    }

    // Add metric row to the list
    override fun addMetricRow(metric: Metric) {
        metrics.add(metric)
        notifyItemInserted(metrics.size) // Notify adapter
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
    }
}
