package com.example.adaptanklebrace.adapters

import com.example.adaptanklebrace.data.Metric

/**
 * Adapter for specific functionality of metrics.
 */
interface RecoveryMetricAdapter: RecoveryAdapter {
    fun setMetrics(newMetrics: List<Metric>)
    fun getMetrics(): List<Metric>
    fun addMetricRow(metric: Metric)
}
