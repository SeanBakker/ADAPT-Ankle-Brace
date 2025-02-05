package com.example.adaptanklebrace.adapters

/**
 * Adapter for specific functionality of overview tables.
 */
interface RecoveryOverviewAdapter: RecoveryAdapter {
    fun getVisibleItemCount(): Int
    fun isRowVisibleByPosition(position: Int): Boolean
}
