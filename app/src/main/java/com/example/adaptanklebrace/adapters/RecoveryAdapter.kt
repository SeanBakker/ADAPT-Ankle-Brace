package com.example.adaptanklebrace.adapters

/**
 * Generic adapter defining common functionality.
 */
interface RecoveryAdapter {
    fun getItemCount(): Int
    fun notifyItemChangedAndRefresh(position: Int)
}
